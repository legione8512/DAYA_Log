import axiosClient from "./axiosClient";

export async function loginRequest(payload) {
  const response = await axiosClient.post("/auth/login", payload);
  return response.data;
}

export async function refreshRequest() {
  const response = await axiosClient.post("/auth/refresh");
  return response.data;
}

export async function logoutRequest() {
  await axiosClient.post("/auth/logout");
}

export async function requestPasswordReset(payload) {
  const response = await axiosClient.post("/auth/password-reset/request", payload);
  return response.data;
}

export async function confirmPasswordReset(payload) {
  const response = await axiosClient.post("/auth/password-reset/confirm", payload);
  return response.data;
}

export async function confirmEmail(token) {
  const response = await axiosClient.get("/auth/confirm-email", {
    params: { token },
  });
  return response.data;
}
