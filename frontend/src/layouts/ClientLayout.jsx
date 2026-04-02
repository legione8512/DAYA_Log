import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "../styles/layout.css";

export default function ClientLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/autentificare");
  };

  return (
    <div className="dashboard-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h2>DAYA Log</h2>
          <p>Cont client</p>
        </div>

        <nav className="sidebar-nav">
          <Link to="/client">Panou principal</Link>
          <Link to="/client/programarile-mele">Programările mele</Link>
        </nav>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div>
            <strong>{user?.firstName} {user?.lastName}</strong>
            <p>{user?.email}</p>
          </div>

          <button className="secondary-button" onClick={handleLogout}>
            Deconectare
          </button>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}