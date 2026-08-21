import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { tokenApi } from '../api/client';

/**
 * Token Manager page.
 * Displays the user's OAuth2 tokens and allows revocation.
 */
export default function TokenManagerPage() {
  const { accessToken } = useAuth();
  const [tokens, setTokens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  /**
   * Load tokens from the API.
   */
  const loadTokens = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const data = await tokenApi.listTokens(accessToken);
      setTokens(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  // Load tokens on mount
  useEffect(() => {
    loadTokens();
  }, [loadTokens]);

  /**
   * Revoke a specific token.
   */
  const handleRevoke = async (tokenId) => {
    if (!confirm('Are you sure you want to revoke this token?')) {
      return;
    }

    setActionLoading(true);
    try {
      await tokenApi.revokeToken(accessToken, tokenId);
      // Reload the token list
      await loadTokens();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Revoke all tokens.
   */
  const handleRevokeAll = async () => {
    if (!confirm('Are you sure you want to revoke ALL tokens? This will sign you out.')) {
      return;
    }

    setActionLoading(true);
    try {
      await tokenApi.revokeAllTokens(accessToken);
      // Reload the token list
      await loadTokens();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="max-w-4xl">
      {/* Page header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">
            Token Manager
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            View and manage your OAuth2 access tokens
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={loadTokens}
            disabled={loading}
            className="btn-secondary"
          >
            {loading ? 'Loading...' : 'Refresh'}
          </button>
          {tokens.length > 0 && (
            <button
              onClick={handleRevokeAll}
              disabled={actionLoading}
              className="btn-danger"
            >
              Revoke All
            </button>
          )}
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-sm text-red-600">{error}</p>
        </div>
      )}

      {/* Token list */}
      <div className="card">
        {loading ? (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto" />
            <p className="text-sm text-slate-500 mt-3">Loading tokens...</p>
          </div>
        ) : tokens.length === 0 ? (
          <div className="text-center py-8">
            <p className="text-slate-500">No tokens found.</p>
            <p className="text-sm text-slate-400 mt-1">
              Tokens will appear here after you log in.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="text-left py-3 px-2 font-medium text-slate-600">
                    Client
                  </th>
                  <th className="text-left py-3 px-2 font-medium text-slate-600">
                    Token
                  </th>
                  <th className="text-left py-3 px-2 font-medium text-slate-600">
                    Scopes
                  </th>
                  <th className="text-left py-3 px-2 font-medium text-slate-600">
                    Issued
                  </th>
                  <th className="text-left py-3 px-2 font-medium text-slate-600">
                    Expires
                  </th>
                  <th className="text-left py-3 px-2 font-medium text-slate-600">
                    Status
                  </th>
                  <th className="text-right py-3 px-2 font-medium text-slate-600">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody>
                {tokens.map((token) => (
                  <tr key={token.id} className="border-b border-slate-100 last:border-0">
                    <td className="py-3 px-2">
                      <span className="font-medium text-slate-700">
                        {token.clientId}
                      </span>
                    </td>
                    <td className="py-3 px-2">
                      <code className="text-xs bg-slate-100 px-1.5 py-0.5 rounded">
                        {token.accessTokenPreview}
                      </code>
                    </td>
                    <td className="py-3 px-2">
                      <span className="text-slate-600">{token.scopes || '-'}</span>
                    </td>
                    <td className="py-3 px-2 text-slate-500">
                      {formatDate(token.issuedAt)}
                    </td>
                    <td className="py-3 px-2 text-slate-500">
                      {formatDate(token.expiresAt)}
                    </td>
                    <td className="py-3 px-2">
                      <TokenStatusBadge token={token} />
                    </td>
                    <td className="py-3 px-2 text-right">
                      {!token.revoked && !token.expired && (
                        <button
                          onClick={() => handleRevoke(token.id)}
                          disabled={actionLoading}
                          className="text-red-600 hover:text-red-700 text-sm font-medium"
                        >
                          Revoke
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Badge component showing token status.
 */
function TokenStatusBadge({ token }) {
  if (token.revoked) {
    return <span className="badge bg-red-50 text-red-600">Revoked</span>;
  }
  if (token.expired) {
    return <span className="badge bg-slate-100 text-slate-500">Expired</span>;
  }
  return <span className="badge bg-green-50 text-green-600">Active</span>;
}

/**
 * Format a date string for display.
 */
function formatDate(dateStr) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
