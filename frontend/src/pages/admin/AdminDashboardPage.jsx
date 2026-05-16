import { useEffect, useState } from "react";
import { getAdminDashboard } from "../../api/dashboardApi";
import { getApiErrorMessage } from "../../utils/apiErrors";

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function loadDashboard() {
      try {
        const data = await getAdminDashboard();

        if (isMounted) {
          setDashboard(data);
        }
      } catch (err) {
        if (isMounted) {
          setError(getApiErrorMessage(err, "Nu am putut încărca dashboard-ul."));
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
    return <div className="page-loading">Se încarcă dashboard-ul...</div>;
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">DAYA Log</p>
          <h1>Dashboard administrator</h1>
          <p>Rezumat rapid pentru clienți, programări și catalog.</p>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      {dashboard && (
        <div className="summary-grid">
          <SummaryCard label="Clienți activi" value={dashboard.activeClientsCount} />
          <SummaryCard label="Programări astăzi" value={dashboard.todayAppointmentsCount} />
          <SummaryCard label="Programări viitoare" value={dashboard.upcomingAppointmentsCount} />
          <SummaryCard label="Servicii active" value={dashboard.activeServicesCount} />
          <SummaryCard label="Instructori activi" value={dashboard.activeInstructorsCount} />
          <SummaryCard label="Resurse active" value={dashboard.activeResourcesCount} />
        </div>
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
