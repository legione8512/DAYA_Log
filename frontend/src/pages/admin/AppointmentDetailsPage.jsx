import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import {
  addAppointmentParticipants,
  addAppointmentWaitlistEntry,
  cancelAppointment,
  changeAppointmentStatus,
  getAdminAppointment,
  getAppointmentWaitlist,
  promoteAppointmentWaitlistEntry,
  removeAppointmentParticipant,
  removeAppointmentWaitlistEntry,
  sendAppointmentConfirmation,
} from "../../api/appointmentApi";
import { searchClients } from "../../api/clientApi";
import { getApiErrorMessage } from "../../utils/apiErrors";
import { formatAppointmentInterval, formatDateTime } from "../../utils/dateTime";

const STATUS_OPTIONS = ["SCHEDULED", "CONFIRMED", "COMPLETED", "CANCELLED", "NO_SHOW"];

export default function AppointmentDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [appointment, setAppointment] = useState(null);
  const [waitlist, setWaitlist] = useState([]);
  const [clients, setClients] = useState([]);
  const [clientQuery, setClientQuery] = useState("");
  const [selectedClientIds, setSelectedClientIds] = useState([]);
  const [selectedWaitlistClientId, setSelectedWaitlistClientId] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadingClients, setLoadingClients] = useState(false);
  const [actionLoading, setActionLoading] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadAppointment = useCallback(async () => {
    const data = await getAdminAppointment(id);
    setAppointment(data);
    setStatus(data.status || "");
  }, [id]);

  const loadWaitlist = useCallback(async () => {
    const data = await getAppointmentWaitlist(id);
    setWaitlist(Array.isArray(data) ? data : []);
  }, [id]);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      await Promise.all([loadAppointment(), loadWaitlist()]);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca detaliile programării."));
    } finally {
      setLoading(false);
    }
  }, [loadAppointment, loadWaitlist]);

  const loadClients = useCallback(async (query) => {
    setLoadingClients(true);

    try {
      const data = await searchClients({ query, active: true });
      setClients(Array.isArray(data) ? data : data.content || []);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca lista de clienți."));
    } finally {
      setLoadingClients(false);
    }
  }, []);

  useEffect(() => {
    loadAll();

    if (location.state?.success) {
      setSuccess(location.state.success);
    }
  }, [loadAll, location.state?.success]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadClients(clientQuery);
    }, 300);

    return () => window.clearTimeout(timer);
  }, [clientQuery, loadClients]);

  const participantIds = useMemo(
    () => new Set((appointment?.participants || []).map((participant) => participant.clientId)),
    [appointment]
  );

  const waitlistClientIds = useMemo(
    () => new Set(waitlist.map((entry) => entry.clientId)),
    [waitlist]
  );

  const availableClientsForParticipants = useMemo(
    () => clients.filter((client) => !participantIds.has(client.id)),
    [clients, participantIds]
  );

  const availableClientsForWaitlist = useMemo(
    () => clients.filter((client) => !participantIds.has(client.id) && !waitlistClientIds.has(client.id)),
    [clients, participantIds, waitlistClientIds]
  );

  const isGroup = appointment?.appointmentType === "GROUP";
  const isCancelled = appointment?.status === "CANCELLED";

  const runAction = async (actionName, action, fallbackMessage) => {
    setActionLoading(actionName);
    setError("");
    setSuccess("");

    try {
      await action();
      setSuccess(fallbackMessage);
    } catch (err) {
      setError(getApiErrorMessage(err, fallbackMessage));
    } finally {
      setActionLoading("");
    }
  };

  const handleSendConfirmation = () => {
    runAction(
      "send-confirmation",
      async () => {
        const response = await sendAppointmentConfirmation(id);
        setSuccess(response?.message || "Emailul de confirmare a fost trimis.");
      },
      "Emailul de confirmare a fost trimis."
    );
  };

  const handleCancel = () => {
    const confirmed = window.confirm("Sigur vrei să anulezi această programare?");

    if (!confirmed) {
      return;
    }

    runAction(
      "cancel",
      async () => {
        await cancelAppointment(id, { reason: "Anulată din pagina de detalii." });
        await loadAppointment();
      },
      "Programarea a fost anulată."
    );
  };

  const handleChangeStatus = () => {
    if (!status || status === appointment.status) {
      setError("Alege un status diferit înainte de salvare.");
      return;
    }

    runAction(
      "change-status",
      async () => {
        const updated = await changeAppointmentStatus(id, status);
        setAppointment(updated);
        setStatus(updated.status || "");
      },
      "Statusul programării a fost actualizat."
    );
  };

  const toggleSelectedClient = (clientId) => {
    setSelectedClientIds((prev) =>
      prev.includes(clientId)
        ? prev.filter((idValue) => idValue !== clientId)
        : [...prev, clientId]
    );
  };

  const handleAddParticipants = () => {
    if (!selectedClientIds.length) {
      setError("Selectează cel puțin un client pentru adăugare.");
      return;
    }

    runAction(
      "add-participants",
      async () => {
        const updated = await addAppointmentParticipants(id, selectedClientIds);
        setAppointment(updated);
        setSelectedClientIds([]);
        await loadWaitlist();
      },
      "Participanții au fost adăugați."
    );
  };

  const handleRemoveParticipant = (clientId) => {
    const confirmed = window.confirm("Sigur vrei să elimini acest participant din programare?");

    if (!confirmed) {
      return;
    }

    runAction(
      `remove-participant-${clientId}`,
      async () => {
        const updated = await removeAppointmentParticipant(id, clientId);
        setAppointment(updated);
        setSelectedClientIds((prev) => prev.filter((item) => item !== clientId));
      },
      "Participantul a fost eliminat."
    );
  };

  const handleAddWaitlistEntry = () => {
    if (!selectedWaitlistClientId) {
      setError("Selectează un client pentru lista de așteptare.");
      return;
    }

    runAction(
      "add-waitlist",
      async () => {
        const updatedWaitlist = await addAppointmentWaitlistEntry(id, selectedWaitlistClientId);
        setWaitlist(updatedWaitlist);
        setSelectedWaitlistClientId("");
      },
      "Clientul a fost adăugat pe lista de așteptare."
    );
  };

  const handleRemoveWaitlistEntry = (waitlistEntryId) => {
    const confirmed = window.confirm("Sigur vrei să elimini clientul din lista de așteptare?");

    if (!confirmed) {
      return;
    }

    runAction(
      `remove-waitlist-${waitlistEntryId}`,
      async () => {
        const updatedWaitlist = await removeAppointmentWaitlistEntry(id, waitlistEntryId);
        setWaitlist(updatedWaitlist);
      },
      "Clientul a fost eliminat din lista de așteptare."
    );
  };

  const handlePromoteWaitlistEntry = (waitlistEntryId) => {
    const confirmed = window.confirm("Sigur vrei să promovezi manual acest client în programare?");

    if (!confirmed) {
      return;
    }

    runAction(
      `promote-waitlist-${waitlistEntryId}`,
      async () => {
        const result = await promoteAppointmentWaitlistEntry(id, waitlistEntryId);
        setAppointment(result.appointment);
        setWaitlist(result.waitlist || []);
      },
      "Clientul a fost promovat manual din lista de așteptare."
    );
  };

  if (loading) {
    return <div className="page-loading">Se încarcă detaliile programării...</div>;
  }

  if (!appointment) {
    return (
      <section>
        <div className="page-header">
          <div>
            <p className="page-kicker">Administrare programări</p>
            <h1>Programare negăsită</h1>
          </div>
          <Link className="secondary-link-button" to="/admin/programari">
            Înapoi la listă
          </Link>
        </div>
        {error && <div className="form-error">{error}</div>}
      </section>
    );
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Administrare programări</p>
          <h1>Detalii programare</h1>
          <p>{appointment.serviceName} · {appointment.instructorName}</p>
        </div>
        <div className="header-actions">
          <Link className="secondary-link-button" to="/admin/programari">
            Înapoi la listă
          </Link>
          <Link className="secondary-link-button" to={`/admin/programari/${id}/editeaza`}>
            Editează
          </Link>
          <button className="secondary-button" type="button" onClick={() => navigate("/admin/programari/noua")}>
            Programare nouă
          </button>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}
      {success && <div className="form-success">{success}</div>}

      <div className="details-grid appointment-details-grid">
        <DetailItem label="Tip" value={appointment.appointmentType} />
        <DetailItem label="Status" value={appointment.status} />
        <DetailItem label="Capacitate" value={appointment.capacity} />
        <DetailItem label="Serviciu" value={appointment.serviceName} />
        <DetailItem label="Instructor" value={appointment.instructorName} />
        <DetailItem label="Resursă" value={appointment.resourceName || "Fără resursă"} />
        <DetailItem label="Început" value={formatDateTime(appointment.startAt)} />
        <DetailItem label="Sfârșit" value={formatDateTime(appointment.endAt)} />
        <DetailItem label="Interval" value={formatAppointmentInterval(appointment.startAt, appointment.endAt)} />
      </div>

      {appointment.notes && (
        <div className="content-card">
          <h2>Notițe interne</h2>
          <p>{appointment.notes}</p>
        </div>
      )}

      <div className="content-card details-actions-card">
        <div>
          <h2>Acțiuni programare</h2>
          <p className="muted-text">Aceste acțiuni folosesc regulile backend-ului pentru status, anulare și email.</p>
        </div>

        <div className="details-action-row">
          <label>
            Schimbă statusul
            <select value={status} onChange={(event) => setStatus(event.target.value)}>
              {STATUS_OPTIONS.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
          <button
            className="primary-button"
            type="button"
            disabled={actionLoading === "change-status" || status === appointment.status}
            onClick={handleChangeStatus}
          >
            {actionLoading === "change-status" ? "Se salvează..." : "Salvează status"}
          </button>
        </div>

        <div className="row-actions">
          <button
            className="small-button"
            type="button"
            disabled={actionLoading === "send-confirmation" || isCancelled}
            onClick={handleSendConfirmation}
          >
            Trimite confirmarea
          </button>
          <button
            className="small-button danger"
            type="button"
            disabled={actionLoading === "cancel" || isCancelled}
            onClick={handleCancel}
          >
            Anulează programarea
          </button>
        </div>
      </div>

      <div className="content-card">
        <h2>Participanți</h2>
        <ParticipantsTable
          appointment={appointment}
          actionLoading={actionLoading}
          onRemove={handleRemoveParticipant}
        />
      </div>

      {isGroup ? (
        <div className="content-card">
          <h2>Adaugă participanți</h2>
          <ClientSearchBox
            clientQuery={clientQuery}
            loadingClients={loadingClients}
            onClientQueryChange={setClientQuery}
          />
          <SelectableClientList
            clients={availableClientsForParticipants}
            selectedIds={selectedClientIds}
            onToggle={toggleSelectedClient}
            emptyText="Nu există clienți activi disponibili pentru adăugare."
          />
          <div className="modal-actions">
            <button
              className="primary-button"
              type="button"
              disabled={actionLoading === "add-participants" || isCancelled}
              onClick={handleAddParticipants}
            >
              {actionLoading === "add-participants" ? "Se adaugă..." : "Adaugă participanți"}
            </button>
          </div>
        </div>
      ) : (
        <div className="info-banner">
          Programările individuale pot avea un singur participant. Pentru mai mulți participanți creează o programare de grup.
        </div>
      )}

      {isGroup && (
        <div className="content-card">
          <h2>Listă de așteptare</h2>
          <p className="muted-text">
            Aceasta este gestionare manuală. Nu există promovare automată din waitlist.
          </p>
          <div className="details-action-row waitlist-add-row">
            <label>
              Client disponibil
              <select
                value={selectedWaitlistClientId}
                onChange={(event) => setSelectedWaitlistClientId(event.target.value)}
              >
                <option value="">Selectează clientul</option>
                {availableClientsForWaitlist.map((client) => (
                  <option key={client.id} value={client.id}>
                    {client.fullName} · {client.email || client.phone || "fără contact"}
                  </option>
                ))}
              </select>
            </label>
            <button
              className="primary-button"
              type="button"
              disabled={actionLoading === "add-waitlist" || isCancelled}
              onClick={handleAddWaitlistEntry}
            >
              {actionLoading === "add-waitlist" ? "Se adaugă..." : "Adaugă în waitlist"}
            </button>
          </div>
          <WaitlistTable
            waitlist={waitlist}
            actionLoading={actionLoading}
            isCancelled={isCancelled}
            onRemove={handleRemoveWaitlistEntry}
            onPromote={handlePromoteWaitlistEntry}
          />
        </div>
      )}
    </section>
  );
}

