import { useEffect, useState } from "react";
import {
  getClientAppointmentHistory,
  getClientFutureAppointments,
} from "../../api/appointmentApi";
import { getApiErrorMessage } from "../../utils/apiErrors";
import { formatAppointmentInterval, formatDate } from "../../utils/dateTime";

export default function MyAppointmentsPage() {
  const [activeTab, setActiveTab] = useState("future");
  const [futureAppointments, setFutureAppointments] = useState([]);
  const [historyAppointments, setHistoryAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function loadAppointments() {
      try {
        const [future, history] = await Promise.all([
          getClientFutureAppointments(),
          getClientAppointmentHistory(),
        ]);

        if (isMounted) {
          setFutureAppointments(future);
          setHistoryAppointments(history);
        }
      } catch (err) {
        if (isMounted) {
          setError(getApiErrorMessage(err, "Nu am putut încărca programările."));
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadAppointments();

    return () => {
      isMounted = false;
    };
  }, []);

  const appointments = activeTab === "future" ? futureAppointments : historyAppointments;

  if (loading) {
    return <div className="page-loading">Se încarcă programările...</div>;
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Cont client</p>
          <h1>Programările mele</h1>
          <p>Vezi programările viitoare și istoricul complet.</p>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="tab-row">
        <button
          className={activeTab === "future" ? "tab-button active" : "tab-button"}
          onClick={() => setActiveTab("future")}
        >
          Viitoare
        </button>
        <button
          className={activeTab === "history" ? "tab-button active" : "tab-button"}
          onClick={() => setActiveTab("history")}
        >
          Istoric
        </button>
      </div>

      <AppointmentTable appointments={appointments} emptyType={activeTab} />
    </section>
  );
}

function AppointmentTable({ appointments, emptyType }) {
  if (!appointments.length) {
    return (
      <div className="empty-state">
        {emptyType === "future"
          ? "Nu există programări viitoare."
          : "Nu există programări anterioare."}
      </div>
    );
  }

  return (
    <div className="table-card">
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Interval</th>
            <th>Serviciu</th>
            <th>Instructor</th>
            <th>Tip</th>
            <th>Status</th>
            <th>Resursă</th>
          </tr>
        </thead>
        <tbody>
          {appointments.map((appointment) => (
            <tr key={appointment.id}>
              <td>{formatDate(appointment.startAt)}</td>
              <td>{formatAppointmentInterval(appointment.startAt, appointment.endAt)}</td>
              <td>{appointment.serviceName}</td>
              <td>{appointment.instructorName}</td>
              <td>{appointment.appointmentType}</td>
              <td><span className="status-pill">{appointment.status}</span></td>
              <td>{appointment.resourceName || "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
