import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { otpApi } from '../api/client';

/**
 * OTP Setup page.
 * Allows users to enable, verify, and disable TOTP two-factor authentication.
 */
export default function OtpSetupPage() {
  const { accessToken } = useAuth();

  // State
  const [totpEnabled, setTotpEnabled] = useState(false);
  const [setupData, setSetupData] = useState(null);
  const [verifyCode, setVerifyCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /**
     * Load the current OTP status.
     */
  const loadStatus = useCallback(async () => {
    try {
      const status = await otpApi.getStatus(accessToken);
      setTotpEnabled(status.totpEnabled);
    } catch (err) {
      setError(err.message);
    }
  }, [accessToken]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  /**
     * Start TOTP setup - generates secret and QR code.
     */
  const handleStartSetup = async () => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const setup = await otpApi.setup(accessToken);
      setSetupData(setup);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
     * Verify the setup code and enable TOTP.
     */
  const handleVerifySetup = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await otpApi.verifySetup(accessToken, verifyCode);
      setSuccess('Two-factor authentication enabled successfully!');
      setSetupData(null);
      setVerifyCode('');
      setTotpEnabled(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
     * Disable TOTP.
     */
  const handleDisable = async () => {
    if (!confirm('Are you sure you want to disable two-factor authentication?')) {
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await otpApi.disable(accessToken);
      setSuccess('Two-factor authentication disabled.');
      setTotpEnabled(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
     * Cancel the setup process.
     */
  const handleCancelSetup = () => {
    setSetupData(null);
    setVerifyCode('');
    setError('');
  };

  return (
    <div className="max-w-2xl">
      {/* Page header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">
          Two-Factor Authentication
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Add an extra layer of security using time-based one-time passwords (TOTP)
        </p>
      </div>

      {/* Error message */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-sm text-red-600">{error}</p>
        </div>
      )}

      {/* Success message */}
      {success && (
        <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
          <p className="text-sm text-green-600">{success}</p>
        </div>
      )}

      {/* Status card */}
      <div className="card mb-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-800">
              TOTP Status
            </h2>
            <p className="text-sm text-slate-500 mt-1">
              {totpEnabled
                ? 'Your account is protected with two-factor authentication.'
                : 'Two-factor authentication is not enabled.'}
            </p>
          </div>
          <span className={`badge ${totpEnabled ? 'bg-green-50 text-green-600' : 'bg-slate-100 text-slate-500'}`}>
            {totpEnabled ? 'Enabled' : 'Disabled'}
          </span>
        </div>
      </div>

      {/* Setup flow */}
      {setupData && !totpEnabled && (
        <div className="card mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">
            Scan QR Code
          </h2>

          {/* QR Code */}
          <div className="flex justify-center mb-4">
            <div className="bg-white p-4 rounded-lg border border-slate-200">
              <img
                src={`data:image/png;base64,${setupData.qrCodeBase64}`}
                alt="TOTP QR Code"
                className="w-48 h-48"
              />
            </div>
          </div>

          {/* Manual entry */}
          <div className="mb-4 p-3 bg-slate-50 rounded-lg">
            <p className="text-xs text-slate-500 mb-1">
              Can't scan? Enter this code manually:
            </p>
            <code className="text-sm font-mono text-slate-700 break-all">
              {setupData.secret}
            </code>
          </div>

          {/* Verify form */}
          <form onSubmit={handleVerifySetup} className="space-y-4">
            <div>
              <label htmlFor="verify-code" className="block text-sm font-medium text-slate-700 mb-1">
                Enter the 6-digit code from your authenticator app
              </label>
              <input
                id="verify-code"
                type="text"
                inputMode="numeric"
                value={verifyCode}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                  setVerifyCode(val);
                }}
                className="input-field text-center text-xl tracking-widest font-mono"
                placeholder="000000"
                required
                maxLength={6}
                pattern="\d{6}"
              />
            </div>
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={loading || verifyCode.length !== 6}
                className="btn-primary"
              >
                {loading ? 'Verifying...' : 'Verify & Enable'}
              </button>
              <button
                type="button"
                onClick={handleCancelSetup}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Action buttons */}
      {!setupData && (
        <div className="card">
          {!totpEnabled ? (
            <div>
              <h2 className="text-lg font-semibold text-slate-800 mb-2">
                Enable Two-Factor Authentication
              </h2>
              <p className="text-sm text-slate-500 mb-4">
                Use an authenticator app like Google Authenticator, Authy,
                or Microsoft Authenticator to generate secure codes.
              </p>
              <button
                onClick={handleStartSetup}
                disabled={loading}
                className="btn-primary"
              >
                {loading ? 'Setting up...' : 'Set up 2FA'}
              </button>
            </div>
          ) : (
            <div>
              <h2 className="text-lg font-semibold text-slate-800 mb-2">
                Disable Two-Factor Authentication
              </h2>
              <p className="text-sm text-slate-500 mb-4">
                This will remove the extra security from your account.
                You can re-enable it at any time.
              </p>
              <button
                onClick={handleDisable}
                disabled={loading}
                className="btn-danger"
              >
                {loading ? 'Disabling...' : 'Disable 2FA'}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Instructions */}
      <div className="card mt-6">
        <h3 className="text-sm font-semibold text-slate-700 mb-3">
          How it works
        </h3>
        <ol className="text-sm text-slate-600 space-y-2 list-decimal list-inside">
          <li>Click "Set up 2FA" to generate a secret key</li>
          <li>Scan the QR code with your authenticator app</li>
          <li>Enter the 6-digit code to verify setup</li>
          <li>Next time you log in, you'll need to enter a code from your app</li>
        </ol>
      </div>
    </div>
  );
}
