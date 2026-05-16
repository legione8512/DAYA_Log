import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { confirmEmail } from "../api/authApi";
import { getApiErrorMessage } from "../utils/apiErrors";

export default function ConfirmEmailPage() {
  const [searchParams] = useSearchParams();
  const token = useMemo(() => searchParams.get("token") || "", [searchParams]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function runConfirmation() {
      if (!token) {
        setError("Linkul de confirmare este invalid sau lipsește tokenul.");
        setLoading(false);
        return;
      }

      try {
        const response = await confirmEmail(token);

        if (isMounted) {
          setSuccess(response?.message || "Adresa de email a fost confirmată cu succes.");
        }
      } catch (err) {
        if (isMounted) {
          setError(
            getApiErrorMessage(
              err,
              "Nu am putut confirma adresa de email. Linkul poate fi expirat sau deja folosit."
            )
          );
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    runConfirmation();

    return () => {
      isMounted = false;
    };
  }, [token]);

  return (
    <div>
      <h2>Confirmare email</h2>
      <p>Verificăm linkul de confirmare pentru contul tău DAYA Log.</p>

      {loading && <div className="page-loading auth-inline-card">Se confirmă adresa de email...</div>}
      {error && <div className="form-error auth-inline-card">{error}</div>}
      {success && <div className="form-success auth-inline-card">{success}</div>}

      <p className="muted-text auth-helper-text">
        <Link to="/autentificare">Înapoi la autentificare</Link>
      </p>
    </div>
  );
}
