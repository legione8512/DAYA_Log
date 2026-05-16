import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { changePassword } from "../../api/profileApi";
import { useAuth } from "../../context/AuthContext";
import { getApiErrorMessage } from "../../utils/apiErrors";

const EMPTY_FORM = {
  currentPassword: "",
  newPassword: "",
  confirmNewPassword: "",
};

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");

    try {
      const response = await changePassword(form);
      setSuccess(response?.message || "Parola a fost actualizată cu succes.");
      setForm(EMPTY_FORM);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut schimba parola."));
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate("/autentificare", { replace: true });
  };

  const goBack = () => {
    navigate(user?.role === "ADMIN" ? "/admin" : "/client");
  };

  return (
    <main className="profile-page-shell">
      <section className="profile-card">
        <div className="page-header">
          <div>
            <p className="page-kicker">Profil</p>
            <h1>Contul meu</h1>
            <p>Gestionează datele de autentificare și parola contului.</p>
          </div>
          <button className="secondary-button" type="button" onClick={goBack}>Înapoi</button>
        </div>

        {user?.forcePasswordChange && (
          <div className="info-banner">
            Este necesar să îți schimbi parola la prima autentificare.
          </div>
        )}

        <article className="content-card">
          <h2>Informații cont</h2>
          <div className="details-grid">
            <div>
              <span>Email</span>
              <strong>{user?.email}</strong>
            </div>
            <div>
              <span>Rol</span>
              <strong>{user?.role}</strong>
            </div>
            <div>
              <span>Studio ID</span>
              <strong>{user?.studioId}</strong>
            </div>
          </div>
        </article>

        <article className="content-card">
          <h2>Schimbă parola</h2>

          <form className="form-card" onSubmit={handleSubmit}>
            <label>
              Parola curentă
              <input
                type="password"
                name="currentPassword"
                value={form.currentPassword}
                onChange={handleChange}
                autoComplete="current-password"
                required
              />
            </label>

            <label>
              Parola nouă
              <input
                type="password"
                name="newPassword"
                value={form.newPassword}
                onChange={handleChange}
                autoComplete="new-password"
                required
              />
            </label>

            <label>
              Confirmă parola nouă
              <input
                type="password"
                name="confirmNewPassword"
                value={form.confirmNewPassword}
                onChange={handleChange}
                autoComplete="new-password"
                required
              />
            </label>

            {error && <div className="form-error">{error}</div>}
            {success && <div className="form-success">{success}</div>}

            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? "Se salvează..." : "Salvează parola nouă"}
            </button>
          </form>
        </article>

        <button className="secondary-button" type="button" onClick={handleLogout}>
          Deconectare
        </button>
      </section>
    </main>
  );
}
