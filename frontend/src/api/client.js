/**
 * API client for communicating with the OAuth2 backend.
 * All requests go through the Vite proxy to avoid CORS issues.
 *
 * Features automatic token refresh: when a request receives a 401, the client
 * uses the stored refresh token to obtain a new access token, then retries the
 * original request. Subscribers (e.g. AuthContext) are notified of the new token.
 */

const API_BASE = '/api';

// --- Token refresh coordination --------------------------------------------

// Tracks an in-flight refresh so concurrent 401s share a single refresh request.
let refreshPromise = null;

// Subscribers notified with the new access token after a successful refresh.
const tokenSubscribers = new Set();

/**
 * Register a callback that will be invoked with the new access token
 * whenever the tokens are refreshed. Returns an unsubscribe function.
 */
export function onTokenRefresh(callback) {
  tokenSubscribers.add(callback);
  return () => tokenSubscribers.delete(callback);
}

function notifyTokenRefresh(accessToken) {
  tokenSubscribers.forEach((cb) => cb(accessToken));
}

/**
 * Refresh the access token using the refresh token stored in localStorage.
 * Updates localStorage with the new token pair. Only one refresh runs at a time;
 * concurrent callers share the same promise.
 *
 * @returns {Promise<string>} the new access token
 * @throws {Error} if no refresh token exists or the refresh fails
 */
async function refreshTokens() {
  // Reuse an already-running refresh if one is in progress.
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    const storedRefreshToken = localStorage.getItem('refresh_token');
    if (!storedRefreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: storedRefreshToken }),
    });

    if (!response.ok) {
      // Refresh failed — clear stored tokens so we don't keep retrying.
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      throw new Error('Session expired. Please log in again.');
    }

    const data = await response.json();

    // Persist the new token pair.
    localStorage.setItem('access_token', data.accessToken);
    localStorage.setItem('refresh_token', data.refreshToken);

    // Notify subscribers (e.g. AuthContext) so React state stays in sync.
    notifyTokenRefresh(data.accessToken);

    return data.accessToken;
  })();

  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

// --- Core request helper ---------------------------------------------------

/**
 * Paths that should never trigger a refresh-and-retry cycle.
 */
const UNAUTHENTICATED_PATHS = new Set(['/auth/login', '/auth/register', '/auth/refresh']);

/**
 * Make an API request with optional authentication.
 * On a 401 response, automatically refreshes the token and retries once.
 *
 * @param {string} path - API path (e.g., '/auth/login')
 * @param {object} options - fetch options
 * @param {string} [token] - optional bearer token
 * @returns {Promise<object>} parsed JSON response
 */
async function apiRequest(path, options = {}, token = null) {
  const buildHeaders = (accessToken) => ({
    'Content-Type': 'application/json',
    ...options.headers,
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  });

  const url = `${API_BASE}${path}`;

  // First attempt
  let response = await fetch(url, { ...options, headers: buildHeaders(token) });

  // If unauthorized and this isn't an auth endpoint, try to refresh and retry.
  if (response.status === 401 && !UNAUTHENTICATED_PATHS.has(path)) {
    try {
      const newToken = await refreshTokens();
      response = await fetch(url, { ...options, headers: buildHeaders(newToken) });
    } catch (refreshError) {
      // Refresh failed — surface a clear error; original 401 is irrelevant.
      throw refreshError;
    }
  }

  // Handle error responses
  if (!response.ok) {
    let errorMessage = `Request failed with status ${response.status}`;
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch {
      // Response was not JSON
    }
    throw new Error(errorMessage);
  }

  // Handle empty responses
  if (response.status === 204) {
    return null;
  }

  return response.json();
}

// --- API method groups -----------------------------------------------------

/**
 * Authentication API methods.
 */
export const authApi = {
  /**
   * Log in with username and password.
   * Returns either an AuthResponse (with tokens) or OtpRequiredResponse
   * (with tempToken) when 2FA is enabled.
   *
   * This method does NOT use the authenticated apiRequest helper
   * (no bearer token needed for login).
   *
   * @returns {Promise<object>} auth response or OTP-required response
   */
  login: (username, password) =>
    fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }).then(async (response) => {
      if (!response.ok) {
        let errorMessage = `Login failed with status ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch {
          // Response was not JSON
        }
        throw new Error(errorMessage);
      }
      return response.json();
    }),

  /**
   * Register a new user.
   * This method does NOT use the authenticated apiRequest helper.
   * @returns {Promise<object>} user profile
   */
  register: (username, email, password) =>
    fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password }),
    }).then(async (response) => {
      if (!response.ok) {
        let errorMessage = `Registration failed with status ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch {
          // Response was not JSON
        }
        throw new Error(errorMessage);
      }
      return response.json();
    }),

  /**
   * Get the current user's profile.
   * @param {string} token - bearer token
   * @returns {Promise<object>} user profile
   */
  getProfile: (token) =>
    apiRequest('/auth/me', {}, token),

  /**
   * Refresh the access token.
   * @param {string} refreshToken - the refresh token
   * @returns {Promise<object>} new auth response with tokens
   */
  refresh: (refreshToken) =>
    apiRequest('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
};

