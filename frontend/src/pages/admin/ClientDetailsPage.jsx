import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  createClientUserAccount,
  getClientAppointments,
  getClientDetails,
  updateClient,
  updateClientStatus,
} from "../../api/clientApi";
import { getApiErrorMessage } from "../../utils/apiErrors";
import { formatAppointmentInterval, formatDate, formatDateTime } from "../../utils/dateTime";

const EMPTY_TIMELINE = {
  clientId: null,
  clientFullName: "",
  futureAppointments: [],
  historyAppointments: [],
};

const GENDER_OPTIONS = [
  { value: "", label: "Nespecificat" },
  { value: "FEMALE", label: "Feminin" },
  { value: "MALE", label: "Masculin" },
  { value: "OTHER", label: "Altul" },
  { value: "PREFER_NOT_TO_SAY", label: "Prefer să nu spun" },
];

export default function ClientDetailsPage() {
  const { id } = useParams();
  const [client, setClient] = useState(null);
  const [timeline, setTimeline] = useState(EMPTY_TIMELINE);
  const [form, setForm] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadClient = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [details, appointments] = await Promise.all([
        getClientDetails(id),
        getClientAppointments(id),
      ]);

      setClient(details);
      setForm(toClientForm(details));
      setTimeline(appointments || EMPTY_TIMELINE);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca detaliile clientului."));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadClient();
  }, [loadClient]);

  const fullName = useMemo(() => {
    if (!client) {
      return "Client";
    }

    return `${client.firstName || ""} ${client.lastName || ""}`.trim() || "Client";
  }, [client]);

  const handleFormChange = (event) => {
    const { name, value, type, checked } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleUpdate = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");

    try {
      const updated = await updateClient(id, toUpdateClientPayload(form));
      setClient(updated);
      setForm(toClientForm(updated));
      setEditMode(false);
      setSuccess("Datele clientului au fost actualizate.");
      await refreshTimelineOnly(id, setTimeline);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut actualiza clientul."));
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async () => {
    if (!client) {
      return;
    }

    setSaving(true);
    setError("");
    setSuccess("");

    try {
      await updateClientStatus(client.id, !client.active);
      setSuccess(client.active ? "Clientul a fost dezactivat." : "Clientul a fost reactivat.");
      await loadClient();
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut actualiza statusul clientului."));
    } finally {
      setSaving(false);
    }
  };

  const handleAccountCreated = async () => {
    setShowAccountModal(false);
    setSuccess("Contul de utilizator a fost creat.");
    await loadClient();
  };

  if (loading) {
    return <div className="page-loading">Se încarcă detaliile clientului...</div>;
  }

  if (error && !client) {
    return (
      <section>
        <div className="page-header">
          <div>
            <p className="page-kicker">Clienți</p>
            <h1>Detalii client</h1>
          </div>
          <Link className="secondary-link-button" to="/admin/clienti">Înapoi la clienți</Link>
        </div>
        <div className="form-error">{error}</div>
      </section>
    );
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Fișă client</p>
          <h1>{fullName}</h1>
          <p>Detalii generale, informații sensibile, cont de acces și programări.</p>
        </div>

        <div className="header-actions">
          <Link className="secondary-link-button" to="/admin/clienti">Înapoi</Link>
          <Link className="secondary-link-button" to="/admin/programari/noua">Creează programare</Link>
          {!editMode && (
            <button className="primary-button" type="button" onClick={() => setEditMode(true)}>
              Editează
            </button>
          )}
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}
      {success && <div className="form-success">{success}</div>}

      <div className="summary-grid two-columns client-details-summary">
        <div className="summary-card">
          <p>Status client</p>
          <strong>{client.active ? "Activ" : "Inactiv"}</strong>
          <button className="small-button" type="button" disabled={saving} onClick={handleStatusChange}>
            {client.active ? "Dezactivează" : "Reactivează"}
          </button>
        </div>

        <div className="summary-card">
          <p>Cont platformă</p>
          <strong>{client.hasUserAccount ? "Creat" : "Lipsește"}</strong>
          {client.hasUserAccount ? (
            <span className="muted-text">{client.accountEmail || client.email || "Email indisponibil"}</span>
          ) : (
            <button className="small-button" type="button" onClick={() => setShowAccountModal(true)}>
              Creează cont utilizator
            </button>
          )}
        </div>
      </div>

      {editMode ? (
        <ClientEditForm
          form={form}
          saving={saving}
          onChange={handleFormChange}
          onCancel={() => {
            setForm(toClientForm(client));
            setEditMode(false);
            setError("");
          }}
          onSubmit={handleUpdate}
        />
      ) : (
        <ClientReadOnlyDetails client={client} />
      )}

      <ClientAppointmentSection timeline={timeline} recentAppointments={client.recentAppointments || []} />

      {showAccountModal && (
        <CreateAccountModal
          client={client}
          onClose={() => setShowAccountModal(false)}
          onCreated={handleAccountCreated}
        />
      )}
    </section>
  );
}

