import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "../styles/layout.css";

export default function AdminLayout() {
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
          <p>Panou administrator</p>
        </div>

        <nav className="sidebar-nav">
          <NavLink to="/admin" end>Dashboard</NavLink>
          <NavLink to="/admin/programari">Programări</NavLink>
          <NavLink to="/admin/clienti">Clienți</NavLink>
          <NavLink to="/admin/servicii">Servicii</NavLink>
          <NavLink to="/admin/instructori">Instructori</NavLink>
          <NavLink to="/admin/resurse">Resurse</NavLink>
          <NavLink to="/admin/audit">Audit</NavLink>
          <NavLink to="/profil">Profil</NavLink>
        </nav>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div>
            <strong>{user?.role === "ADMIN" ? "Administrator" : "Utilizator"}</strong>
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
