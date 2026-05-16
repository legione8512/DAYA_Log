import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function NotFoundPage() {
  const { user, isAuthenticated } = useAuth();

  const homePath = !isAuthenticated
    ? "/autentificare"
    : user?.role === "ADMIN"
      ? "/admin"
      : "/client";

  return (
    <main className="state-page">
      <section className="state-card">
        <p className="page-kicker">Pagină indisponibilă</p>
        <h1>Pagina nu a fost găsită</h1>
        <p className="muted-text">
          Linkul folosit nu mai există sau adresa a fost scrisă greșit.
        </p>

        <div className="state-actions">
          <Link className="primary-link-button" to={homePath}>
            Înapoi la panoul principal
          </Link>
          <Link className="secondary-link-button" to="/autentificare">
            Mergi la autentificare
          </Link>
        </div>
      </section>
    </main>
  );
}
