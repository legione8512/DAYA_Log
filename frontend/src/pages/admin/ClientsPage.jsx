import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { createClient, searchClients, updateClientStatus } from "../../api/clientApi";
import { getApiErrorMessage } from "../../utils/apiErrors";

const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  city: "",
  dateOfBirth: "",
  gender: "",
  leadSource: "",
  gdprConsent: false,
  emailAllowed: true,
  smsAllowed: false,
  marketingAllowed: false,
  emergencyContactName: "",
  emergencyContactPhone: "",
  medicalNotes: "",
  restrictions: "",
  createUserAccount: false,
  initialPassword: "",
  forcePasswordChange: true,
};

export default function ClientsPage() {
  const [query, setQuery] = useState("");
  const [active, setActive] = useState("");
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showModal, setShowModal] = useState(false);

  const loadClients = async (currentQuery = query, currentActive = active) => {
    setLoading(true);
    setError("");

    try {
      const data = await searchClients(cleanParams({ query: currentQuery, active: currentActive }));
      setClients(data);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca lista de clienți."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadClients(query, active);
    }, 300);

    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, active]);

  const handleStatusChange = async (client) => {
    setError("");
    setSuccess("");

    try {
      await updateClientStatus(client.id, !client.active);
      setSuccess(client.active ? "Clientul a fost dezactivat." : "Clientul a fost reactivat.");
      await loadClients(query, active);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut actualiza statusul clientului."));
    }
  };

  const handleClientCreated = async () => {
    setShowModal(false);
    setSuccess("Clientul a fost adăugat cu succes.");
    await loadClients(query, active);
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Administrare</p>
          <h1>Clienți</h1>
          <p>Caută rapid clienți și adaugă fișe noi pentru programări.</p>
        </div>
        <button className="primary-button" type="button" onClick={() => setShowModal(true)}>
          Adaugă client
        </button>
      </div>

      <div className="filter-card compact-filter-card">
        <label>
          Caută client
          <input
            type="search"
            placeholder="Nume, email sau telefon"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>

        <label>
          Status
          <select value={active} onChange={(event) => setActive(event.target.value)}>
            <option value="">Toți</option>
            <option value="true">Activi</option>
            <option value="false">Inactivi</option>
          </select>
        </label>
      </div>

      {error && <div className="form-error">{error}</div>}
      {success && <div className="form-success">{success}</div>}

      {loading ? (
        <div className="page-loading">Se caută clienții...</div>
      ) : (
        <ClientsTable clients={clients} onStatusChange={handleStatusChange} onAddClick={() => setShowModal(true)} />
      )}

      {showModal && (
        <AddClientModal
          onClose={() => setShowModal(false)}
          onCreated={handleClientCreated}
        />
      )}
    </section>
  );
}

