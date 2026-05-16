import axiosClient from "./axiosClient";

export async function getAdminDashboard() {
  const response = await axiosClient.get("/admin/dashboard");
  return response.data;
}

export async function getClientDashboard() {
  const response = await axiosClient.get("/client/dashboard");
  return response.data;
}
