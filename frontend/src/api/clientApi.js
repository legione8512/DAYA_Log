import axiosClient from "./axiosClient";

export async function searchClients(params = {}) {
  const response = await axiosClient.get("/admin/clients", { params });
  return response.data;
}

export async function createClient(payload) {
  const response = await axiosClient.post("/admin/clients", payload);
  return response.data;
}

export async function getClientDetails(id) {
  const response = await axiosClient.get(`/admin/clients/${id}`);
  return response.data;
}

export async function updateClient(id, payload) {
  const response = await axiosClient.put(`/admin/clients/${id}`, payload);
  return response.data;
}

export async function getClientAppointments(id) {
  const response = await axiosClient.get(`/admin/clients/${id}/appointments`);
  return response.data;
}

export async function updateClientStatus(id, active) {
  await axiosClient.patch(`/admin/clients/${id}/status`, { active });
}

export async function createClientUserAccount(id, payload) {
  const response = await axiosClient.post(`/admin/clients/${id}/create-user-account`, payload);
  return response.data;
}
