import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

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

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const user = await login(form.email, form.password, form.rememberMe);

      if (user.role === "ADMIN") {
        navigate("/admin");
      } else {
        navigate("/client");
      }
    } catch (err) {
      setError(
        err?.response?.data?.message || "Autentificarea a eșuat. Verifică datele introduse."
      );
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
    </div>
  );
}