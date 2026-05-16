import axiosClient from "./axiosClient";

export async function getAdminAppointments(params = {}) {
  const response = await axiosClient.get("/admin/appointments", { params });
  return response.data;
}

export async function getAdminAppointment(id) {
  const response = await axiosClient.get(`/admin/appointments/${id}`);
  return response.data;
}

export async function getAppointmentFormOptions() {
  const response = await axiosClient.get("/admin/appointments/form-options");
  return response.data;
}

export async function createAppointment(payload) {
  const response = await axiosClient.post("/admin/appointments", payload);
  return response.data;
}

export async function updateAppointment(id, payload) {
  const response = await axiosClient.put(`/admin/appointments/${id}`, payload);
  return response.data;
}

export async function changeAppointmentStatus(id, status) {
  const response = await axiosClient.post(`/admin/appointments/${id}/change-status`, { status });
  return response.data;
}

export async function addAppointmentParticipants(id, participantClientIds) {
  const response = await axiosClient.post(`/admin/appointments/${id}/add-participants`, {
    participantClientIds,
  });
  return response.data;
}

export async function removeAppointmentParticipant(id, clientId) {
  const response = await axiosClient.post(`/admin/appointments/${id}/remove-participant`, {
    clientId,
  });
  return response.data;
}

export async function getAppointmentWaitlist(id) {
  const response = await axiosClient.get(`/admin/appointments/${id}/waitlist`);
  return response.data;
}

export async function addAppointmentWaitlistEntry(id, clientId) {
  const response = await axiosClient.post(`/admin/appointments/${id}/waitlist`, { clientId });
  return response.data;
}

export async function removeAppointmentWaitlistEntry(id, waitlistEntryId) {
  const response = await axiosClient.post(`/admin/appointments/${id}/waitlist/remove`, {
    waitlistEntryId,
  });
  return response.data;
}

export async function promoteAppointmentWaitlistEntry(id, waitlistEntryId) {
  const response = await axiosClient.post(`/admin/appointments/${id}/waitlist/promote`, {
    waitlistEntryId,
  });
  return response.data;
}

export async function cancelAppointment(id, payload = {}) {
  const response = await axiosClient.post(`/admin/appointments/${id}/cancel`, payload);
  return response.data;
}

export async function sendAppointmentConfirmation(id) {
  const response = await axiosClient.post(`/admin/appointments/${id}/send-confirmation`);
  return response.data;
}

export async function getClientFutureAppointments() {
  const response = await axiosClient.get("/client/appointments/future");
  return response.data;
}

export async function getClientAppointmentHistory() {
  const response = await axiosClient.get("/client/appointments/history");
  return response.data;
}
