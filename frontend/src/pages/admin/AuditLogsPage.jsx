import { useEffect, useMemo, useState } from "react";
import { getAuditLog, listAuditLogs } from "../../api/auditApi";
import { getApiErrorMessage } from "../../utils/apiErrors";
import { formatDateTime } from "../../utils/dateTime";

const DEFAULT_FILTERS = {
  entityName: "",
  action: "",
  dateFrom: "",
  dateTo: "",
  page: 0,
  size: 20,
};

const ENTITY_OPTIONS = [
  "AUTH",
  "CLIENT",
  "APPOINTMENT",
  "APPOINTMENT_PARTICIPANT",
  "APPOINTMENT_WAITLIST",
  "SERVICE",
  "INSTRUCTOR",
  "INSTRUCTOR_WORKING_HOURS",
  "RESOURCE",
  "CATALOGUE",
];

const ACTION_OPTIONS = [
  "LOGIN",
  "LOGOUT",
  "CREATE",
  "UPDATE",
  "STATUS_CHANGE",
  "CANCEL",
  "SEND_CONFIRMATION",
  "ADD_PARTICIPANT",
  "REMOVE_PARTICIPANT",
  "ADD_WAITLIST",
  "REMOVE_WAITLIST",
  "PROMOTE_WAITLIST",
  "CREATE_USER_ACCOUNT",
  "CHANGE_PASSWORD",
  "PASSWORD_RESET_REQUEST",
  "PASSWORD_RESET_CONFIRM",
];

