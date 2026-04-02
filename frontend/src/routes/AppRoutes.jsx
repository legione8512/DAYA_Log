import { Navigate, Route, Routes } from "react-router-dom";
import AuthLayout from "../layouts/AuthLayout";
import AdminLayout from "../layouts/AdminLayout";
import ClientLayout from "../layouts/ClientLayout";
import LoginPage from "../pages/LoginPage";
import AdminDashboardPage from "../pages/admin/AdminDashboardPage";
import AppointmentsPage from "../pages/admin/AppointmentsPage";
import ClientsPage from "../pages/admin/ClientsPage";
import ClientDashboardPage from "../pages/client/ClientDashboardPage";
import MyAppointmentsPage from "../pages/client/MyAppointmentsPage";
import ProtectedRoute from "./ProtectedRoute";

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/autentificare" element={<LoginPage />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/programari" element={<AppointmentsPage />} />
          <Route path="/admin/clienti" element={<ClientsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["CLIENT"]} />}>
        <Route element={<ClientLayout />}>
          <Route path="/client" element={<ClientDashboardPage />} />
          <Route
            path="/client/programarile-mele"
            element={<MyAppointmentsPage />}
          />
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/autentificare" replace />} />
      <Route path="*" element={<Navigate to="/autentificare" replace />} />
    </Routes>
  );
}