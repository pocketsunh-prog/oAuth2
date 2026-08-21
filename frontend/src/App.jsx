import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import TokenManagerPage from './pages/TokenManagerPage';
import AdminPage from './pages/AdminPage';
import AdminRoute from './components/AdminRoute';
import Layout from './components/Layout';

/**
 * Main application component.
 * Sets up routing with protected routes.
 */
export default function App() {
  const { isAuthenticated, loading } = useAuth();

  // Show a loading spinner while checking auth state
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={
        isAuthenticated ? <Navigate to="/tokens" replace /> : <LoginPage />
      } />

      {/* Protected routes */}
      <Route path="/" element={
        isAuthenticated ? <Layout /> : <Navigate to="/login" replace />
      }>
        <Route index element={<Navigate to="/tokens" replace />} />
        <Route path="tokens" element={<TokenManagerPage />} />
        <Route path="admin" element={<AdminRoute><AdminPage /></AdminRoute>} />
      </Route>

      {/* Catch-all redirect */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
