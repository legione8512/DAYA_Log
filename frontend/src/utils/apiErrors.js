export function getApiErrorMessage(error, fallbackMessage = "A apărut o eroare. Încearcă din nou.") {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallbackMessage
  );
}
