import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi, otpApi } from '../api/client';

/**
 * Login page component.
 * Provides forms for login, OTP verification, and registration.
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const { setSession } = useAuth();

  // View mode: 'login' | 'register' | 'otp'
  const [view, setView] = useState('login');

  // Form state
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [otpCode, setOtpCode] = useState('');

  // Temporary token for OTP flow
  const [tempToken, setTempToken] = useState('');

  // UI state
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');

  /**
   * Handle login form submission.
   * May trigger OTP verification if 2FA is enabled.
   */
  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authApi.login(username, password);

      // Check if OTP is required (response has tempToken instead of accessToken)
      if (response.tempToken) {
        setTempToken(response.tempToken);
        setView('otp');
        setError('');
        return;
      }

      // Direct login (no OTP) — update context and redirect
      setSession(response);
      navigate('/tokens');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle OTP verification submission.
   */
  const handleOtpVerify = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await otpApi.verifyLogin(tempToken, otpCode);
      setSession(response);
      navigate('/tokens');
    } catch (err) {
      setError(err.message);
      setOtpCode('');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle registration form submission.
   */
  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authApi.register(username, email, password);
      setSuccess('Registration successful! Please log in.');
      setView('login');
      setPassword('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Cancel OTP and go back to login.
   */
  const handleOtpCancel = () => {
    setView('login');
    setTempToken('');
    setOtpCode('');
    setError('');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-slate-800">
            OAuth2 Server
          </h1>
          <p className="text-slate-500 mt-2">
            {view === 'otp' ? 'Two-Factor Authentication' :
             view === 'register' ? 'Create your account' :
             'Sign in to manage your tokens'}
          </p>
        </div>

        {/* Card */}
        <div className="card">
          {/* Success message */}
          {success && (
            <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
              <p className="text-sm text-green-600">{success}</p>
            </div>
          )}

          {/* Error message */}
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}

          {/* LOGIN FORM */}
          {view === 'login' && (
            <>
              <h2 className="text-lg font-semibold text-slate-800 mb-6">
                Sign In
              </h2>
              <form onSubmit={handleLogin} className="space-y-4">
                <div>
                  <label htmlFor="username" className="block text-sm font-medium text-slate-700 mb-1">
                    Username
                  </label>
                  <input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="input-field"
                    placeholder="Enter your username"
                    required
                    autoComplete="username"
                  />
                </div>
                <div>
                  <label htmlFor="password" className="block text-sm font-medium text-slate-700 mb-1">
                    Password
                  </label>
                  <input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="input-field"
                    placeholder="Enter your password"
                    required
                    autoComplete="current-password"
                  />
                </div>
                <button type="submit" disabled={loading} className="btn-primary w-full">
                  {loading ? 'Signing in...' : 'Sign In'}
                </button>
              </form>
              <div className="mt-4 text-center">
                <button
                  type="button"
                  onClick={() => { setView('register'); setError(''); setSuccess(''); }}
                  className="text-sm text-primary-600 hover:text-primary-700"
                >
                  Don't have an account? Register
                </button>
              </div>
            </>
          )}

          {/* OTP VERIFICATION FORM */}
          {view === 'otp' && (
            <>
              <div className="text-center mb-6">
                <div className="w-16 h-16 bg-primary-50 rounded-full flex items-center justify-center mx-auto mb-3">
                  <span className="text-2xl">🔐</span>
                </div>
                <h2 className="text-lg font-semibold text-slate-800">
                  Enter Authenticator Code
                </h2>
                <p className="text-sm text-slate-500 mt-1">
                  Open your authenticator app and enter the 6-digit code
                </p>
              </div>
              <form onSubmit={handleOtpVerify} className="space-y-4">
                <div>
                  <label htmlFor="otp-code" className="block text-sm font-medium text-slate-700 mb-1">
                    6-Digit Code
                  </label>
                  <input
                    id="otp-code"
                    type="text"
                    inputMode="numeric"
                    value={otpCode}
                    onChange={(e) => {
                      const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                      setOtpCode(val);
                    }}
                    className="input-field text-center text-2xl tracking-widest font-mono"
                    placeholder="000000"
                    required
                    maxLength={6}
                    pattern="\d{6}"
                    autoComplete="one-time-code"
                    autoFocus
                  />
                </div>
                <button
                  type="submit"
                  disabled={loading || otpCode.length !== 6}
                  className="btn-primary w-full"
                >
                  {loading ? 'Verifying...' : 'Verify'}
                </button>
                <button
                  type="button"
                  onClick={handleOtpCancel}
                  className="btn-secondary w-full"
                >
                  Cancel
                </button>
              </form>
            </>
          )}

          {/* REGISTRATION FORM */}
          {view === 'register' && (
            <>
              <h2 className="text-lg font-semibold text-slate-800 mb-6">
                Create Account
              </h2>
              <form onSubmit={handleRegister} className="space-y-4">
                <div>
                  <label htmlFor="reg-username" className="block text-sm font-medium text-slate-700 mb-1">
                    Username
                  </label>
                  <input
                    id="reg-username"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="input-field"
                    placeholder="Choose a username"
                    required
                    minLength={3}
                  />
                </div>
                <div>
                  <label htmlFor="reg-email" className="block text-sm font-medium text-slate-700 mb-1">
                    Email
                  </label>
                  <input
                    id="reg-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="input-field"
                    placeholder="you@example.com"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="reg-password" className="block text-sm font-medium text-slate-700 mb-1">
                    Password
                  </label>
                  <input
                    id="reg-password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="input-field"
                    placeholder="At least 6 characters"
                    required
                    minLength={6}
                  />
                </div>
                <button type="submit" disabled={loading} className="btn-primary w-full">
                  {loading ? 'Creating account...' : 'Create Account'}
                </button>
              </form>
              <div className="mt-4 text-center">
                <button
                  type="button"
                  onClick={() => { setView('login'); setError(''); setSuccess(''); }}
                  className="text-sm text-primary-600 hover:text-primary-700"
                >
                  Already have an account? Sign in
                </button>
              </div>
            </>
          )}
        </div>

        {/* Demo credentials hint */}
        {view === 'login' && (
          <div className="mt-4 text-center">
            <p className="text-xs text-slate-400">
              Demo: username <code className="bg-slate-100 px-1 rounded">admin</code> /
              password <code className="bg-slate-100 px-1 rounded">admin123</code>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
