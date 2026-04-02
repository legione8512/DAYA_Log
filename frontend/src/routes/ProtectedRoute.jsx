import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

// I use this component to block routes for users who are not logged in
// or who do not have the correct role.
export default function ProtectedRoute({ allowedRoles }) {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) {
    return <div style={{ padding: "40px" }}>Se încarcă...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/autentificare" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={user.role === "ADMIN" ? "/admin" : "/client"} replace />;
  }

  return <Outlet />;
}