import axios from "axios";

// I create one Axios instance so all requests use the same API base URL.
const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// I read the token from localStorage first, then from sessionStorage.
axiosClient.interceptors.request.use((config) => {
  const token =
    localStorage.getItem("daya_access_token") ||
    sessionStorage.getItem("daya_access_token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export default axiosClient;