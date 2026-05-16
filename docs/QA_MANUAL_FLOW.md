# DAYA Log — Manual QA Flow

Use this file as the browser-level QA checklist for the MVP. Test with both backend and frontend running locally.

## Local URLs

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- API base: `http://localhost:8080/api`

## Demo/admin account from seed

- Email: `admin@dayalog.ro`
- Password: `Admin123!Change`

Seeded clients do not have login accounts by default. Create a CLIENT account from the admin client detail page before testing the client UI.

## Pre-flight checks

| Status | Check | Expected result |
|---|---|---|
| ☐ | Backend starts | Spring Boot starts on port 8080 without Flyway/JPA validation errors. |
| ☐ | Frontend starts | Vite starts on port 5173. |
| ☐ | `npm run lint` | No ESLint errors. |
| ☐ | `npm run build` | Production build succeeds. |
| ☐ | Browser console | No persistent React/JS errors during navigation. |
| ☐ | Network tab | API requests go to `http://localhost:8080/api`. |
| ☐ | CORS | No CORS errors when calling backend from frontend. |

---

# 1. Authentication QA

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/` | Redirects to `/autentificare` when not logged in. |
| ☐ | Login with wrong password | Shows Romanian error, does not authenticate. |
| ☐ | Login as ADMIN | Redirects to `/admin` or admin dashboard. |
| ☐ | Refresh browser page | Session is restored via refresh cookie. |
| ☐ | Open `/profil` | Profile page loads current user data. |
| ☐ | Try weak/new invalid password | Validation or backend error appears clearly. |
| ☐ | Logout | Session clears and returns to login. |
| ☐ | Open `/admin` after logout | Protected route redirects to login. |

---

# 2. Admin dashboard QA

| Status | Step | Expected result |
|---|---|---|
| ☐ | Login as ADMIN | Admin layout is visible. |
| ☐ | Open `/admin` | Summary cards load without error. |
| ☐ | Check sidebar links | Programări, Clienți, Servicii, Instructori, Resurse, Audit all navigate correctly. |
| ☐ | Refresh page | Route remains accessible and session is preserved. |

---

# 3. Catalogue QA

## Services

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/admin/servicii` | Existing seeded services appear. |
| ☐ | Search service | List filters correctly. |
| ☐ | Add service | New service appears in list. |
| ☐ | Edit service | Updated values appear after save. |
| ☐ | Deactivate service | Status changes and service should not be usable for new bookings if backend blocks inactive services. |
| ☐ | Reactivate service | Service becomes active again. |

## Instructors

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/admin/instructori` | Existing seeded instructors appear. |
| ☐ | Add instructor | New instructor appears. |
| ☐ | Edit instructor | Updated values appear. |
| ☐ | Deactivate/reactivate instructor | Status changes correctly. |
| ☐ | Open working hours modal | Existing hours load or empty state appears. |
| ☐ | Save working hours | New working hours are saved and visible after reopening. |

## Resources

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/admin/resurse` | Existing seeded resources appear. |
| ☐ | Add resource | New resource appears. |
| ☐ | Edit resource | Updated values appear. |
| ☐ | Deactivate/reactivate resource | Status changes correctly. |

---