export default function AuditLogsPage() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [logs, setLogs] = useState([]);
  const [pageInfo, setPageInfo] = useState(null);
  const [selectedLog, setSelectedLog] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [error, setError] = useState("");
  const [detailsError, setDetailsError] = useState("");

  const hasPreviousPage = (pageInfo?.page || 0) > 0;
  const hasNextPage = useMemo(() => {
    if (!pageInfo) {
      return false;
    }

    return pageInfo.page + 1 < pageInfo.totalPages;
  }, [pageInfo]);

  const loadLogs = async (currentFilters = filters) => {
    setLoading(true);
    setError("");

    try {
      const data = await listAuditLogs(cleanFilters(currentFilters));
      setLogs(data.content || []);
      setPageInfo(data);
    } catch (err) {
      setError(getApiErrorMessage(err, "Nu am putut încărca jurnalul de audit."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs(DEFAULT_FILTERS);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((prev) => ({ ...prev, [name]: value, page: 0 }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    loadLogs(filters);
  };

  const handleReset = () => {
    setFilters(DEFAULT_FILTERS);
    setSelectedLog(null);
    loadLogs(DEFAULT_FILTERS);
  };

  const handlePageChange = async (nextPage) => {
    const nextFilters = { ...filters, page: nextPage };
    setFilters(nextFilters);
    await loadLogs(nextFilters);
  };

  const handleSelectLog = async (id) => {
    setDetailsLoading(true);
    setDetailsError("");

    try {
      const data = await getAuditLog(id);
      setSelectedLog(data);
    } catch (err) {
      setDetailsError(getApiErrorMessage(err, "Nu am putut încărca detaliile auditului."));
    } finally {
      setDetailsLoading(false);
    }
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="page-kicker">Observabilitate</p>
          <h1>Jurnal audit</h1>
          <p>Urmărește acțiunile importante făcute în platformă de utilizatorii studioului.</p>
        </div>
      </div>

      <form className="filter-card audit-filter-card" onSubmit={handleSubmit}>
        <label>
          Entitate
          <select name="entityName" value={filters.entityName} onChange={handleFilterChange}>
            <option value="">Toate</option>
            {ENTITY_OPTIONS.map((entityName) => (
              <option key={entityName} value={entityName}>{entityName}</option>
            ))}
          </select>
        </label>

        <label>
          Acțiune
          <select name="action" value={filters.action} onChange={handleFilterChange}>
            <option value="">Toate</option>
            {ACTION_OPTIONS.map((action) => (
              <option key={action} value={action}>{action}</option>
            ))}
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

      <div className="audit-layout-grid">
        <div>
          {loading ? (
            <div className="page-loading">Se încarcă jurnalul de audit...</div>
          ) : (
            <AuditLogsTable
              logs={logs}
              selectedLogId={selectedLog?.id}
              onSelectLog={handleSelectLog}
            />
          )}

          {pageInfo && (
            <div className="pagination-row">
              <p className="muted-text table-footer-text">
                Pagina {pageInfo.page + 1} din {pageInfo.totalPages || 1}. Total: {pageInfo.totalElements || 0} înregistrări.
              </p>

              <div className="row-actions">
                <button
                  className="small-button"
                  type="button"
                  disabled={!hasPreviousPage || loading}
                  onClick={() => handlePageChange(pageInfo.page - 1)}
                >
                  Înapoi
                </button>
                <button
                  className="small-button"
                  type="button"
                  disabled={!hasNextPage || loading}
                  onClick={() => handlePageChange(pageInfo.page + 1)}
                >
                  Înainte
                </button>
              </div>
            </div>
          )}
        </div>

        <AuditLogDetailsPanel
          auditLog={selectedLog}
          loading={detailsLoading}
          error={detailsError}
          onClose={() => setSelectedLog(null)}
        />
      </div>
    </section>
  );
}

function AuditLogsTable({ logs, selectedLogId, onSelectLog }) {
  if (!logs.length) {
    return <div className="empty-state">Nu există înregistrări de audit pentru filtrele selectate.</div>;
  }

  return (
    <div className="table-card audit-table-card">
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Entitate</th>
            <th>Acțiune</th>
            <th>Actor</th>
            <th>Rezumat</th>
            <th>Acțiuni</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.id} className={selectedLogId === log.id ? "selected-table-row" : ""}>
              <td>{formatDateTime(log.createdAt)}</td>
              <td>
                <span className="status-pill">{log.entityName}</span>
                <span className="table-subtext">{shortId(log.entityId)}</span>
              </td>
              <td>{log.action}</td>
              <td>
                {log.actorEmail || "-"}
                <span className="table-subtext">{shortId(log.actorUserId)}</span>
              </td>
              <td>{formatChangeSummaryPreview(log.changeSummary)}</td>
              <td>
                <button className="small-button" type="button" onClick={() => onSelectLog(log.id)}>
                  Detalii
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AuditLogDetailsPanel({ auditLog, loading, error, onClose }) {
  if (loading) {
    return <aside className="content-card audit-details-panel">Se încarcă detaliile...</aside>;
  }

  if (error) {
    return <aside className="content-card audit-details-panel form-error">{error}</aside>;
  }

  if (!auditLog) {
    return (
      <aside className="content-card audit-details-panel audit-empty-panel">
        <p className="page-kicker">Detalii</p>
        <h2>Selectează o înregistrare</h2>
        <p className="muted-text">Apasă pe „Detalii” pentru a vedea actorul, entitatea și rezumatul modificărilor.</p>
      </aside>
    );
  }

  return (
    <aside className="content-card audit-details-panel">
      <div className="section-title-row">
        <div>
          <p className="page-kicker">Detalii audit</p>
          <h2>{auditLog.action}</h2>
        </div>
        <button className="icon-button" type="button" onClick={onClose}>×</button>
      </div>

      <dl className="details-list">
        <div>
          <dt>Data</dt>
          <dd>{formatDateTime(auditLog.createdAt)}</dd>
        </div>
        <div>
          <dt>Entitate</dt>
          <dd>{auditLog.entityName}</dd>
        </div>
        <div>
          <dt>ID entitate</dt>
          <dd>{auditLog.entityId}</dd>
        </div>
        <div>
          <dt>Actor</dt>
          <dd>{auditLog.actorEmail || "-"}</dd>
        </div>
        <div>
          <dt>Rol actor</dt>
          <dd>{auditLog.actorRole || "-"}</dd>
        </div>
        <div>
          <dt>ID actor</dt>
          <dd>{auditLog.actorUserId}</dd>
        </div>
        <div>
          <dt>Studio ID</dt>
          <dd>{auditLog.studioId}</dd>
        </div>
      </dl>

      <div className="audit-json-block">
        <h3>Change summary</h3>
        <pre>{formatJson(auditLog.changeSummary)}</pre>
      </div>
    </aside>
  );
}

function cleanFilters(filters) {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== "" && value !== null && value !== undefined)
  );
}

function shortId(value) {
  if (!value) {
    return "-";
  }

  return String(value).slice(0, 8);
}

function formatChangeSummaryPreview(changeSummary) {
  if (!changeSummary || typeof changeSummary !== "object") {
    return "-";
  }

  const keys = Object.keys(changeSummary);

  if (!keys.length) {
    return "-";
  }

  return keys.slice(0, 3).join(", ");
}

function formatJson(value) {
  if (!value) {
    return "{}";
  }

  return JSON.stringify(value, null, 2);
}
