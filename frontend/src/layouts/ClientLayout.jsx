import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "../styles/layout.css";

export default function ClientLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/autentificare", { replace: true });
  };

  return (
    <div className="dashboard-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h2>DAYA Log</h2>
          <p>Cont client</p>
        </div>

        <nav className="sidebar-nav">
          <NavLink to="/client" end>Panou principal</NavLink>
          <NavLink to="/client/programarile-mele">Programările mele</NavLink>
          <NavLink to="/profil">Profil</NavLink>
        </nav>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div>
            <strong>Client</strong>
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
