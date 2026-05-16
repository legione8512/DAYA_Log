import { useEffect, useState } from "react";
import {
  createResource,
  listResources,
  updateResource,
  updateResourceStatus,
} from "../../api/catalogueApi";
import { getApiErrorMessage } from "../../utils/apiErrors";

const EMPTY_RESOURCE_FORM = {
  name: "",
  type: "REFORMER",
  notes: "",
};

const RESOURCE_TYPES = [
  { value: "ROOM", label: "ROOM" },
  { value: "STUDIO_SPACE", label: "STUDIO_SPACE" },
  { value: "REFORMER", label: "REFORMER" },
  { value: "MAT", label: "MAT" },
  { value: "OTHER", label: "OTHER" },
];

export default function ResourcesPage() {
  const [query, setQuery] = useState("");
  const [active, setActive] = useState("");
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [editingResource, setEditingResource] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const loadResources = async (currentQuery = query, currentActive = active) => {
    setLoading(true);
    setError("");

    try {
      const data = await listResources(cleanParams({ query: currentQuery, active: currentActive }));
      setResources(data);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca resursele."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadResources(query, active);
    }, 300);

    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, active]);

  const openCreateModal = () => {
    setEditingResource(null);
    setShowModal(true);
  };

  const openEditModal = (resource) => {
    setEditingResource(resource);
    setShowModal(true);
  };

  const handleSaved = async (wasEdit) => {
    setShowModal(false);
    setEditingResource(null);
    setSuccess(wasEdit ? "Resursa a fost actualizată." : "Resursa a fost adăugată.");
    await loadResources(query, active);
  };

  const handleStatusChange = async (resource) => {
    setError("");
    setSuccess("");

    try {
      await updateResourceStatus(resource.id, !resource.active);
      setSuccess(resource.active ? "Resursa a fost dezactivată." : "Resursa a fost reactivată.");
      await loadResources(query, active);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut actualiza statusul resursei."));
    }
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Catalog</p>
          <h1>Resurse</h1>
          <p>Administrează camerele, reformerele și alte resurse folosite în programări.</p>
        </div>
        <button className="primary-button" type="button" onClick={openCreateModal}>
          Adaugă resursă
        </button>
      </div>

      <div className="filter-card compact-filter-card">
        <label>
          Caută resursă
          <input
            type="search"
            placeholder="Nume, tip sau notițe"
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
        <div className="page-loading">Se încarcă resursele...</div>
      ) : (
        <ResourcesTable
          resources={resources}
          onAddClick={openCreateModal}
          onEditClick={openEditModal}
          onStatusChange={handleStatusChange}
        />
      )}

      {showModal && (
        <ResourceModal
          resource={editingResource}
          onClose={() => setShowModal(false)}
          onSaved={handleSaved}
        />
      )}
    </section>
  );
}

function ResourcesTable({ resources, onAddClick, onEditClick, onStatusChange }) {
  if (!resources.length) {
    return (
      <div className="empty-state">
        <p>Nu există resurse pentru filtrele selectate.</p>
        <button className="primary-button" type="button" onClick={onAddClick}>Adaugă resursă</button>
      </div>
    );
  }

  return (
    <div className="table-card">
      <table>
        <thead>
          <tr>
            <th>Nume</th>
            <th>Tip</th>
            <th>Notițe</th>
            <th>Status</th>
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {resources.map((resource) => (
            <tr key={resource.id}>
              <td><strong>{resource.name}</strong></td>
              <td>{resource.type}</td>
              <td>{resource.notes || "-"}</td>
              <td><span className="status-pill">{resource.active ? "Activă" : "Inactivă"}</span></td>
              <td>
                <div className="row-actions">
                  <button className="small-button" type="button" onClick={() => onEditClick(resource)}>
                    Editează
                  </button>
                  <button className="small-button" type="button" onClick={() => onStatusChange(resource)}>
                    {resource.active ? "Dezactivează" : "Reactivează"}
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

function ResourceModal({ resource, onClose, onSaved }) {
  const isEdit = Boolean(resource);
  const [form, setForm] = useState(() => resource ? toResourceForm(resource) : EMPTY_RESOURCE_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");

    try {
      const payload = {
        name: form.name.trim(),
        type: form.type,
        notes: blankToNull(form.notes),
      };

      if (isEdit) {
        await updateResource(resource.id, payload);
      } else {
        await createResource(payload);
      }

      await onSaved(isEdit);
    } catch (err) {
      setError(getApiErrorMessage(err, isEdit ? "Nu am putut actualiza resursa." : "Nu am putut crea resursa."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-header">
          <div>
            <p className="page-kicker">Resursă</p>
            <h2>{isEdit ? "Editează resursă" : "Adaugă resursă"}</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose}>×</button>
        </div>

        <form className="form-card modal-form" onSubmit={handleSubmit}>
          {error && <div className="form-error">{error}</div>}

          <label>
            Nume resursă
            <input name="name" value={form.name} onChange={handleChange} required maxLength={150} />
          </label>

          <label>
            Tip
            <select name="type" value={form.type} onChange={handleChange} required>
              {RESOURCE_TYPES.map((type) => (
                <option key={type.value} value={type.value}>{type.label}</option>
              ))}
            </select>
          </label>

          <label>
            Notițe
            <textarea name="notes" value={form.notes} onChange={handleChange} />
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

function toResourceForm(resource) {
  return {
    name: resource.name || "",
    type: resource.type || "REFORMER",
    notes: resource.notes || "",
  };
}

function cleanParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== "" && value !== null && value !== undefined));
}

function blankToNull(value) {
  return value && value.trim() ? value.trim() : null;
}
