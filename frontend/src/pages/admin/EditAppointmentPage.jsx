import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  getAdminAppointment,
  getAppointmentFormOptions,
  updateAppointment,
} from "../../api/appointmentApi";
import { searchClients } from "../../api/clientApi";
import { getApiErrorMessage } from "../../utils/apiErrors";

const EMPTY_FORM = {
  appointmentType: "INDIVIDUAL",
  serviceId: "",
  instructorId: "",
  resourceId: "",
  date: "",
  startTime: "",
  endTime: "",
  status: "SCHEDULED",
  capacity: 1,
  notes: "",
  participantClientIds: [],
};

export default function EditAppointmentPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY_FORM);
  const [appointment, setAppointment] = useState(null);
  const [options, setOptions] = useState({
    services: [],
    instructors: [],
    resources: [],
  });
  const [clients, setClients] = useState([]);
  const [selectedClients, setSelectedClients] = useState([]);
  const [clientQuery, setClientQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadingClients, setLoadingClients] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [validationErrors, setValidationErrors] = useState([]);

  const selectedService = useMemo(
    () => options.services.find((service) => service.id === form.serviceId),
    [options.services, form.serviceId],
  );

  const clientsToDisplay = useMemo(() => {
    const byId = new Map();

    selectedClients.forEach((client) => byId.set(client.id, client));
    clients.forEach((client) => byId.set(client.id, client));

    return Array.from(byId.values());
  }, [clients, selectedClients]);

  useEffect(() => {
    loadInitialData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadClients(clientQuery);
    }, 300);

    return () => window.clearTimeout(timer);
  }, [clientQuery]);

  const loadInitialData = async () => {
    setLoading(true);
    setError("");

    try {
      const [appointmentData, optionsData] = await Promise.all([
        getAdminAppointment(id),
        getAppointmentFormOptions(),
      ]);

      setAppointment(appointmentData);
      setOptions({
        services: optionsData.services || [],
        instructors: optionsData.instructors || [],
        resources: optionsData.resources || [],
      });
      setForm(fromAppointmentToForm(appointmentData));
      setSelectedClients(
        (appointmentData.participants || []).map(toClientPickerItem),
      );
      await loadClients("");
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          "Nu am putut încărca programarea pentru editare.",
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  const loadClients = async (query) => {
    setLoadingClients(true);

    try {
      const data = await searchClients({ query, active: true });
      setClients(Array.isArray(data) ? data : data.content || []);
    } catch (err) {
      setError(
        getApiErrorMessage(err, "Nu am putut încărca lista de clienți."),
      );
    } finally {
      setLoadingClients(false);
    }
  };

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((prev) => {
      const next = {
        ...prev,
        [name]: value,
      };

      if (name === "appointmentType") {
        next.participantClientIds = [];
        next.capacity =
          value === "INDIVIDUAL" ? 1 : Math.max(Number(prev.capacity) || 2, 2);
        setSelectedClients([]);
      }

      if (name === "serviceId") {
        const service = options.services.find((item) => item.id === value);
        next.endTime = calculateEndTime(
          next.startTime,
          service?.defaultDurationMinutes,
          prev.endTime,
        );
      }

      if (name === "startTime") {
        next.endTime = calculateEndTime(
          value,
          selectedService?.defaultDurationMinutes,
          prev.endTime,
        );
      }

      return next;
    });
  };

  const handleAppointmentTypeChange = (appointmentType) => {
    setSelectedClients([]);
    setForm((prev) => ({
      ...prev,
      appointmentType,
      participantClientIds: [],
      capacity:
        appointmentType === "INDIVIDUAL"
          ? 1
          : Math.max(Number(prev.capacity) || 2, 2),
    }));
  };

  const handleCapacityChange = (event) => {
    const capacity = Number(event.target.value);

    setForm((prev) => ({
      ...prev,
      capacity: Number.isNaN(capacity) ? "" : capacity,
    }));
  };

  const toggleClient = (client) => {
    setForm((prev) => {
      if (prev.appointmentType === "INDIVIDUAL") {
        setSelectedClients([client]);

        return {
          ...prev,
          participantClientIds: [client.id],
          capacity: 1,
        };
      }

      const exists = prev.participantClientIds.includes(client.id);
      const participantClientIds = exists
        ? prev.participantClientIds.filter((clientId) => clientId !== client.id)
        : [...prev.participantClientIds, client.id];

      setSelectedClients((current) => {
        if (exists) {
          return current.filter((item) => item.id !== client.id);
        }

        if (current.some((item) => item.id === client.id)) {
          return current;
        }

        return [...current, client];
      });

      return {
        ...prev,
        participantClientIds,
        capacity: Math.max(
          Number(prev.capacity) || 2,
          participantClientIds.length,
          2,
        ),
      };
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setValidationErrors([]);

    const errors = validateForm(form);

    if (errors.length > 0) {
      setValidationErrors(errors);
      return;
    }

    setSaving(true);

    try {
      await updateAppointment(id, toPayload(form));
      navigate(`/admin/programari/${id}`, {
        replace: true,
        state: { success: "Programarea a fost actualizată." },
      });
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut actualiza programarea."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="page-loading">Se încarcă formularul de editare...</div>
    );
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

  const isCancelled = appointment?.status === "CANCELLED";

  const cancelledEditMessage =
    "Programarea este anulată. Statusul este final și această programare nu mai poate fi editată. Dacă vrei să refaci rezervarea, creează o programare nouă.";
  const statusInfoMessage =
    "Pentru schimbarea statusului folosește pagina de detalii. Endpointul de update păstrează statusul existent.";

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Administrare programări</p>
          <h1>Editează programarea</h1>
          <p>
            Modifică datele programării. Statusul se schimbă separat din pagina
            de detalii.
          </p>
        </div>
        <div className="header-actions">
          <Link
            className="secondary-link-button"
            to={`/admin/programari/${id}`}
          >
            Înapoi la detalii
          </Link>
          <Link className="secondary-link-button" to="/admin/programari">
            Înapoi la listă
          </Link>
        </div>
      </div>

      {isCancelled && (
        <div className="info-banner">
          Programarea este anulată. Backend-ul nu permite modificarea
          programărilor anulate.
        </div>
      )}

      {isCancelled ? (
        <div className="warning-banner">{}</div>
      ) : (
        <div className="info-banner">{statusInfoMessage}</div>
      )}

      {error && <div className="form-error">{error}</div>}
      {validationErrors.length > 0 && (
        <div className="form-error">
          {validationErrors.map((item) => (
            <p key={item}>{item}</p>
          ))}
        </div>
      )}

      <form className="content-card appointment-form" onSubmit={handleSubmit}>
        <div className="form-section">
          <h2>Tip programare</h2>
          <div className="segmented-control">
            <button
              type="button"
              className={form.appointmentType === "INDIVIDUAL" ? "active" : ""}
              disabled={isCancelled}
              onClick={() => handleAppointmentTypeChange("INDIVIDUAL")}
            >
              Individual
            </button>
            <button
              type="button"
              className={form.appointmentType === "GROUP" ? "active" : ""}
              disabled={isCancelled}
              onClick={() => handleAppointmentTypeChange("GROUP")}
            >
              Grup
            </button>
          </div>
        </div>

        <div className="form-section">
          <h2>Client / participanți</h2>
          <label>
            Caută client activ
            <input
              type="search"
              value={clientQuery}
              onChange={(event) => setClientQuery(event.target.value)}
              placeholder="Caută după nume, email sau telefon"
              disabled={isCancelled}
            />
          </label>

          <ClientPicker
            clients={clientsToDisplay}
            loading={loadingClients}
            selectedIds={form.participantClientIds}
            appointmentType={form.appointmentType}
            disabled={isCancelled}
            onToggle={toggleClient}
          />
        </div>

        <div className="form-section">
          <h2>Serviciu și echipă</h2>
          <div className="form-grid">
            <label>
              Serviciu
              <select
                name="serviceId"
                value={form.serviceId}
                onChange={handleChange}
                disabled={isCancelled}
                required
              >
                <option value="">Selectează serviciul</option>
                {options.services.map((service) => (
                  <option key={service.id} value={service.id}>
                    {service.name} · {service.defaultDurationMinutes} min
                  </option>
                ))}
              </select>
            </label>

            <label>
              Instructor
              <select
                name="instructorId"
                value={form.instructorId}
                onChange={handleChange}
                disabled={isCancelled}
                required
              >
                <option value="">Selectează instructorul</option>
                {options.instructors.map((instructor) => (
                  <option key={instructor.id} value={instructor.id}>
                    {instructor.fullName}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Resursă
              <select
                name="resourceId"
                value={form.resourceId}
                onChange={handleChange}
                disabled={isCancelled}
              >
                <option value="">Fără resursă</option>
                {options.resources.map((resource) => (
                  <option key={resource.id} value={resource.id}>
                    {resource.name} · {resource.type}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Status curent
              <input value={form.status} disabled readOnly />
            </label>
          </div>
        </div>

        <div className="form-section">
          <h2>Data și ora</h2>
          <div className="form-grid">
            <label>
              Data
              <input
                type="date"
                name="date"
                value={form.date}
                onChange={handleChange}
                disabled={isCancelled}
                required
              />
            </label>

            <label>
              Ora început
              <input
                type="time"
                name="startTime"
                value={form.startTime}
                onChange={handleChange}
                disabled={isCancelled}
                required
              />
            </label>

            <label>
              Ora sfârșit
              <input
                type="time"
                name="endTime"
                value={form.endTime}
                onChange={handleChange}
                disabled={isCancelled}
                required
              />
            </label>

            <label>
              Capacitate
              <input
                type="number"
                min={form.appointmentType === "INDIVIDUAL" ? 1 : 2}
                name="capacity"
                value={form.capacity}
                onChange={handleCapacityChange}
                disabled={isCancelled || form.appointmentType === "INDIVIDUAL"}
                required
              />
            </label>
          </div>
        </div>

        <div className="form-section">
          <h2>Notițe</h2>
          <label>
            Notițe interne
            <textarea
              name="notes"
              value={form.notes}
              onChange={handleChange}
              placeholder="Observații pentru programare"
              disabled={isCancelled}
            />
          </label>
        </div>

        <div className="form-summary-card">
          <strong>Rezumat</strong>
          <p>
            {form.appointmentType === "INDIVIDUAL"
              ? "Programare individuală"
              : "Programare de grup"}
          </p>
          <p>Participanți selectați: {form.participantClientIds.length}</p>
          <p>Capacitate: {form.capacity || "-"}</p>
          <p>Status păstrat: {form.status}</p>
        </div>

        <div className="modal-actions">
          <Link
            className="secondary-link-button"
            to={`/admin/programari/${id}`}
          >
            Renunță
          </Link>
          <button
            className="primary-button"
            type="submit"
            disabled={saving || isCancelled}
          >
            {saving ? "Se salvează..." : "Salvează modificările"}
          </button>
        </div>
      </form>
    </section>
  );
}

function ClientPicker({
  clients,
  loading,
  selectedIds,
  appointmentType,
  disabled,
  onToggle,
}) {
  if (loading) {
    return (
      <div className="page-loading compact-loading">Se caută clienții...</div>
    );
  }

  if (!clients.length) {
    return (
      <div className="empty-state compact-empty">
        Nu am găsit clienți activi pentru căutarea curentă.
      </div>
    );
  }

  return (
    <div className="client-picker-list">
      {clients.map((client) => {
        const selected = selectedIds.includes(client.id);

        return (
          <button
            key={client.id}
            className={
              selected ? "client-picker-item selected" : "client-picker-item"
            }
            type="button"
            disabled={disabled}
            onClick={() => onToggle(client)}
          >
            <span className="client-picker-main">
              <strong>{client.fullName}</strong>
              <small>
                {client.email || "Fără email"} ·{" "}
                {client.phone || "Fără telefon"}
              </small>
            </span>
            <span className="status-pill">
              {selected
                ? "Selectat"
                : appointmentType === "INDIVIDUAL"
                  ? "Alege"
                  : "Adaugă"}
            </span>
          </button>
        );
      })}
    </div>
  );
}

function fromAppointmentToForm(appointment) {
  const startParts = toLocalDateAndTime(appointment.startAt);
  const endParts = toLocalDateAndTime(appointment.endAt);

  return {
    appointmentType: appointment.appointmentType || "INDIVIDUAL",
    serviceId: appointment.serviceId || "",
    instructorId: appointment.instructorId || "",
    resourceId: appointment.resourceId || "",
    date: startParts.date,
    startTime: startParts.time,
    endTime: endParts.time,
    status: appointment.status || "SCHEDULED",
    capacity: appointment.capacity || 1,
    notes: appointment.notes || "",
    participantClientIds: (appointment.participants || []).map(
      (participant) => participant.clientId,
    ),
  };
}

function toClientPickerItem(participant) {
  return {
    id: participant.clientId,
    fullName: participant.fullName,
    email: "Participant existent",
    phone: participant.participationStatus,
  };
}

function calculateEndTime(startTime, durationMinutes, currentEndTime) {
  if (!startTime || !durationMinutes) {
    return currentEndTime;
  }

  const [hours, minutes] = startTime.split(":").map(Number);
  const startDate = new Date(2000, 0, 1, hours, minutes);
  startDate.setMinutes(startDate.getMinutes() + Number(durationMinutes));

  return `${String(startDate.getHours()).padStart(2, "0")}:${String(startDate.getMinutes()).padStart(2, "0")}`;
}

function validateForm(form) {
  const errors = [];

  if (!form.serviceId) {
    errors.push("Selectează serviciul.");
  }

  if (!form.instructorId) {
    errors.push("Selectează instructorul.");
  }

  if (!form.date || !form.startTime || !form.endTime) {
    errors.push("Completează data, ora de început și ora de sfârșit.");
  }

  if (form.date && form.startTime && form.endTime) {
    const startAt = toDate(form.date, form.startTime);
    const endAt = toDate(form.date, form.endTime);

    if (endAt <= startAt) {
      errors.push("Ora de sfârșit trebuie să fie după ora de început.");
    }
  }

  if (!form.participantClientIds.length) {
    errors.push("Selectează cel puțin un client.");
  }

  if (
    form.appointmentType === "INDIVIDUAL" &&
    form.participantClientIds.length !== 1
  ) {
    errors.push("Programarea individuală poate avea un singur client.");
  }

  if (form.appointmentType === "GROUP") {
    if (Number(form.capacity) < 2) {
      errors.push(
        "Programarea de grup trebuie să aibă o capacitate de minimum 2.",
      );
    }

    if (Number(form.capacity) < form.participantClientIds.length) {
      errors.push("Capacitatea este prea mică pentru numărul de participanți.");
    }
  }

  return errors;
}

function toPayload(form) {
  return {
    appointmentType: form.appointmentType,
    serviceId: form.serviceId,
    instructorId: form.instructorId,
    resourceId: form.resourceId || null,
    startAt: toIsoOffsetDateTime(form.date, form.startTime),
    endAt: toIsoOffsetDateTime(form.date, form.endTime),
    status: form.status,
    notes: form.notes.trim() || null,
    participantClientIds: form.participantClientIds,
    capacity: Number(form.capacity),
  };
}

function toDate(date, time) {
  return new Date(`${date}T${time}:00`);
}

function toIsoOffsetDateTime(date, time) {
  return toDate(date, time).toISOString();
}

function toLocalDateAndTime(value) {
  if (!value) {
    return { date: "", time: "" };
  }

  const date = new Date(value);

  return {
    date: [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, "0"),
      String(date.getDate()).padStart(2, "0"),
    ].join("-"),
    time: [
      String(date.getHours()).padStart(2, "0"),
      String(date.getMinutes()).padStart(2, "0"),
    ].join(":"),
  };
}
