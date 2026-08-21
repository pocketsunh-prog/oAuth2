import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminApi } from '../api/client';

/**
 * Admin Dashboard page.
 * Provides user management and service token management.
 */
export default function AdminPage() {
  const { accessToken } = useAuth();

  return (
    <div className="max-w-5xl space-y-8">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-800">
          Admin Dashboard
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Manage users and service tokens
        </p>
      </div>

      {/* User Management Section */}
      <UserManagementSection accessToken={accessToken} />

      {/* Service Token Management Section */}
      <ServiceTokenManagementSection accessToken={accessToken} />
    </div>
  );
}

/**
 * User Management section of the admin dashboard.
 * Allows admins to create users and view all users.
 */
function UserManagementSection({ accessToken }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Form state
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('USER');
  const [creating, setCreating] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');

  /**
   * Load all users from the API.
   */
  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const data = await adminApi.listUsers(accessToken);
      setUsers(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  // Load users on mount
  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  /**
   * Handle user creation form submission.
   */
  const handleCreateUser = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setCreating(true);

    try {
      await adminApi.createUser(accessToken, { username, email, password, role });
      setSuccessMsg(`User "${username}" created successfully!`);
      // Reset form
      setUsername('');
      setEmail('');
      setPassword('');
      setRole('USER');
      // Reload user list
      await loadUsers();
    } catch (err) {
      setError(err.message);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="card">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">
        User Management
      </h2>

      {/* Create User Form */}
      <form onSubmit={handleCreateUser} className="mb-6 p-4 bg-slate-50 rounded-lg">
        <h3 className="text-sm font-medium text-slate-700 mb-3">Create New User</h3>

        {error && (
          <div className="mb-3 p-2 bg-red-50 border border-red-200 rounded">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        {successMsg && (
          <div className="mb-3 p-2 bg-green-50 border border-green-200 rounded">
            <p className="text-sm text-green-600">{successMsg}</p>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="input-field text-sm"
              placeholder="johndoe"
              required
              minLength={3}
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input-field text-sm"
              placeholder="john@example.com"
              required
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input-field text-sm"
              placeholder="Min 6 characters"
              required
              minLength={6}
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Role</label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="input-field text-sm"
            >
              <option value="USER">User</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
        </div>

        <button
          type="submit"
          disabled={creating}
          className="btn-primary mt-3 text-sm"
        >
          {creating ? 'Creating...' : 'Create User'}
        </button>
      </form>

      {/* User List */}
      <div>
        <h3 className="text-sm font-medium text-slate-700 mb-3">All Users ({users.length})</h3>

        {loading ? (
          <div className="text-center py-4">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600 mx-auto" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="text-left py-2 px-2 font-medium text-slate-600">ID</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Username</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Email</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Role</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-slate-100 last:border-0">
                    <td className="py-2 px-2 text-slate-500">{u.id}</td>
                    <td className="py-2 px-2 font-medium text-slate-700">{u.username}</td>
                    <td className="py-2 px-2 text-slate-600">{u.email}</td>
                    <td className="py-2 px-2">
                      <span className={`badge ${u.role === 'ADMIN' ? 'bg-purple-50 text-purple-600' : 'bg-slate-100 text-slate-600'}`}>
                        {u.role}
                      </span>
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
 * Service Token Management section of the admin dashboard.
 * Allows admins to create and revoke service tokens.
 */
function ServiceTokenManagementSection({ accessToken }) {
  const [tokens, setTokens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Available scopes
  const availableScopes = ['read', 'write'];

  // Form state
  const [name, setName] = useState('');
  const [selectedScopes, setSelectedScopes] = useState([]);
  const [expiresInDays, setExpiresInDays] = useState('');
  const [creating, setCreating] = useState(false);
  const [newToken, setNewToken] = useState(null);

  /**
   * Load all service tokens from the API.
   */
  const loadTokens = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const data = await adminApi.listServiceTokens(accessToken);
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
   * Handle service token creation form submission.
   */
  const handleCreateToken = async (e) => {
    e.preventDefault();
    setError('');
    setNewToken(null);
    setCreating(true);

    try {
      const expires = expiresInDays ? parseInt(expiresInDays, 10) : null;
      const result = await adminApi.createServiceToken(accessToken, {
        name,
        scopes: selectedScopes.length > 0 ? selectedScopes.join(',') : null,
        expiresInDays: expires,
      });
      setNewToken(result);
      // Reset form
      setName('');
      setSelectedScopes([]);
      setExpiresInDays('');
      // Reload token list
      await loadTokens();
    } catch (err) {
      setError(err.message);
    } finally {
      setCreating(false);
    }
  };

  /**
   * Handle service token revocation.
   */
  const handleRevoke = async (id) => {
    if (!confirm('Are you sure you want to revoke this service token?')) {
      return;
    }

    try {
      await adminApi.revokeServiceToken(accessToken, id);
      await loadTokens();
    } catch (err) {
      setError(err.message);
    }
  };

  /**
   * Copy the new token value to clipboard.
   */
  const copyToken = () => {
    if (newToken?.tokenPreview) {
      navigator.clipboard.writeText(newToken.tokenPreview);
    }
  };

  return (
    <div className="card">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">
        Service Token Management
      </h2>

      {/* New Token Display (shown after creation) */}
      {newToken && (
        <div className="mb-4 p-4 bg-amber-50 border border-amber-200 rounded-lg">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-amber-800">Token Created - Copy Now!</h3>
            <button
              onClick={() => setNewToken(null)}
              className="text-amber-600 hover:text-amber-800 text-sm"
            >
              Dismiss
            </button>
          </div>
          <p className="text-xs text-amber-700 mb-2">
            This token will only be shown once. Make sure to copy it now.
          </p>
          <div className="flex items-center gap-2">
            <code className="flex-1 text-xs bg-white px-3 py-2 rounded border border-amber-200 break-all font-mono">
              {newToken.tokenPreview}
            </code>
            <button
              onClick={copyToken}
              className="btn-secondary text-sm whitespace-nowrap"
            >
              Copy
            </button>
          </div>
        </div>
      )}

      {/* Create Token Form */}
      <form onSubmit={handleCreateToken} className="mb-6 p-4 bg-slate-50 rounded-lg">
        <h3 className="text-sm font-medium text-slate-700 mb-3">Create New Service Token</h3>

        {error && (
          <div className="mb-3 p-2 bg-red-50 border border-red-200 rounded">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="input-field text-sm"
              placeholder="My Service Token"
              required
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Scopes</label>
            <div className="flex items-center gap-4 py-2">
              {availableScopes.map((scope) => (
                <label key={scope} className="flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selectedScopes.includes(scope)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedScopes([...selectedScopes, scope]);
                      } else {
                        setSelectedScopes(selectedScopes.filter((s) => s !== scope));
                      }
                    }}
                    className="w-4 h-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm text-slate-700">{scope}</span>
                </label>
              ))}
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Expires In (days)</label>
            <input
              type="number"
              value={expiresInDays}
              onChange={(e) => setExpiresInDays(e.target.value)}
              className="input-field text-sm"
              placeholder="Never"
              min="1"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={creating}
          className="btn-primary mt-3 text-sm"
        >
          {creating ? 'Creating...' : 'Create Service Token'}
        </button>
      </form>

      {/* Token List */}
      <div>
        <h3 className="text-sm font-medium text-slate-700 mb-3">All Service Tokens ({tokens.length})</h3>

        {loading ? (
          <div className="text-center py-4">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600 mx-auto" />
          </div>
        ) : tokens.length === 0 ? (
          <p className="text-slate-500 text-sm">No service tokens found.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Name</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Token</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Scopes</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Issued By</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Expires</th>
                  <th className="text-left py-2 px-2 font-medium text-slate-600">Status</th>
                  <th className="text-right py-2 px-2 font-medium text-slate-600">Action</th>
                </tr>
              </thead>
              <tbody>
                {tokens.map((token) => (
                  <tr key={token.id} className="border-b border-slate-100 last:border-0">
                    <td className="py-2 px-2 font-medium text-slate-700">{token.name}</td>
                    <td className="py-2 px-2">
                      <code className="text-xs bg-slate-100 px-1.5 py-0.5 rounded">
                        {token.tokenPreview}
                      </code>
                    </td>
                    <td className="py-2 px-2 text-slate-600">{token.scopes || '-'}</td>
                    <td className="py-2 px-2 text-slate-600">{token.issuedBy || '-'}</td>
                    <td className="py-2 px-2 text-slate-500">
                      {token.expiresAt ? formatDate(token.expiresAt) : 'Never'}
                    </td>
                    <td className="py-2 px-2">
                      <ServiceTokenStatusBadge token={token} />
                    </td>
                    <td className="py-2 px-2 text-right">
                      {!token.revoked && !token.expired && (
                        <button
                          onClick={() => handleRevoke(token.id)}
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
 * Badge component showing service token status.
 */
function ServiceTokenStatusBadge({ token }) {
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
  });
}
