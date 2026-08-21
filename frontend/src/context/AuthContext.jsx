import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { authApi } from '../api/client';

/**
 * Authentication context.
 * Provides the current user, tokens, and auth methods to the component tree.
 */
const AuthContext = createContext(null);

/**
 * Hook to access the authentication context.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

/**
 * AuthProvider component that wraps the app and manages authentication state.
 */
export function AuthProvider({ children }) {
  // Initialize state from localStorage if available
  const [user, setUser] = useState(null);
  const [accessToken, setAccessToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // On mount, check for stored tokens and load the user profile
  useEffect(() => {
    const storedToken = localStorage.getItem('access_token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      setAccessToken(storedToken);
      setUser(JSON.parse(storedUser));
    }

    setLoading(false);
  }, []);

  /**
   * Log in with username and password.
   * Stores the tokens and user in state and localStorage.
   */
  const login = useCallback(async (username, password) => {
    const response = await authApi.login(username, password);

    setUser(response.user);
    setAccessToken(response.accessToken);

    // Persist to localStorage
    localStorage.setItem('access_token', response.accessToken);
    localStorage.setItem('refresh_token', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.user));

    return response;
  }, []);

  /**
   * Register a new user account.
   */
  const register = useCallback(async (username, email, password) => {
    return authApi.register(username, email, password);
  }, []);

  /**
   * Log out the current user.
   * Clears all auth state and localStorage.
   */
  const logout = useCallback(() => {
    setUser(null);
    setAccessToken(null);
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user');
  }, []);

  const isAdmin = user?.role === 'ADMIN';

  const value = {
    user,
    accessToken,
    loading,
    login,
    register,
    logout,
    isAuthenticated: !!accessToken,
    isAdmin,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
