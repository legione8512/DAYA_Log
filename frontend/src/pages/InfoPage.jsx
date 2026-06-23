import { Link } from "react-router-dom";
import "./InfoPage.css";

export default function InfoPage() {
  return (
    <main className="landing-page">
      <nav className="landing-nav">
        <Link to="/" className="landing-brand" aria-label="DAYA Log home">
          <span className="landing-brand-mark">D</span>
          <span>DAYA Log</span>
        </Link>

        <Link to="/autentificare" className="landing-login-link">
          Autentificare
        </Link>
      </nav>

      <section className="landing-hero">
        <div className="landing-hero-content">
          <p className="landing-kicker">Aplicație pentru salonul tău de Pilates</p>

          <h1>Organizează programările, clienții și instructorii dintr-un singur loc.</h1>

          <p className="landing-description">
            DAYA Log te ajută să gestionezi rapid activitatea zilnică a studioului:
            programări, clienți, instructori, servicii și evidență administrativă.
          </p>

          <div className="landing-actions">
            <Link to="/autentificare" className="landing-primary-button">
              Intră în aplicație
            </Link>

            <a href="#functionalitati" className="landing-secondary-button">
              Vezi funcționalitățile
            </a>
          </div>
        </div>

        <aside className="landing-preview-card" aria-label="Rezumat studio">
          <div className="landing-preview-header">
            <span>Astăzi</span>
            <strong>Rezumat studio</strong>
          </div>

          <div className="landing-preview-row">
            <span>Programări</span>
            <strong>18</strong>
          </div>

          <div className="landing-preview-row">
            <span>Clienți activi</span>
            <strong>124</strong>
          </div>

          <div className="landing-preview-row">
            <span>Instructori</span>
            <strong>5</strong>
          </div>

          <div className="landing-next-session">
            <span>Următoarea sesiune</span>
            <strong>Reformer Pilates</strong>
            <p>18:30 · Sala 2</p>
          </div>
        </aside>
      </section>

      <section id="functionalitati" className="landing-features">
        <div className="landing-section-heading">
          <p>Funcționalități principale</p>
          <h2>Creat pentru administrarea simplă a unui studio Pilates.</h2>
        </div>

        <div className="landing-feature-grid">
          <article className="landing-feature-card">
            <h3>Gestionare clienți</h3>
            <p>
              Păstrezi datele clienților, statusul lor și istoricul programărilor într-o
              structură clară.
            </p>
          </article>

          <article className="landing-feature-card">
            <h3>Programări</h3>
            <p>
              Organizezi clasele și sesiunile individuale, cu acces rapid la detaliile
              fiecărei programări.
            </p>
          </article>

          <article className="landing-feature-card">
            <h3>Instructori</h3>
            <p>
              Urmărești instructorii, activitatea lor și sesiunile la care sunt alocați.
            </p>
          </article>

          <article className="landing-feature-card">
            <h3>Dashboard</h3>
            <p>
              Vezi rapid informațiile importante despre activitatea studioului și
              programul curent.
            </p>
          </article>
        </div>
      </section>

      <section className="landing-cta">
        <h2>Ai deja cont?</h2>
        <p>Intră în aplicație și continuă administrarea salonului tău de Pilates.</p>

        <Link to="/autentificare" className="landing-cta-button">
          Mergi la autentificare
        </Link>
      </section>
    </main>
  );
}
