import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function UnauthorizedPage() {
  const location = useLocation();
  const { user, isAuthenticated } = useAuth();

  const homePath = !isAuthenticated
    ? "/autentificare"
    : user?.role === "ADMIN"
      ? "/admin"
      : "/client";

  const allowedRoles = location.state?.allowedRoles || [];
  const attemptedPath = location.state?.from?.pathname;

  return (
    <main className="state-page">
      <section className="state-card">
        <p className="page-kicker">Acces restricționat</p>
        <h1>Nu ai acces la această pagină</h1>
        <p className="muted-text">
          Contul curent nu are rolul necesar pentru zona accesată.
        </p>

        <div className="details-list compact-details-list">
          {user?.role && (
            <div>
              <dt>Rol curent</dt>
              <dd>{user.role}</dd>
            </div>
          )}
          {allowedRoles.length > 0 && (
            <div>
              <dt>Rol permis</dt>
              <dd>{allowedRoles.join(", ")}</dd>
            </div>
          )}
          {attemptedPath && (
            <div>
              <dt>Pagină cerută</dt>
              <dd>{attemptedPath}</dd>
            </div>
          )}
        </div>

        <div className="state-actions">
          <Link className="primary-link-button" to={homePath}>
            Înapoi la panoul meu
          </Link>
          <Link className="secondary-link-button" to="/autentificare">
            Schimbă contul
          </Link>
        </div>
      </section>
    </main>
  );
}
