import axiosClient from "./axiosClient";

export async function getCurrentUser() {
  const response = await axiosClient.get("/profile/me");
  return response.data;
}

export async function changePassword(payload) {
  const response = await axiosClient.post("/profile/change-password", payload);
  return response.data;
}
