import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getApiErrorMessage } from "../utils/apiErrors";

function getDefaultPath(authenticatedUser) {
  if (authenticatedUser?.forcePasswordChange) {
    return "/profil";
  }

  return authenticatedUser?.role === "ADMIN" ? "/admin" : "/client";
}

export default function LoginPage() {
  const { login, user, loading: sessionLoading, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({
    email: "",
    password: "",
    rememberMe: true,
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const getRedirectPath = (authenticatedUser) => {
    const requestedPath = location.state?.from?.pathname;

    if (requestedPath && requestedPath !== "/autentificare") {
      return requestedPath;
    }

    return getDefaultPath(authenticatedUser);
  };

  useEffect(() => {
    if (!sessionLoading && isAuthenticated) {
      navigate(getDefaultPath(user), { replace: true });
    }
  }, [sessionLoading, isAuthenticated, user, navigate]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const user = await login(form.email.trim(), form.password, form.rememberMe);
      navigate(getRedirectPath(user), { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err, "Autentificarea a eșuat. Verifică datele introduse."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Autentificare</h2>
      <p>Introdu adresa de email și parola pentru a continua.</p>

      <form onSubmit={handleSubmit} className="form-card">
        <label>
          Email
          <input
            type="email"
            name="email"
            placeholder="exemplu@email.ro"
            value={form.email}
            onChange={handleChange}
            autoComplete="email"
            required
          />
        </label>

        <label>
          Parolă
          <input
            type="password"
            name="password"
            placeholder="Introdu parola"
            value={form.password}
            onChange={handleChange}
            autoComplete="current-password"
            required
          />
        </label>

        <label className="checkbox-row">
          <input
            type="checkbox"
            name="rememberMe"
            checked={form.rememberMe}
            onChange={handleChange}
          />
          Ține-mă minte
        </label>

        {error && <div className="form-error">{error}</div>}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? "Se autentifică..." : "Intră în cont"}
        </button>
      </form>

      <p className="muted-text auth-helper-text">
        Ai uitat parola? <Link to="/resetare-parola">Resetează parola</Link>
      </p>
    </div>
  );
}
