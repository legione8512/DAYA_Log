import { Navigate, Route, Routes } from "react-router-dom";
import AuthLayout from "../layouts/AuthLayout";
import AdminLayout from "../layouts/AdminLayout";
import ClientLayout from "../layouts/ClientLayout";
import LoginPage from "../pages/LoginPage";

import RequestPasswordResetPage from "../pages/RequestPasswordResetPage";
import ConfirmPasswordResetPage from "../pages/ConfirmPasswordResetPage";
import ConfirmEmailPage from "../pages/ConfirmEmailPage";
import UnauthorizedPage from "../pages/UnauthorizedPage";
import NotFoundPage from "../pages/NotFoundPage";

import LandingPage from "../pages/LandingPage";

import AdminDashboardPage from "../pages/admin/AdminDashboardPage";
import AppointmentsPage from "../pages/admin/AppointmentsPage";
import CreateAppointmentPage from "../pages/admin/CreateAppointmentPage";
import EditAppointmentPage from "../pages/admin/EditAppointmentPage";
import AppointmentDetailsPage from "../pages/admin/AppointmentDetailsPage";
import ClientsPage from "../pages/admin/ClientsPage";
import ClientDetailsPage from "../pages/admin/ClientDetailsPage";
import ServicesPage from "../pages/admin/ServicesPage";
import InstructorsPage from "../pages/admin/InstructorsPage";
import ResourcesPage from "../pages/admin/ResourcesPage";
import AuditLogsPage from "../pages/admin/AuditLogsPage";
import ClientDashboardPage from "../pages/client/ClientDashboardPage";
import MyAppointmentsPage from "../pages/client/MyAppointmentsPage";
import ProfilePage from "../pages/profile/ProfilePage";
import ProtectedRoute from "./ProtectedRoute";
import { useAuth } from "../context/AuthContext";

function RootRedirect() {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) {
    return (
      <main className="state-page">
        <section className="state-card state-card-small">
          <div className="loading-dot-row" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <h1>Se încarcă aplicația</h1>
          <p className="muted-text">Pregătim sesiunea înainte de redirecționare.</p>
        </section>
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/autentificare" replace />;
  }

  if (user?.forcePasswordChange) {
    return <Navigate to="/profil" replace />;
  }

  return <Navigate to={user?.role === "ADMIN" ? "/admin" : "/client"} replace />;
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/autentificare" element={<LoginPage />} />
        <Route path="/resetare-parola" element={<RequestPasswordResetPage />} />
        <Route path="/resetare-parola/confirmare" element={<ConfirmPasswordResetPage />} />
        <Route path="/confirmare-email" element={<ConfirmEmailPage />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/programari" element={<AppointmentsPage />} />
          <Route path="/admin/programari/noua" element={<CreateAppointmentPage />} />
          <Route path="/admin/programari/:id" element={<AppointmentDetailsPage />} />
          <Route path="/admin/programari/:id/editeaza" element={<EditAppointmentPage />} />
          <Route path="/admin/clienti" element={<ClientsPage />} />
          <Route path="/admin/clienti/:id" element={<ClientDetailsPage />} />
          <Route path="/admin/servicii" element={<ServicesPage />} />
          <Route path="/admin/instructori" element={<InstructorsPage />} />
          <Route path="/admin/resurse" element={<ResourcesPage />} />
          <Route path="/admin/audit" element={<AuditLogsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["CLIENT"]} />}>
        <Route element={<ClientLayout />}>
          <Route path="/client" element={<ClientDashboardPage />} />
          <Route path="/client/programarile-mele" element={<MyAppointmentsPage />} />
        </Route>
      </Route>

      <Route path="/" element={<LandingPage />} />
      <Route path="/app" element={<RootRedirect />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/profil" element={<ProfilePage />} />
      </Route>

      <Route path="/neautorizat" element={<UnauthorizedPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
