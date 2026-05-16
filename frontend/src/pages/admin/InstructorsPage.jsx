import { useEffect, useState } from "react";
import {
  createInstructor,
  getInstructorWorkingHours,
  listInstructors,
  replaceInstructorWorkingHours,
  updateInstructor,
  updateInstructorStatus,
} from "../../api/catalogueApi";
import { getApiErrorMessage } from "../../utils/apiErrors";

const EMPTY_INSTRUCTOR_FORM = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
};

const DAYS = [
  { value: "MONDAY", label: "Luni" },
  { value: "TUESDAY", label: "Marți" },
  { value: "WEDNESDAY", label: "Miercuri" },
  { value: "THURSDAY", label: "Joi" },
  { value: "FRIDAY", label: "Vineri" },
  { value: "SATURDAY", label: "Sâmbătă" },
  { value: "SUNDAY", label: "Duminică" },
];

export default function InstructorsPage() {
  const [query, setQuery] = useState("");
  const [active, setActive] = useState("");
  const [instructors, setInstructors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [editingInstructor, setEditingInstructor] = useState(null);
  const [workingHoursInstructor, setWorkingHoursInstructor] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const loadInstructors = async (currentQuery = query, currentActive = active) => {
    setLoading(true);
    setError("");

    try {
      const data = await listInstructors(cleanParams({ query: currentQuery, active: currentActive }));
      setInstructors(data);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca instructorii."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadInstructors(query, active);
    }, 300);

    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, active]);

  const openCreateModal = () => {
    setEditingInstructor(null);
    setShowModal(true);
  };

  const openEditModal = (instructor) => {
    setEditingInstructor(instructor);
    setShowModal(true);
  };

  const handleSaved = async (wasEdit) => {
    setShowModal(false);
    setEditingInstructor(null);
    setSuccess(wasEdit ? "Instructorul a fost actualizat." : "Instructorul a fost adăugat.");
    await loadInstructors(query, active);
  };

  const handleStatusChange = async (instructor) => {
    setError("");
    setSuccess("");

    try {
      await updateInstructorStatus(instructor.id, !instructor.active);
      setSuccess(instructor.active ? "Instructorul a fost dezactivat." : "Instructorul a fost reactivat.");
      await loadInstructors(query, active);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut actualiza statusul instructorului."));
    }
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Catalog</p>
          <h1>Instructori</h1>
          <p>Administrează instructorii și programul lor de lucru.</p>
        </div>
        <button className="primary-button" type="button" onClick={openCreateModal}>
          Adaugă instructor
        </button>
      </div>

      <div className="filter-card compact-filter-card">
        <label>
          Caută instructor
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
        <div className="page-loading">Se încarcă instructorii...</div>
      ) : (
        <InstructorsTable
          instructors={instructors}
          onAddClick={openCreateModal}
          onEditClick={openEditModal}
          onStatusChange={handleStatusChange}
          onWorkingHoursClick={setWorkingHoursInstructor}
        />
      )}

      {showModal && (
        <InstructorModal
          instructor={editingInstructor}
          onClose={() => setShowModal(false)}
          onSaved={handleSaved}
        />
      )}

      {workingHoursInstructor && (
        <WorkingHoursModal
          instructor={workingHoursInstructor}
          onClose={() => setWorkingHoursInstructor(null)}
          onSaved={() => {
            setWorkingHoursInstructor(null);
            setSuccess("Programul instructorului a fost actualizat.");
          }}
        />
      )}
    </section>
  );
}