/**
 * Token management API methods.
 */
export const tokenApi = {
  /**
   * List all tokens for the current user.
   * @param {string} token - bearer token
   * @returns {Promise<Array>} list of token info objects
   */
  listTokens: (token) =>
    apiRequest('/tokens', {}, token),

  /**
   * Revoke a specific token.
   * @param {string} token - bearer token
   * @param {number} tokenId - ID of the token to revoke
   */
  revokeToken: (token, tokenId) =>
    apiRequest(`/tokens/${tokenId}`, { method: 'DELETE' }, token),

  /**
   * Revoke all tokens for the current user.
   * @param {string} token - bearer token
   * @returns {Promise<number>} count of revoked tokens
   */
  revokeAllTokens: (token) =>
    apiRequest('/tokens', { method: 'DELETE' }, token),
};

/**
 * OTP / Two-Factor Authentication API methods.
 */
export const otpApi = {
  /**
   * Get the current user's OTP status.
   * @param {string} token - bearer token
   * @returns {Promise<object>} { totpEnabled: boolean }
   */
  getStatus: (token) =>
    apiRequest('/auth/otp/status', {}, token),

  /**
   * Set up TOTP for the current user.
   * @param {string} token - bearer token
   * @returns {Promise<object>} { secret, otpauthUri, qrCodeBase64, alreadyEnabled }
   */
  setup: (token) =>
    apiRequest('/auth/otp/setup', { method: 'POST' }, token),

  /**
   * Verify and enable TOTP setup.
   * @param {string} token - bearer token
   * @param {string} code - 6-digit code from authenticator app
   * @returns {Promise<object>} confirmation message
   */
  verifySetup: (token, code) =>
    apiRequest('/auth/otp/verify-setup', {
      method: 'POST',
      body: JSON.stringify({ code }),
    }, token),

  /**
   * Disable TOTP for the current user.
   * @param {string} token - bearer token
   * @returns {Promise<object>} confirmation message
   */
  disable: (token) =>
    apiRequest('/auth/otp/disable', { method: 'POST' }, token),

  /**
   * Verify OTP code during login (second step).
   * @param {string} tempToken - temporary token from login response
   * @param {string} code - 6-digit code from authenticator app
   * @returns {Promise<object>} auth response with tokens
   */
  verifyLogin: (tempToken, code) =>
    fetch(`${API_BASE}/auth/otp/verify-login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Temp-Token': tempToken,
      },
      body: JSON.stringify({ code }),
    }).then(async (response) => {
      if (!response.ok) {
        let errorMessage = `Request failed with status ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch {
          // Response was not JSON
        }
        throw new Error(errorMessage);
      }
      return response.json();
    }),
};

/**
 * Admin API methods.
 */
export const adminApi = {
  /**
   * Create a new user (admin only).
   * @param {string} token - bearer token
   * @param {object} userData - { username, email, password, role }
   * @returns {Promise<object>} created user info
   */
  createUser: (token, { username, email, password, role }) =>
    apiRequest('/admin/users', {
      method: 'POST',
      body: JSON.stringify({ username, email, password, role }),
    }, token),

  /**
   * List all users (admin only).
   * @param {string} token - bearer token
   * @returns {Promise<Array>} list of user info objects
   */
  listUsers: (token) =>
    apiRequest('/admin/users', {}, token),

  /**
   * Update a user (admin only).
   * @param {string} token - bearer token
   * @param {number} userId - ID of the user to update
   * @param {object} userData - { email?, role?, password?, totpEnabled? }
   * @returns {Promise<object>} updated user info
   */
  updateUser: (token, userId, { email, role, password, totpEnabled }) =>
    apiRequest(`/admin/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify({ email, role, password, totpEnabled }),
    }, token),

  /**
   * Create a new service token (admin only).
   * @param {string} token - bearer token
   * @param {object} tokenData - { name, scopes, expiresInDays }
   * @returns {Promise<object>} service token info (with full token value)
   */
  createServiceToken: (token, { name, scopes, expiresInDays }) =>
    apiRequest('/admin/service-tokens', {
      method: 'POST',
      body: JSON.stringify({ name, scopes, expiresInDays }),
    }, token),

  /**
   * List all service tokens (admin only).
   * @param {string} token - bearer token
   * @returns {Promise<Array>} list of service token info objects
   */
  listServiceTokens: (token) =>
    apiRequest('/admin/service-tokens', {}, token),

  /**
   * Revoke a service token (admin only).
   * @param {string} token - bearer token
   * @param {number} id - ID of the service token to revoke
   */
  revokeServiceToken: (token, id) =>
    apiRequest(`/admin/service-tokens/${id}`, { method: 'DELETE' }, token),
};

export default apiRequest;