function ClientsTable({ clients, onStatusChange, onAddClick }) {
  if (!clients.length) {
    return (
      <div className="empty-state">
        <p>Nu am găsit niciun client.</p>
        <button className="primary-button" type="button" onClick={onAddClick}>Adaugă client</button>
      </div>
    );
  }

  return (
    <div className="table-card">
      <table>
        <thead>
          <tr>
            <th>Nume</th>
            <th>Email</th>
            <th>Telefon</th>
            <th>Status</th>
            <th>Cont</th>
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {clients.map((client) => (
            <tr key={client.id}>
              <td>{client.fullName}</td>
              <td>{client.email || "-"}</td>
              <td>{client.phone || "-"}</td>
              <td><span className="status-pill">{client.active ? "Activ" : "Inactiv"}</span></td>
              <td>{client.hasUserAccount ? "Cont creat" : "Fără cont"}</td>
              <td>
                <div className="row-actions">
                  <Link className="small-link-button" to={`/admin/clienti/${client.id}`}>Detalii</Link>
                  <button className="small-button" type="button" onClick={() => onStatusChange(client)}>
                    {client.active ? "Dezactivează" : "Reactivează"}
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

function AddClientModal({ onClose, onCreated }) {
  const [form, setForm] = useState(EMPTY_FORM);
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
      await createClient(toCreateClientPayload(form));
      onCreated();
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut crea clientul."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card wide-modal">
        <div className="modal-header">
          <div>
            <p className="page-kicker">Client nou</p>
            <h2>Adaugă client nou</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose}>×</button>
        </div>

        <form className="form-card modal-form" onSubmit={handleSubmit}>
          <div className="form-grid">
            <label>
              Prenume
              <input name="firstName" value={form.firstName} onChange={handleChange} required />
            </label>

            <label>
              Nume
              <input name="lastName" value={form.lastName} onChange={handleChange} required />
            </label>

            <label>
              Email
              <input type="email" name="email" value={form.email} onChange={handleChange} />
            </label>

            <label>
              Telefon
              <input name="phone" value={form.phone} onChange={handleChange} />
            </label>

            <label>
              Oraș
              <input name="city" value={form.city} onChange={handleChange} />
            </label>

            <label>
              Data nașterii
              <input type="date" name="dateOfBirth" value={form.dateOfBirth} onChange={handleChange} />
            </label>

            <label>
              Gen
              <select name="gender" value={form.gender} onChange={handleChange}>
                <option value="">Nespecificat</option>
                <option value="FEMALE">FEMALE</option>
                <option value="MALE">MALE</option>
                <option value="OTHER">OTHER</option>
                <option value="PREFER_NOT_TO_SAY">PREFER_NOT_TO_SAY</option>
              </select>
            </label>

            <label>
              Sursă lead
              <input name="leadSource" value={form.leadSource} onChange={handleChange} />
            </label>
          </div>

          <div className="checkbox-grid">
            <label className="checkbox-row">
              <input type="checkbox" name="gdprConsent" checked={form.gdprConsent} onChange={handleChange} />
              Consimțământ GDPR
            </label>
            <label className="checkbox-row">
              <input type="checkbox" name="emailAllowed" checked={form.emailAllowed} onChange={handleChange} />
              Email permis
            </label>
            <label className="checkbox-row">
              <input type="checkbox" name="smsAllowed" checked={form.smsAllowed} onChange={handleChange} />
              SMS permis
            </label>
            <label className="checkbox-row">
              <input type="checkbox" name="marketingAllowed" checked={form.marketingAllowed} onChange={handleChange} />
              Marketing permis
            </label>
          </div>

          <div className="form-grid">
            <label>
              Contact urgență
              <input name="emergencyContactName" value={form.emergencyContactName} onChange={handleChange} />
            </label>

            <label>
              Telefon urgență
              <input name="emergencyContactPhone" value={form.emergencyContactPhone} onChange={handleChange} />
            </label>
          </div>

          <label>
            Note medicale
            <textarea name="medicalNotes" value={form.medicalNotes} onChange={handleChange} />
          </label>

          <label>
            Restricții
            <textarea name="restrictions" value={form.restrictions} onChange={handleChange} />
          </label>

          <div className="account-section">
            <label className="checkbox-row">
              <input type="checkbox" name="createUserAccount" checked={form.createUserAccount} onChange={handleChange} />
              Creează cont de utilizator acum
            </label>

            {form.createUserAccount && (
              <div className="form-grid">
                <label>
                  Parolă inițială
                  <input type="password" name="initialPassword" value={form.initialPassword} onChange={handleChange} />
                </label>
                <label className="checkbox-row force-password-row">
                  <input type="checkbox" name="forcePasswordChange" checked={form.forcePasswordChange} onChange={handleChange} />
                  Forțează schimbarea parolei
                </label>
              </div>
            )}
          </div>

          {error && <div className="form-error">{error}</div>}

          <div className="modal-actions">
            <button className="secondary-button" type="button" onClick={onClose}>Renunță</button>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? "Se salvează..." : "Salvează clientul"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== "" && value !== null && value !== undefined)
  );
}

function emptyToNull(value) {
  return value === "" ? null : value;
}

function toCreateClientPayload(form) {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    email: emptyToNull(form.email.trim()),
    phone: emptyToNull(form.phone.trim()),
    addressLine1: null,
    addressLine2: null,
    city: emptyToNull(form.city.trim()),
    county: null,
    postcode: null,
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
    createUserAccount: form.createUserAccount,
    initialPassword: form.createUserAccount ? form.initialPassword : null,
    forcePasswordChange: form.createUserAccount ? form.forcePasswordChange : true,
  };
}