function InstructorsTable({ instructors, onAddClick, onEditClick, onStatusChange, onWorkingHoursClick }) {
  if (!instructors.length) {
    return (
      <div className="empty-state">
        <p>Nu există instructori pentru filtrele selectate.</p>
        <button className="primary-button" type="button" onClick={onAddClick}>Adaugă instructor</button>
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
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {instructors.map((instructor) => (
            <tr key={instructor.id}>
              <td><strong>{instructor.fullName || `${instructor.firstName} ${instructor.lastName}`}</strong></td>
              <td>{instructor.email || "-"}</td>
              <td>{instructor.phone || "-"}</td>
              <td><span className="status-pill">{instructor.active ? "Activ" : "Inactiv"}</span></td>
              <td>
                <div className="row-actions">
                  <button className="small-button" type="button" onClick={() => onEditClick(instructor)}>
                    Editează
                  </button>
                  <button className="small-button" type="button" onClick={() => onWorkingHoursClick(instructor)}>
                    Program
                  </button>
                  <button className="small-button" type="button" onClick={() => onStatusChange(instructor)}>
                    {instructor.active ? "Dezactivează" : "Reactivează"}
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

function InstructorModal({ instructor, onClose, onSaved }) {
  const isEdit = Boolean(instructor);
  const [form, setForm] = useState(() => instructor ? toInstructorForm(instructor) : EMPTY_INSTRUCTOR_FORM);
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
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: blankToNull(form.email),
        phone: blankToNull(form.phone),
      };

      if (isEdit) {
        await updateInstructor(instructor.id, payload);
      } else {
        await createInstructor(payload);
      }

      await onSaved(isEdit);
    } catch (err) {
      setError(getApiErrorMessage(err, isEdit ? "Nu am putut actualiza instructorul." : "Nu am putut crea instructorul."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-header">
          <div>
            <p className="page-kicker">Instructor</p>
            <h2>{isEdit ? "Editează instructor" : "Adaugă instructor"}</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose}>×</button>
        </div>

        <form className="form-card modal-form" onSubmit={handleSubmit}>
          {error && <div className="form-error">{error}</div>}

          <div className="form-grid">
            <label>
              Prenume
              <input name="firstName" value={form.firstName} onChange={handleChange} required maxLength={100} />
            </label>

            <label>
              Nume
              <input name="lastName" value={form.lastName} onChange={handleChange} required maxLength={100} />
            </label>

            <label>
              Email
              <input type="email" name="email" value={form.email} onChange={handleChange} maxLength={150} />
            </label>

            <label>
              Telefon
              <input name="phone" value={form.phone} onChange={handleChange} maxLength={30} />
            </label>
          </div>

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

function WorkingHoursModal({ instructor, onClose, onSaved }) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadWorkingHours() {
      setLoading(true);
      setError("");

      try {
        const data = await getInstructorWorkingHours(instructor.id);

        if (!cancelled) {
          setEntries((data.workingHours || []).map((entry) => ({
            dayOfWeek: entry.dayOfWeek,
            startTime: normalizeTime(entry.startTime),
            endTime: normalizeTime(entry.endTime),
          })));
        }
      } catch (err) {
        if (!cancelled) {
          setError(getApiErrorMessage(err, "Nu am putut încărca programul instructorului."));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadWorkingHours();

    return () => {
      cancelled = true;
    };
  }, [instructor.id]);

  const addEntry = () => {
    setEntries((prev) => [
      ...prev,
      { dayOfWeek: "MONDAY", startTime: "09:00", endTime: "17:00" },
    ]);
  };

  const removeEntry = (index) => {
    setEntries((prev) => prev.filter((_, itemIndex) => itemIndex !== index));
  };

  const updateEntry = (index, field, value) => {
    setEntries((prev) => prev.map((entry, itemIndex) => (
      itemIndex === index ? { ...entry, [field]: value } : entry
    )));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");

    try {
      await replaceInstructorWorkingHours(instructor.id, entries);
      onSaved();
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut salva programul instructorului."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card wide-modal">
        <div className="modal-header">
          <div>
            <p className="page-kicker">Program lucru</p>
            <h2>{instructor.fullName || `${instructor.firstName} ${instructor.lastName}`}</h2>
            <p className="muted-text">Programările noi sunt validate pe baza acestor intervale.</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose}>×</button>
        </div>

        {loading ? (
          <div className="page-loading">Se încarcă programul...</div>
        ) : (
          <form className="form-card modal-form" onSubmit={handleSubmit}>
            {error && <div className="form-error">{error}</div>}

            <div className="working-hours-list">
              {entries.length === 0 && (
                <div className="empty-state compact-empty">
                  Nu există încă intervale definite pentru acest instructor.
                </div>
              )}

              {entries.map((entry, index) => (
                <div className="working-hours-row" key={`${entry.dayOfWeek}-${index}`}>
                  <label>
                    Zi
                    <select value={entry.dayOfWeek} onChange={(event) => updateEntry(index, "dayOfWeek", event.target.value)}>
                      {DAYS.map((day) => (
                        <option key={day.value} value={day.value}>{day.label}</option>
                      ))}
                    </select>
                  </label>

                  <label>
                    Ora început
                    <input
                      type="time"
                      value={entry.startTime}
                      onChange={(event) => updateEntry(index, "startTime", event.target.value)}
                      required
                    />
                  </label>

                  <label>
                    Ora sfârșit
                    <input
                      type="time"
                      value={entry.endTime}
                      onChange={(event) => updateEntry(index, "endTime", event.target.value)}
                      required
                    />
                  </label>

                  <button className="small-button danger" type="button" onClick={() => removeEntry(index)}>
                    Șterge
                  </button>
                </div>
              ))}
            </div>

            <div className="catalogue-inline-actions">
              <button className="secondary-button" type="button" onClick={addEntry}>Adaugă interval</button>
            </div>

            <div className="modal-actions">
              <button className="secondary-button" type="button" onClick={onClose}>Renunță</button>
              <button className="primary-button" type="submit" disabled={saving}>
                {saving ? "Se salvează..." : "Salvează programul"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

function toInstructorForm(instructor) {
  return {
    firstName: instructor.firstName || "",
    lastName: instructor.lastName || "",
    email: instructor.email || "",
    phone: instructor.phone || "",
  };
}

function normalizeTime(value) {
  if (!value) {
    return "";
  }

  return value.slice(0, 5);
}

function cleanParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== "" && value !== null && value !== undefined));
}

function blankToNull(value) {
  return value && value.trim() ? value.trim() : null;
}