function ClientReadOnlyDetails({ client }) {
  return (
    <>
      <div className="content-card">
        <h2>Date generale</h2>
        <div className="details-grid">
          <DetailItem label="Prenume" value={client.firstName} />
          <DetailItem label="Nume" value={client.lastName} />
          <DetailItem label="Email" value={client.email} />
          <DetailItem label="Telefon" value={client.phone} />
          <DetailItem label="Data nașterii" value={formatDate(client.dateOfBirth)} />
          <DetailItem label="Gen" value={translateGender(client.gender)} />
          <DetailItem label="Sursă lead" value={client.leadSource} />
          <DetailItem label="Oraș" value={client.city} />
          <DetailItem label="Adresă" value={formatAddress(client)} />
        </div>
      </div>

      <div className="content-card">
        <h2>Comunicare și GDPR</h2>
        <div className="details-grid">
          <DetailItem label="GDPR" value={client.gdprConsent ? "Acceptat" : "Neacceptat"} />
          <DetailItem label="Email permis" value={client.emailAllowed ? "Da" : "Nu"} />
          <DetailItem label="SMS permis" value={client.smsAllowed ? "Da" : "Nu"} />
          <DetailItem label="Marketing permis" value={client.marketingAllowed ? "Da" : "Nu"} />
        </div>
      </div>

      <div className="content-card sensitive-card">
        <div className="section-title-row">
          <div>
            <p className="page-kicker">Acces administrator</p>
            <h2>Date sensibile</h2>
          </div>
          <span className="status-pill">Vizibil doar pentru ADMIN</span>
        </div>
        <div className="details-grid">
          <DetailItem label="Contact urgență" value={client.emergencyContactName} />
          <DetailItem label="Telefon urgență" value={client.emergencyContactPhone} />
        </div>
        <div className="notes-grid">
          <NoteBlock label="Note medicale" value={client.medicalNotes} />
          <NoteBlock label="Restricții" value={client.restrictions} />
        </div>
      </div>

      <div className="content-card">
        <h2>Cont de acces</h2>
        <div className="details-grid">
          <DetailItem label="Are cont" value={client.hasUserAccount ? "Da" : "Nu"} />
          <DetailItem label="Email cont" value={client.accountEmail} />
          <DetailItem label="Rol" value={client.accountRole} />
          <DetailItem label="Schimbare parolă obligatorie" value={client.forcePasswordChange ? "Da" : "Nu"} />
        </div>
      </div>
    </>
  );
}