# 4. Client management QA

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/admin/clienti` | Seeded clients appear. |
| ☐ | Search by name | Matching clients appear. |
| ☐ | Search by phone | Matching clients appear. |
| ☐ | Add client | Client is created and appears in list. |
| ☐ | Open client details | `/admin/clienti/:id` loads complete client profile. |
| ☐ | Edit client | Updated details persist after refresh. |
| ☐ | Sensitive fields visible to ADMIN | Medical notes/restrictions are visible only in admin view. |
| ☐ | Deactivate client | Client status changes and inactive client should not be usable for new bookings. |
| ☐ | Reactivate client | Client becomes active again. |
| ☐ | Create platform account | Account section updates and shows account email/role/force password flag. |

---

# 5. Appointment creation QA

Before testing appointments, make sure the selected instructor has working hours covering the appointment time.

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/admin/programari/noua` | Form options load: services, instructors, resources. |
| ☐ | Search/select client | Active clients can be selected. |
| ☐ | Select service | End time is suggested from service duration. |
| ☐ | Create valid INDIVIDUAL appointment | Appointment is saved and redirects/list updates. |
| ☐ | Open appointment details | Participants, service, instructor, resource, interval and status are visible. |
| ☐ | Create overlapping appointment for same client | Backend conflict error is shown in Romanian. |
| ☐ | Create appointment outside working hours | Backend business-rule error is shown. |
| ☐ | Create valid GROUP appointment | Group appointment is saved with capacity and participants. |
| ☐ | Try capacity lower than participant count | Validation/error appears. |
| ☐ | Try duplicate compatible group shell | Backend should reject/suggest using existing group session. |

---

# 6. Appointment details/actions QA

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/admin/programari` | Appointment list loads with filters. |
| ☐ | Use date/status/type filters | List updates correctly. |
| ☐ | Open details | Details page loads. |
| ☐ | Send confirmation | Success message appears; backend logs/sends email through configured EmailService. |
| ☐ | Change status to CONFIRMED | Status updates. |
| ☐ | Change status to COMPLETED for suitable appointment | Status updates or backend rejects if rule does not allow it. |
| ☐ | Cancel appointment more than 3h before start | Status becomes CANCELLED. |
| ☐ | Cancel appointment less than 3h before start | Backend rejects with business-rule message. |
| ☐ | Edit appointment | Changes persist and detail page shows updated values. |
| ☐ | Edit cancelled appointment | UI blocks or backend rejects. |

---

# 7. Group participant/waitlist QA

No automatic waitlist promotion should occur.

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open a GROUP appointment details page | Participant controls are visible. |
| ☐ | Add participant | Participant appears if capacity and conflicts allow. |
| ☐ | Remove participant | Participant is removed. |
| ☐ | Add client to waitlist | Client appears in waitlist with position. |
| ☐ | Remove client from waitlist | Entry disappears/updates. |
| ☐ | Promote manually | Client is added to appointment only after pressing manual promote. |
| ☐ | Do nothing after removing participant | No automatic waitlist promotion happens. |

---

# 8. Client UI QA

Create a platform account for a client first. Then log out from admin and log in as that client.

| Status | Step | Expected result |
|---|---|---|
| ☐ | Login as CLIENT | Redirects to `/client` or `/profil` if forcePasswordChange is true. |
| ☐ | If forcePasswordChange true, change password | User can continue after password change. |
| ☐ | Open `/client` | Client dashboard loads only own data. |
| ☐ | Open `/client/programarile-mele` | Future/history tabs load. |
| ☐ | Check sensitive data | Client does not see medical notes/restrictions. |
| ☐ | Try opening `/admin` manually | Redirects to `/neautorizat`. |
| ☐ | Logout | Session clears. |

---

# 9. Audit QA

| Status | Step | Expected result |
|---|---|---|
| ☐ | Open `/admin/audit` | Audit list loads. |
| ☐ | Filter by entity | Results update. |
| ☐ | Filter by action | Results update. |
| ☐ | Filter by date range | Results update. |
| ☐ | Open audit details | JSON change summary is visible and readable. |
| ☐ | Perform new client/appointment action | New audit entry appears after refresh. |

---

# 10. Final release decision

| Status | Gate | Pass condition |
|---|---|---|
| ☐ | P0 bugs | No P0 bugs open. |
| ☐ | P1 bugs | Either fixed or explicitly accepted for MVP. |
| ☐ | Auth/security | Role restrictions verified manually. |
| ☐ | Scheduling rules | Main conflict rules verified. |
| ☐ | Backup | Restore/rollback procedure tested or documented. |
| ☐ | Build | Backend and frontend builds pass. |
| ☐ | UAT | Admin and client flows accepted. |
