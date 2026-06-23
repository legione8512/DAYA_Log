import { Link } from "react-router-dom";
import "./LandingPage.css";

export default function LandingPage() {
  return (
    <main className="simple-landing-page">
      <Link to="/autentificare" className="landing-auth-button">
        Autentificare
      </Link>

      <section className="landing-logo-area">
        <img
          src="/DAYA_Logo.svg"
          alt="DAYA Log"
          className="simple-landing-logo"
        />
      </section>

      <Link to="/informatii" className="landing-info-button">
        Informații despre aplicație
      </Link>
    </main>
  );
}