import { useState } from "react";
import { Link } from "react-router-dom";
import { requestPasswordReset } from "../api/authApi";
import { getApiErrorMessage } from "../utils/apiErrors";

export default function RequestPasswordResetPage() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      const response = await requestPasswordReset({ email: email.trim() });
      setSuccess(
        response?.message ||
          "Dacă adresa există în sistem, vei primi un email cu instrucțiuni pentru resetarea parolei."
      );
      setEmail("");
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          "Nu am putut trimite cererea de resetare. Verifică adresa de email și încearcă din nou."
        )
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Resetare parolă</h2>
      <p>Introdu adresa de email asociată contului tău DAYA Log.</p>

      <form onSubmit={handleSubmit} className="form-card">
        <label>
          Email
          <input
            type="email"
            name="email"
            placeholder="exemplu@email.ro"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="email"
            required
          />
        </label>

        {error && <div className="form-error">{error}</div>}
        {success && <div className="form-success">{success}</div>}

        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? "Se trimite..." : "Trimite linkul de resetare"}
        </button>
      </form>

      <p className="muted-text auth-helper-text">
        Ți-ai amintit parola? <Link to="/autentificare">Înapoi la autentificare</Link>
      </p>
    </div>
  );
}
