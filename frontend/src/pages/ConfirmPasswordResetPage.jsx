import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { confirmPasswordReset } from "../api/authApi";
import { getApiErrorMessage } from "../utils/apiErrors";

const initialForm = {
  newPassword: "",
  confirmPassword: "",
};

export default function ConfirmPasswordResetPage() {
  const [searchParams] = useSearchParams();
  const token = useMemo(() => searchParams.get("token") || "", [searchParams]);

  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const validateForm = () => {
    if (!token) {
      return "Linkul de resetare este invalid sau lipsește tokenul.";
    }

    if (form.newPassword.length < 10) {
      return "Parola nouă trebuie să aibă minimum 10 caractere.";
    }

    if (form.newPassword !== form.confirmPassword) {
      return "Parolele introduse nu coincid.";
    }

    return "";
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const validationError = validateForm();

    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);

    try {
      const response = await confirmPasswordReset({
        token,
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword,
      });

      setSuccess(response?.message || "Parola a fost resetată cu succes.");
      setForm(initialForm);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          "Nu am putut reseta parola. Linkul poate fi expirat sau deja folosit."
        )
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Setează parola nouă</h2>
      <p>Alege o parolă nouă pentru contul tău.</p>

      {!token && (
        <div className="form-error">
          Linkul de resetare nu conține un token valid. Cere un link nou de resetare.
        </div>
      )}

      <form onSubmit={handleSubmit} className="form-card">
        <label>
          Parolă nouă
          <input
            type="password"
            name="newPassword"
            placeholder="Minimum 10 caractere"
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
            name="confirmPassword"
            placeholder="Repetă parola nouă"
            value={form.confirmPassword}
            onChange={handleChange}
            autoComplete="new-password"
            required
          />
        </label>

        {error && <div className="form-error">{error}</div>}
        {success && <div className="form-success">{success}</div>}

        <button className="primary-button" type="submit" disabled={loading || !token}>
          {loading ? "Se salvează..." : "Salvează parola nouă"}
        </button>
      </form>

      <p className="muted-text auth-helper-text">
        <Link to="/autentificare">Înapoi la autentificare</Link>
      </p>
    </div>
  );
}
