import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Route guard component that only allows admin users.
 * Non-admin users are redirected to the tokens page.
 */
export default function AdminRoute({ children }) {
  const { isAdmin } = useAuth();

  if (!isAdmin) {
    return <Navigate to="/tokens" replace />;
  }

  return children;
}
