import { Link } from "react-router-dom";
import "./LandingPage.css";

export default function LandingPage() {
  return (
    <main className="simple-landing-page">
      <section className="simple-landing-card">
        <img
          src="/DAYA_Logo.svg"
          alt="DAYA Log"
          className="simple-landing-logo"
        />

        <div className="simple-landing-actions">
          <Link to="/autentificare" className="simple-landing-button primary">
            Autentificare
          </Link>

          <Link to="/informatii" className="simple-landing-button secondary">
            Informații despre aplicație
          </Link>
        </div>
      </section>
    </main>
  );
}