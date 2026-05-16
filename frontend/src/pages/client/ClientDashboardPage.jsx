import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getClientDashboard } from "../../api/dashboardApi";
import { getApiErrorMessage } from "../../utils/apiErrors";
import { formatDateTime } from "../../utils/dateTime";

export default function ClientDashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function loadDashboard() {
      try {
        const data = await getClientDashboard();

        if (isMounted) {
          setDashboard(data);
        }
      } catch (err) {
        if (isMounted) {
          setError(getApiErrorMessage(err, "Nu am putut încărca panoul clientului."));
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadDashboard();

    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return <div className="page-loading">Se încarcă panoul tău...</div>;
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Cont client</p>
          <h1>Bun venit{dashboard?.firstName ? `, ${dashboard.firstName}` : ""}</h1>
          <p>Aici vezi rapid programările tale viitoare și istoricul.</p>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      {dashboard && (
        <>
          <div className="summary-grid two-columns">
            <SummaryCard label="Programări viitoare" value={dashboard.futureAppointmentsCount} />
            <SummaryCard label="Programări în istoric" value={dashboard.historyAppointmentsCount} />
          </div>

          <article className="content-card next-appointment-card">
            <h2>Următoarea programare</h2>

            {dashboard.nextAppointment ? (
              <div className="appointment-highlight">
                <strong>{dashboard.nextAppointment.serviceName}</strong>
                <p>{formatDateTime(dashboard.nextAppointment.startAt)}</p>
                <p>Instructor: {dashboard.nextAppointment.instructorName}</p>
                <p>Status: {dashboard.nextAppointment.status}</p>
              </div>
            ) : (
              <p className="muted-text">Nu ai programări viitoare în acest moment.</p>
            )}

            <Link className="primary-link-button" to="/client/programarile-mele">
              Vezi toate programările
            </Link>
          </article>
        </>
      )}
    </section>
  );
}

function SummaryCard({ label, value }) {
  return (
    <article className="summary-card">
      <p>{label}</p>
      <strong>{value ?? 0}</strong>
    </article>
  );
}