function DetailItem({ label, value }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value || "-"}</strong>
    </div>
  );
}

function ParticipantsTable({ appointment, actionLoading, onRemove }) {
  const participants = appointment.participants || [];
  const canRemove = appointment.appointmentType === "GROUP" && appointment.status !== "CANCELLED";

  if (!participants.length) {
    return <div className="empty-state compact-empty">Nu există participanți în această programare.</div>;
  }

  return (
    <div className="table-card inline-table-card">
      <table>
        <thead>
          <tr>
            <th>Client</th>
            <th>Status participare</th>
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {participants.map((participant) => (
            <tr key={participant.clientId}>
              <td>{participant.fullName}</td>
              <td><span className="status-pill">{participant.participationStatus}</span></td>
              <td>
                <button
                  className="small-button danger"
                  type="button"
                  disabled={!canRemove || actionLoading === `remove-participant-${participant.clientId}`}
                  onClick={() => onRemove(participant.clientId)}
                >
                  Elimină
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ClientSearchBox({ clientQuery, loadingClients, onClientQueryChange }) {
  return (
    <label className="client-search-label">
      Caută client activ
      <input
        type="search"
        value={clientQuery}
        onChange={(event) => onClientQueryChange(event.target.value)}
        placeholder="Caută după nume, email sau telefon"
      />
      {loadingClients && <small>Se caută clienții...</small>}
    </label>
  );
}

function SelectableClientList({ clients, selectedIds, onToggle, emptyText }) {
  if (!clients.length) {
    return <div className="empty-state compact-empty">{emptyText}</div>;
  }

  return (
    <div className="client-picker-list compact-client-list">
      {clients.map((client) => {
        const selected = selectedIds.includes(client.id);

        return (
          <button
            key={client.id}
            className={selected ? "client-picker-item selected" : "client-picker-item"}
            type="button"
            onClick={() => onToggle(client.id)}
          >
            <span className="client-picker-main">
              <strong>{client.fullName}</strong>
              <small>{client.email || "Fără email"} · {client.phone || "Fără telefon"}</small>
            </span>
            <span className="status-pill">{selected ? "Selectat" : "Selectează"}</span>
          </button>
        );
      })}
    </div>
  );
}

function WaitlistTable({ waitlist, actionLoading, isCancelled, onRemove, onPromote }) {
  if (!waitlist.length) {
    return <div className="empty-state compact-empty">Nu există clienți în lista de așteptare.</div>;
  }

  return (
    <div className="table-card inline-table-card waitlist-table">
      <table>
        <thead>
          <tr>
            <th>Poziție</th>
            <th>Client</th>
            <th>Status</th>
            <th>Creat la</th>
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {waitlist.map((entry) => (
            <tr key={entry.id}>
              <td>{entry.position}</td>
              <td>{entry.clientFullName}</td>
              <td><span className="status-pill">{entry.status}</span></td>
              <td>{formatDateTime(entry.createdAt)}</td>
              <td>
                <div className="row-actions">
                  <button
                    className="small-button"
                    type="button"
                    disabled={isCancelled || actionLoading === `promote-waitlist-${entry.id}`}
                    onClick={() => onPromote(entry.id)}
                  >
                    Promovează manual
                  </button>
                  <button
                    className="small-button danger"
                    type="button"
                    disabled={actionLoading === `remove-waitlist-${entry.id}`}
                    onClick={() => onRemove(entry.id)}
                  >
                    Elimină
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
