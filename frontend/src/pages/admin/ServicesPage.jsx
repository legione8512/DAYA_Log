import { useEffect, useState } from "react";
import {
  createService,
  listServices,
  updateService,
  updateServiceStatus,
} from "../../api/catalogueApi";
import { getApiErrorMessage } from "../../utils/apiErrors";

const EMPTY_SERVICE_FORM = {
  name: "",
  description: "",
  defaultDurationMinutes: 60,
};

export default function ServicesPage() {
  const [query, setQuery] = useState("");
  const [active, setActive] = useState("");
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [editingService, setEditingService] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const loadServices = async (currentQuery = query, currentActive = active) => {
    setLoading(true);
    setError("");

    try {
      const data = await listServices(cleanParams({ query: currentQuery, active: currentActive }));
      setServices(data);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca serviciile."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadServices(query, active);
    }, 300);

    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, active]);

  const openCreateModal = () => {
    setEditingService(null);
    setShowModal(true);
  };

  const openEditModal = (service) => {
    setEditingService(service);
    setShowModal(true);
  };

  const handleSaved = async (wasEdit) => {
    setShowModal(false);
    setEditingService(null);
    setSuccess(wasEdit ? "Serviciul a fost actualizat." : "Serviciul a fost adăugat.");
    await loadServices(query, active);
  };

  const handleStatusChange = async (service) => {
    setError("");
    setSuccess("");

    try {
      await updateServiceStatus(service.id, !service.active);
      setSuccess(service.active ? "Serviciul a fost dezactivat." : "Serviciul a fost reactivat.");
      await loadServices(query, active);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut actualiza statusul serviciului."));
    }
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Catalog</p>
          <h1>Servicii</h1>
          <p>Administrează serviciile care pot fi folosite în programări.</p>
        </div>
        <button className="primary-button" type="button" onClick={openCreateModal}>
          Adaugă serviciu
        </button>
      </div>

      <div className="filter-card compact-filter-card">
        <label>
          Caută serviciu
          <input
            type="search"
            placeholder="Nume sau descriere"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>

        <label>
          Status
          <select value={active} onChange={(event) => setActive(event.target.value)}>
            <option value="">Toate</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </label>
      </div>

      {error && <div className="form-error">{error}</div>}
      {success && <div className="form-success">{success}</div>}

      {loading ? (
        <div className="page-loading">Se încarcă serviciile...</div>
      ) : (
        <ServicesTable
          services={services}
          onAddClick={openCreateModal}
          onEditClick={openEditModal}
          onStatusChange={handleStatusChange}
        />
      )}

      {showModal && (
        <ServiceModal
          service={editingService}
          onClose={() => setShowModal(false)}
          onSaved={handleSaved}
        />
      )}
    </section>
  );
}

function ServicesTable({ services, onAddClick, onEditClick, onStatusChange }) {
  if (!services.length) {
    return (
      <div className="empty-state">
        <p>Nu există servicii pentru filtrele selectate.</p>
        <button className="primary-button" type="button" onClick={onAddClick}>Adaugă serviciu</button>
      </div>
    );
  }

  return (
    <div className="table-card">
      <table>
        <thead>
          <tr>
            <th>Nume</th>
            <th>Descriere</th>
            <th>Durată</th>
            <th>Status</th>
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {services.map((service) => (
            <tr key={service.id}>
              <td><strong>{service.name}</strong></td>
              <td>{service.description || "-"}</td>
              <td>{service.defaultDurationMinutes || 60} min</td>
              <td><span className="status-pill">{service.active ? "Activ" : "Inactiv"}</span></td>
              <td>
                <div className="row-actions">
                  <button className="small-button" type="button" onClick={() => onEditClick(service)}>
                    Editează
                  </button>
                  <button className="small-button" type="button" onClick={() => onStatusChange(service)}>
                    {service.active ? "Dezactivează" : "Reactivează"}
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

function ServiceModal({ service, onClose, onSaved }) {
  const isEdit = Boolean(service);
  const [form, setForm] = useState(() => service ? toServiceForm(service) : EMPTY_SERVICE_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: name === "defaultDurationMinutes" ? Number(value) : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");

    try {
      const payload = {
        name: form.name.trim(),
        description: blankToNull(form.description),
        defaultDurationMinutes: Number(form.defaultDurationMinutes) || 60,
      };

      if (isEdit) {
        await updateService(service.id, payload);
      } else {
        await createService(payload);
      }

      await onSaved(isEdit);
    } catch (err) {
      setError(getApiErrorMessage(err, isEdit ? "Nu am putut actualiza serviciul." : "Nu am putut crea serviciul."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-header">
          <div>
            <p className="page-kicker">Serviciu</p>
            <h2>{isEdit ? "Editează serviciu" : "Adaugă serviciu"}</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose}>×</button>
        </div>

        <form className="form-card modal-form" onSubmit={handleSubmit}>
          {error && <div className="form-error">{error}</div>}

          <label>
            Nume serviciu
            <input name="name" value={form.name} onChange={handleChange} required maxLength={150} />
          </label>

          <label>
            Durată implicită, minute
            <input
              type="number"
              name="defaultDurationMinutes"
              min="15"
              max="240"
              step="15"
              value={form.defaultDurationMinutes}
              onChange={handleChange}
              required
            />
          </label>

          <label>
            Descriere
            <textarea name="description" value={form.description} onChange={handleChange} />
          </label>

          <div className="modal-actions">
            <button className="secondary-button" type="button" onClick={onClose}>Renunță</button>
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? "Se salvează..." : "Salvează"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function toServiceForm(service) {
  return {
    name: service.name || "",
    description: service.description || "",
    defaultDurationMinutes: service.defaultDurationMinutes || 60,
  };
}

function cleanParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== "" && value !== null && value !== undefined));
}

function blankToNull(value) {
  return value && value.trim() ? value.trim() : null;
}