function ClientEditForm({ form, saving, onChange, onCancel, onSubmit }) {
  return (
    <form className="content-card appointment-form" onSubmit={onSubmit}>
      <div className="form-section">
        <h2>Date generale</h2>
        <div className="form-grid">
          <label>
            Prenume
            <input name="firstName" value={form.firstName} onChange={onChange} required />
          </label>
          <label>
            Nume
            <input name="lastName" value={form.lastName} onChange={onChange} required />
          </label>
          <label>
            Email
            <input type="email" name="email" value={form.email} onChange={onChange} />
          </label>
          <label>
            Telefon
            <input name="phone" value={form.phone} onChange={onChange} />
          </label>
          <label>
            Data nașterii
            <input type="date" name="dateOfBirth" value={form.dateOfBirth} onChange={onChange} />
          </label>
          <label>
            Gen
            <select name="gender" value={form.gender} onChange={onChange}>
              {GENDER_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <label>
            Sursă lead
            <input name="leadSource" value={form.leadSource} onChange={onChange} />
          </label>
        </div>
      </div>

      <div className="form-section">
        <h2>Adresă</h2>
        <div className="form-grid">
          <label>
            Adresă 1
            <input name="addressLine1" value={form.addressLine1} onChange={onChange} />
          </label>
          <label>
            Adresă 2
            <input name="addressLine2" value={form.addressLine2} onChange={onChange} />
          </label>
          <label>
            Oraș
            <input name="city" value={form.city} onChange={onChange} />
          </label>
          <label>
            Județ
            <input name="county" value={form.county} onChange={onChange} />
          </label>
          <label>
            Cod poștal
            <input name="postcode" value={form.postcode} onChange={onChange} />
          </label>
        </div>
      </div>

      <div className="form-section">
        <h2>Comunicare și GDPR</h2>
        <div className="checkbox-grid">
          <label className="checkbox-row">
            <input type="checkbox" name="gdprConsent" checked={form.gdprConsent} onChange={onChange} />
            Consimțământ GDPR
          </label>
          <label className="checkbox-row">
            <input type="checkbox" name="emailAllowed" checked={form.emailAllowed} onChange={onChange} />
            Email permis
          </label>
          <label className="checkbox-row">
            <input type="checkbox" name="smsAllowed" checked={form.smsAllowed} onChange={onChange} />
            SMS permis
          </label>
          <label className="checkbox-row">
            <input type="checkbox" name="marketingAllowed" checked={form.marketingAllowed} onChange={onChange} />
            Marketing permis
          </label>
        </div>
      </div>

      <div className="form-section sensitive-edit-section">
        <h2>Date sensibile</h2>
        <div className="form-grid">
          <label>
            Contact urgență
            <input name="emergencyContactName" value={form.emergencyContactName} onChange={onChange} />
          </label>
          <label>
            Telefon urgență
            <input name="emergencyContactPhone" value={form.emergencyContactPhone} onChange={onChange} />
          </label>
        </div>
        <label>
          Note medicale
          <textarea name="medicalNotes" value={form.medicalNotes} onChange={onChange} />
        </label>
        <label>
          Restricții
          <textarea name="restrictions" value={form.restrictions} onChange={onChange} />
        </label>
      </div>

      <div className="modal-actions">
        <button className="secondary-button" type="button" onClick={onCancel} disabled={saving}>Renunță</button>
        <button className="primary-button" type="submit" disabled={saving}>
          {saving ? "Se salvează..." : "Salvează modificările"}
        </button>
      </div>
    </form>
  );
}

function ClientAppointmentSection({ timeline, recentAppointments }) {
  return (
    <div className="content-card">
      <div className="section-title-row">
        <div>
          <p className="page-kicker">Programări</p>
          <h2>Istoric și programări viitoare</h2>
        </div>
      </div>

      {recentAppointments.length > 0 && (
        <div className="client-recent-block">
          <h3>Programări recente</h3>
          <AppointmentMiniTable appointments={recentAppointments} emptyMessage="Nu există programări recente." />
        </div>
      )}

      <div className="appointment-timeline-grid">
        <div>
          <h3>Viitoare</h3>
          <AppointmentMiniTable appointments={timeline.futureAppointments || []} emptyMessage="Nu există programări viitoare." />
        </div>
        <div>
          <h3>Istoric</h3>
          <AppointmentMiniTable appointments={timeline.historyAppointments || []} emptyMessage="Nu există programări în istoric." />
        </div>
      </div>
    </div>
  );
}

function AppointmentMiniTable({ appointments, emptyMessage }) {
  if (!appointments.length) {
    return <div className="compact-empty">{emptyMessage}</div>;
  }

  return (
    <div className="table-card inline-table-card">
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Interval</th>
            <th>Serviciu</th>
            <th>Instructor</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {appointments.map((appointment) => (
            <tr key={appointment.id}>
              <td>{formatDateTime(appointment.startAt)}</td>
              <td>{formatAppointmentInterval(appointment.startAt, appointment.endAt)}</td>
              <td>{appointment.serviceName || "-"}</td>
              <td>{appointment.instructorName || "-"}</td>
              <td><span className="status-pill">{translateAppointmentStatus(appointment.status)}</span></td>
              <td>
                <Link className="small-link-button" to={`/admin/programari/${appointment.id}`}>Detalii</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CreateAccountModal({ client, onClose, onCreated }) {
  const [form, setForm] = useState({
    email: client.accountEmail || client.email || "",
    initialPassword: "",
    forcePasswordChange: true,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");

    try {
      await createClientUserAccount(client.id, {
        email: form.email.trim(),
        initialPassword: form.initialPassword,
        forcePasswordChange: form.forcePasswordChange,
      });
      onCreated();
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut crea contul de utilizator."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-header">
          <div>
            <p className="page-kicker">Cont client</p>
            <h2>Creează cont de utilizator</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose}>×</button>
        </div>

        <form className="form-card modal-form" onSubmit={handleSubmit}>
          <div className="form-grid single-column-grid">
            <label>
              Email cont
              <input type="email" name="email" value={form.email} onChange={handleChange} required />
            </label>
            <label>
              Parolă inițială
              <input type="password" name="initialPassword" value={form.initialPassword} onChange={handleChange} required />
            </label>
            <label className="checkbox-row">
              <input type="checkbox" name="forcePasswordChange" checked={form.forcePasswordChange} onChange={handleChange} />
              Forțează schimbarea parolei la prima autentificare
            </label>
          </div>

          {error && <div className="form-error">{error}</div>}

          <div className="modal-actions">
            <button className="secondary-button" type="button" onClick={onClose} disabled={saving}>Renunță</button>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? "Se creează..." : "Creează contul"}
            </button>
          </div>
        </form>
      </div>
    </div>
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

function NoteBlock({ label, value }) {
  return (
    <div className="note-block">
      <span>{label}</span>
      <p>{value || "-"}</p>
    </div>
  );
}

async function refreshTimelineOnly(id, setTimeline) {
  try {
    const appointments = await getClientAppointments(id);
    setTimeline(appointments || EMPTY_TIMELINE);
  } catch {
    // The client update already succeeded. Timeline refresh errors are handled on the next full load.
  }
}

function toClientForm(client) {
  return {
    firstName: client.firstName || "",
    lastName: client.lastName || "",
    email: client.email || "",
    phone: client.phone || "",
    addressLine1: client.addressLine1 || "",
    addressLine2: client.addressLine2 || "",
    city: client.city || "",
    county: client.county || "",
    postcode: client.postcode || "",
    dateOfBirth: client.dateOfBirth || "",
    gender: client.gender || "",
    leadSource: client.leadSource || "",
    gdprConsent: Boolean(client.gdprConsent),
    emailAllowed: Boolean(client.emailAllowed),
    smsAllowed: Boolean(client.smsAllowed),
    marketingAllowed: Boolean(client.marketingAllowed),
    emergencyContactName: client.emergencyContactName || "",
    emergencyContactPhone: client.emergencyContactPhone || "",
    medicalNotes: client.medicalNotes || "",
    restrictions: client.restrictions || "",
  };
}

function toUpdateClientPayload(form) {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    email: emptyToNull(form.email.trim()),
    phone: emptyToNull(form.phone.trim()),
    addressLine1: emptyToNull(form.addressLine1.trim()),
    addressLine2: emptyToNull(form.addressLine2.trim()),
    city: emptyToNull(form.city.trim()),
    county: emptyToNull(form.county.trim()),
    postcode: emptyToNull(form.postcode.trim()),
    dateOfBirth: emptyToNull(form.dateOfBirth),
    gender: emptyToNull(form.gender),
    leadSource: emptyToNull(form.leadSource.trim()),
    gdprConsent: form.gdprConsent,
    emailAllowed: form.emailAllowed,
    smsAllowed: form.smsAllowed,
    marketingAllowed: form.marketingAllowed,
    emergencyContactName: emptyToNull(form.emergencyContactName.trim()),
    emergencyContactPhone: emptyToNull(form.emergencyContactPhone.trim()),
    medicalNotes: emptyToNull(form.medicalNotes.trim()),
    restrictions: emptyToNull(form.restrictions.trim()),
  };
}

function emptyToNull(value) {
  return value === "" ? null : value;
}

function formatAddress(client) {
  return [client.addressLine1, client.addressLine2, client.city, client.county, client.postcode]
    .filter(Boolean)
    .join(", ");
}

function translateGender(value) {
  const match = GENDER_OPTIONS.find((option) => option.value === value);
  return match?.label || value || "-";
}

function translateAppointmentStatus(value) {
  const labels = {
    SCHEDULED: "Programată",
    CONFIRMED: "Confirmată",
    COMPLETED: "Finalizată",
    CANCELLED: "Anulată",
    NO_SHOW: "Neprezentare",
  };

  return labels[value] || value || "-";
}
