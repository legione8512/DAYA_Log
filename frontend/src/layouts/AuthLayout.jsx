import { Outlet } from "react-router-dom";
import "../styles/layout.css";

export default function AuthLayout() {
  return (
    <div className="auth-layout">
      <div className="auth-card">
        <div className="brand-block">
          <p className="brand-kicker">DAYA Log</p>
          <h1>Bine ai venit</h1>
          <p className="brand-text">
            Platforma de programări și gestionare pentru studioul tău Pilates.
          </p>
        </div>

        <div className="auth-content">
          <Outlet />
        </div>
      </div>
    </div>
  );
}