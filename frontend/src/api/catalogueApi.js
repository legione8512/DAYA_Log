import axiosClient from "./axiosClient";

export async function listServices(params = {}) {
  const response = await axiosClient.get("/admin/services", { params });
  return response.data;
}

export async function createService(payload) {
  const response = await axiosClient.post("/admin/services", payload);
  return response.data;
}

export async function updateService(id, payload) {
  const response = await axiosClient.put(`/admin/services/${id}`, payload);
  return response.data;
}

export async function updateServiceStatus(id, active) {
  await axiosClient.patch(`/admin/services/${id}/status`, { active });
}

export async function listInstructors(params = {}) {
  const response = await axiosClient.get("/admin/instructors", { params });
  return response.data;
}

export async function createInstructor(payload) {
  const response = await axiosClient.post("/admin/instructors", payload);
  return response.data;
}

export async function updateInstructor(id, payload) {
  const response = await axiosClient.put(`/admin/instructors/${id}`, payload);
  return response.data;
}

export async function updateInstructorStatus(id, active) {
  await axiosClient.patch(`/admin/instructors/${id}/status`, { active });
}

export async function getInstructorWorkingHours(id) {
  const response = await axiosClient.get(`/admin/instructors/${id}/working-hours`);
  return response.data;
}

export async function replaceInstructorWorkingHours(id, entries) {
  const response = await axiosClient.put(`/admin/instructors/${id}/working-hours`, { entries });
  return response.data;
}

export async function listResources(params = {}) {
  const response = await axiosClient.get("/admin/resources", { params });
  return response.data;
}

export async function createResource(payload) {
  const response = await axiosClient.post("/admin/resources", payload);
  return response.data;
}

export async function updateResource(id, payload) {
  const response = await axiosClient.put(`/admin/resources/${id}`, payload);
  return response.data;
}

export async function updateResourceStatus(id, active) {
  await axiosClient.patch(`/admin/resources/${id}/status`, { active });
}
