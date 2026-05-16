import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  cancelAppointment,
  getAdminAppointments,
  sendAppointmentConfirmation,
} from "../../api/appointmentApi";
import { getApiErrorMessage } from "../../utils/apiErrors";

const DEFAULT_FILTERS = {
  status: "",
  appointmentType: "",
  dateFrom: "",
  dateTo: "",
  page: 0,
  size: 20,
};

export default function AppointmentsPage() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [appointments, setAppointments] = useState([]);
  const [pageInfo, setPageInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const location = useLocation();

  const loadAppointments = async (currentFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await getAdminAppointments(cleanFilters(currentFilters));
      setAppointments(data.content || []);
      setPageInfo(data);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca programările."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAppointments(DEFAULT_FILTERS);

    if (location.state?.success) {
      setSuccess(location.state.success);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((prev) => ({ ...prev, [name]: value, page: 0 }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    loadAppointments(filters);
  };

  const handleReset = () => {
    setFilters(DEFAULT_FILTERS);
    loadAppointments(DEFAULT_FILTERS);
  };

  const handleSendConfirmation = async (id) => {
    setActionLoadingId(id);
    setError("");
    setSuccess("");

    try {
      const response = await sendAppointmentConfirmation(id);
      setSuccess(response?.message || "Emailul de confirmare a fost trimis.");
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut trimite emailul de confirmare."));
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleCancel = async (id) => {
    const confirmed = window.confirm("Sigur vrei să anulezi această programare?");

    if (!confirmed) {
      return;
    }

    setActionLoadingId(id);
    setError("");
    setSuccess("");

    try {
      await cancelAppointment(id, { reason: "Anulată din interfața admin." });
      setSuccess("Programarea a fost anulată.");
      await loadAppointments(filters);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut anula programarea."));
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Administrare</p>
          <h1>Programări</h1>
          <p>Listă operațională pentru programările studioului.</p>
        </div>
        <Link className="primary-link-button" to="/admin/programari/noua">
          Programare nouă
        </Link>
      </div>

      <form className="filter-card" onSubmit={handleSubmit}>
        <label>
          Status
          <select name="status" value={filters.status} onChange={handleFilterChange}>
            <option value="">Toate</option>
            <option value="SCHEDULED">SCHEDULED</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="CANCELLED">CANCELLED</option>
            <option value="NO_SHOW">NO_SHOW</option>
          </select>
        </label>

        <label>
          Tip
          <select name="appointmentType" value={filters.appointmentType} onChange={handleFilterChange}>
            <option value="">Toate</option>
            <option value="INDIVIDUAL">INDIVIDUAL</option>
            <option value="GROUP">GROUP</option>
          </select>
        </label>

        <label>
          De la
          <input type="date" name="dateFrom" value={filters.dateFrom} onChange={handleFilterChange} />
        </label>

        <label>
          Până la
          <input type="date" name="dateTo" value={filters.dateTo} onChange={handleFilterChange} />
        </label>

        <div className="filter-actions">
          <button className="primary-button" type="submit">Aplică filtre</button>
          <button className="secondary-button" type="button" onClick={handleReset}>Resetează</button>
        </div>
      </form>

      {error && <div className="form-error">{error}</div>}
      {success && <div className="form-success">{success}</div>}

      {loading ? (
        <div className="page-loading">Se încarcă programările...</div>
      ) : (
        <AppointmentsTable
          appointments={appointments}
          actionLoadingId={actionLoadingId}
          onCancel={handleCancel}
          onSendConfirmation={handleSendConfirmation}
        />
      )}

      {pageInfo && (
        <p className="muted-text table-footer-text">
          Pagina {pageInfo.page + 1} din {pageInfo.totalPages || 1}. Total: {pageInfo.totalElements || 0} programări.
        </p>
      )}
    </section>
  );
}

function AppointmentsTable({ appointments, actionLoadingId, onCancel, onSendConfirmation }) {
  if (!appointments.length) {
    return <div className="empty-state">Nu există programări pentru filtrele selectate.</div>;
  }

  return (
    <div className="table-card">
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Interval</th>
            <th>Tip</th>
            <th>Serviciu</th>
            <th>Instructor</th>
            <th>Participanți</th>
            <th>Status</th>
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {appointments.map((appointment) => (
            <tr key={appointment.id}>
              <td>{appointment.date}</td>
              <td>{appointment.timeRange}</td>
              <td>{appointment.appointmentType}</td>
              <td>{appointment.serviceName}</td>
              <td>{appointment.instructorName}</td>
              <td>{appointment.participantCount}</td>
              <td><span className="status-pill">{appointment.status}</span></td>
              <td>
                <div className="row-actions">
                  <Link className="small-link-button" to={`/admin/programari/${appointment.id}`}>
                    Detalii
                  </Link>
                  <Link className="small-link-button" to={`/admin/programari/${appointment.id}/editeaza`}>
                    Editează
                  </Link>
                  <button
                    className="small-button"
                    type="button"
                    disabled={actionLoadingId === appointment.id}
                    onClick={() => onSendConfirmation(appointment.id)}
                  >
                    Confirmare
                  </button>
                  <button
                    className="small-button danger"
                    type="button"
                    disabled={actionLoadingId === appointment.id || appointment.status === "CANCELLED"}
                    onClick={() => onCancel(appointment.id)}
                  >
                    Anulează
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function cleanFilters(filters) {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== "" && value !== null && value !== undefined)
  );
}
