import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createAppointment, getAppointmentFormOptions } from "../../api/appointmentApi";
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

export default function CreateAppointmentPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY_FORM);
  const [options, setOptions] = useState({ services: [], instructors: [], resources: [] });
  const [clients, setClients] = useState([]);
  const [clientQuery, setClientQuery] = useState("");
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [loadingClients, setLoadingClients] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [validationErrors, setValidationErrors] = useState([]);

  const selectedService = useMemo(
    () => options.services.find((service) => service.id === form.serviceId),
    [options.services, form.serviceId]
  );

  useEffect(() => {
    loadOptions();
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadClients(clientQuery);
    }, 300);

    return () => window.clearTimeout(timer);
  }, [clientQuery]);

  const loadOptions = async () => {
    setLoadingOptions(true);
    setError("");

    try {
      const data = await getAppointmentFormOptions();
      setOptions({
        services: data.services || [],
        instructors: data.instructors || [],
        resources: data.resources || [],
      });
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca opțiunile pentru formular."));
    } finally {
      setLoadingOptions(false);
    }
  };

  const loadClients = async (query) => {
    setLoadingClients(true);

    try {
      const data = await searchClients({ query, active: true });
      setClients(Array.isArray(data) ? data : data.content || []);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca lista de clienți."));
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
        next.capacity = value === "INDIVIDUAL" ? 1 : Math.max(Number(prev.capacity) || 2, 2);
      }

      if (name === "serviceId") {
        const service = options.services.find((item) => item.id === value);
        next.endTime = calculateEndTime(next.startTime, service?.defaultDurationMinutes, prev.endTime);
      }

      if (name === "startTime") {
        next.endTime = calculateEndTime(value, selectedService?.defaultDurationMinutes, prev.endTime);
      }

      return next;
    });
  };

  const handleCapacityChange = (event) => {
    const capacity = Number(event.target.value);

    setForm((prev) => ({
      ...prev,
      capacity: Number.isNaN(capacity) ? "" : capacity,
    }));
  };

  const toggleClient = (clientId) => {
    setForm((prev) => {
      if (prev.appointmentType === "INDIVIDUAL") {
        return {
          ...prev,
          participantClientIds: [clientId],
          capacity: 1,
        };
      }

      const exists = prev.participantClientIds.includes(clientId);
      const participantClientIds = exists
        ? prev.participantClientIds.filter((id) => id !== clientId)
        : [...prev.participantClientIds, clientId];

      return {
        ...prev,
        participantClientIds,
        capacity: Math.max(Number(prev.capacity) || 2, participantClientIds.length, 2),
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
      await createAppointment(toPayload(form));
      navigate("/admin/programari", {
        replace: true,
        state: { success: "Programarea a fost salvată." },
      });
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut salva programarea."));
    } finally {
      setSaving(false);
    }
  };

  if (loadingOptions) {
    return <div className="page-loading">Se încarcă formularul de programare...</div>;
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Administrare programări</p>
          <h1>Programare nouă</h1>
          <p>Completează datele programării. Backend-ul verifică disponibilitatea finală.</p>
        </div>
        <Link className="secondary-link-button" to="/admin/programari">
          Înapoi la listă
        </Link>
      </div>

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
              onClick={() => handleSyntheticChange("appointmentType", "INDIVIDUAL", setForm)}
            >
              Individual
            </button>
            <button
              type="button"
              className={form.appointmentType === "GROUP" ? "active" : ""}
              onClick={() => handleSyntheticChange("appointmentType", "GROUP", setForm)}
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
            />
          </label>

          <ClientPicker
            clients={clients}
            loading={loadingClients}
            selectedIds={form.participantClientIds}
            appointmentType={form.appointmentType}
            onToggle={toggleClient}
          />
        </div>

        <div className="form-section">
          <h2>Serviciu și echipă</h2>
          <div className="form-grid">
            <label>
              Serviciu
              <select name="serviceId" value={form.serviceId} onChange={handleChange} required>
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
              <select name="instructorId" value={form.instructorId} onChange={handleChange} required>
                <option value="">Selectează instructorul</option>
                {options.instructors.map((instructor) => (
                  <option key={instructor.id} value={instructor.id}>{instructor.fullName}</option>
                ))}
              </select>
            </label>

            <label>
              Resursă
              <select name="resourceId" value={form.resourceId} onChange={handleChange}>
                <option value="">Fără resursă</option>
                {options.resources.map((resource) => (
                  <option key={resource.id} value={resource.id}>
                    {resource.name} · {resource.type}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Status
              <select name="status" value={form.status} onChange={handleChange} required>
                <option value="SCHEDULED">SCHEDULED</option>
                <option value="CONFIRMED">CONFIRMED</option>
              </select>
            </label>
          </div>
        </div>

        <div className="form-section">
          <h2>Data și ora</h2>
          <div className="form-grid">
            <label>
              Data
              <input type="date" name="date" value={form.date} onChange={handleChange} required />
            </label>

            <label>
              Ora început
              <input type="time" name="startTime" value={form.startTime} onChange={handleChange} required />
            </label>

            <label>
              Ora sfârșit
              <input type="time" name="endTime" value={form.endTime} onChange={handleChange} required />
            </label>

            <label>
              Capacitate
              <input
                type="number"
                min={form.appointmentType === "INDIVIDUAL" ? 1 : 2}
                name="capacity"
                value={form.capacity}
                onChange={handleCapacityChange}
                disabled={form.appointmentType === "INDIVIDUAL"}
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
            />
          </label>
        </div>

        <div className="form-summary-card">
          <strong>Rezumat</strong>
          <p>{form.appointmentType === "INDIVIDUAL" ? "Programare individuală" : "Programare de grup"}</p>
          <p>Participanți selectați: {form.participantClientIds.length}</p>
          <p>Capacitate: {form.capacity || "-"}</p>
        </div>

        <div className="modal-actions">
          <Link className="secondary-link-button" to="/admin/programari">Renunță</Link>
          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? "Se salvează..." : "Salvează programarea"}
          </button>
        </div>
      </form>
    </section>
  );
}

function ClientPicker({ clients, loading, selectedIds, appointmentType, onToggle }) {
  if (loading) {
    return <div className="page-loading compact-loading">Se caută clienții...</div>;
  }

  if (!clients.length) {
    return <div className="empty-state compact-empty">Nu am găsit clienți activi pentru căutarea curentă.</div>;
  }

  return (
    <div className="client-picker-list">
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
            <span className="status-pill">
              {selected ? "Selectat" : appointmentType === "INDIVIDUAL" ? "Alege" : "Adaugă"}
            </span>
          </button>
        );
      })}
    </div>
  );
}

function handleSyntheticChange(name, value, setForm) {
  setForm((prev) => ({
    ...prev,
    [name]: value,
    participantClientIds: [],
    capacity: value === "INDIVIDUAL" ? 1 : Math.max(Number(prev.capacity) || 2, 2),
  }));
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

  if (form.appointmentType === "INDIVIDUAL" && form.participantClientIds.length !== 1) {
    errors.push("Programarea individuală poate avea un singur client.");
  }

  if (form.appointmentType === "GROUP") {
    if (Number(form.capacity) < 2) {
      errors.push("Programarea de grup trebuie să aibă o capacitate de minimum 2.");
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
