import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ allowedRoles }) {
  const { user, loading, isAuthenticated } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <main className="state-page">
        <section className="state-card state-card-small">
          <div className="loading-dot-row" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <h1>Se încarcă sesiunea</h1>
          <p className="muted-text">Verificăm autentificarea înainte de a deschide pagina.</p>
        </section>
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/autentificare" replace state={{ from: location }} />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return (
      <Navigate
        to="/neautorizat"
        replace
        state={{
          from: location,
          allowedRoles,
        }}
      />
    );
  }

  return <Outlet />;
}
