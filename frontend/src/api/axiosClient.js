import axios from "axios";
import {
  clearAuthStorage,
  getAccessToken,
  saveAccessToken,
  shouldRememberSession,
} from "../utils/authStorage";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

let refreshPromise = null;

function shouldSkipRefresh(url = "") {
  return [
    "/auth/login",
    "/auth/refresh",
    "/auth/logout",
    "/auth/password-reset/request",
    "/auth/password-reset/confirm",
    "/auth/confirm-email",
  ].some((publicPath) => url.includes(publicPath));
}

function notifyAuthExpired() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("daya:auth-expired"));
  }
}

axiosClient.interceptors.request.use((config) => {
  const token = getAccessToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;

    if (
      status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      shouldSkipRefresh(originalRequest.url)
    ) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post(`${API_BASE_URL}/auth/refresh`, null, {
            withCredentials: true,
            headers: {
              "Content-Type": "application/json",
            },
          })
          .finally(() => {
            refreshPromise = null;
          });
      }

      const refreshResponse = await refreshPromise;
      const newAccessToken = refreshResponse.data?.accessToken;

      if (!newAccessToken) {
        throw new Error("Refresh response did not contain an access token.");
      }

      saveAccessToken(newAccessToken, shouldRememberSession());

      originalRequest.headers = originalRequest.headers || {};
      originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

      return axiosClient(originalRequest);
    } catch (refreshError) {
      clearAuthStorage();
      notifyAuthExpired();
      return Promise.reject(refreshError);
    }
  }
);

export default axiosClient;
