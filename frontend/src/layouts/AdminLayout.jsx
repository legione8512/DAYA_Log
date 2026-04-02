import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "../styles/layout.css";

export default function AdminLayout() {
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
          <p>Panou administrator</p>
        </div>

        <nav className="sidebar-nav">
          <Link to="/admin">Dashboard</Link>
          <Link to="/admin/programari">Programări</Link>
          <Link to="/admin/clienti">Clienți</Link>
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