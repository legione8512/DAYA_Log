export function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function formatDate(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
  }).format(new Date(value));
}

export function formatAppointmentInterval(startAt, endAt) {
  if (!startAt || !endAt) {
    return "-";
  }

  const formatter = new Intl.DateTimeFormat("ro-RO", {
    hour: "2-digit",
    minute: "2-digit",
  });

  return `${formatter.format(new Date(startAt))} - ${formatter.format(new Date(endAt))}`;
}
