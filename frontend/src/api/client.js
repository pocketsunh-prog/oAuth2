/**
 * API client for communicating with the OAuth2 backend.
 * All requests go through the Vite proxy to avoid CORS issues.
 */

const API_BASE = '/api';

/**
 * Make an API request with optional authentication.
 *
 * @param {string} path - API path (e.g., '/auth/login')
 * @param {object} options - fetch options
 * @param {string} [token] - optional bearer token
 * @returns {Promise<object>} parsed JSON response
 */
async function apiRequest(path, options = {}, token = null) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  // Add authorization header if token is provided
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  // Handle non-JSON error responses
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

/**
 * Authentication API methods.
 */
export const authApi = {
  /**
   * Log in with username and password.
   * @returns {Promise<object>} auth response with tokens
   */
  login: (username, password) =>
    apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  /**
   * Register a new user.
   * @returns {Promise<object>} user profile
   */
  register: (username, email, password) =>
    apiRequest('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, email, password }),
    }),

  /**
   * Get the current user's profile.
   * @param {string} token - bearer token
   * @returns {Promise<object>} user profile
   */
  getProfile: (token) =>
    apiRequest('/auth/me', {}, token),
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
