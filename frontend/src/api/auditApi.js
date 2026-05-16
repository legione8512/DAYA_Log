import axiosClient from "./axiosClient";

export async function listAuditLogs(params = {}) {
  const response = await axiosClient.get("/admin/audit-logs", { params });
  return response.data;
}

export async function getAuditLog(id) {
  const response = await axiosClient.get(`/admin/audit-logs/${id}`);
  return response.data;
}
