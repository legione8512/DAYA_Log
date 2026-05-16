const ACCESS_TOKEN_KEY = "daya_access_token";
const REMEMBER_ME_KEY = "daya_remember_me";

export function saveAccessToken(accessToken, rememberMe) {
  clearAccessToken();

  if (!accessToken) {
    return;
  }

  const storage = rememberMe ? localStorage : sessionStorage;
  storage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REMEMBER_ME_KEY, rememberMe ? "true" : "false");
}

export function getAccessToken() {
  return (
    localStorage.getItem(ACCESS_TOKEN_KEY) ||
    sessionStorage.getItem(ACCESS_TOKEN_KEY)
  );
}

export function shouldRememberSession() {
  return localStorage.getItem(REMEMBER_ME_KEY) === "true";
}

export function clearAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
}

export function clearAuthStorage() {
  clearAccessToken();
  localStorage.removeItem(REMEMBER_ME_KEY);
}
