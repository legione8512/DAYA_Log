# DAYA Log — API Smoke Test Flow

Use the included Postman collection and environment.

## Import into Postman

1. Import `postman/DAYA_Log_MVP.postman_collection.json`.
2. Import `postman/DAYA_Log_Local.postman_environment.json`.
3. Select the `DAYA Log Local` environment.
4. Confirm `baseUrl = http://localhost:8080`.

## Important notes

- The backend uses an HttpOnly refresh cookie for `/api/auth/refresh` and `/api/auth/logout`.
- Postman must keep cookies enabled.
- Login requests save the access token into environment variables.
- Some IDs are seeded and included in the environment.
- For dynamically created records, collection tests save IDs such as `createdClientId`, `createdServiceId`, `createdAppointmentId`.

## Recommended API order

1. `Auth / Admin Login`
2. `Profile / Me as Admin`
3. `Admin Dashboard / Summary`
4. `Catalogue / Services / List`
5. `Catalogue / Instructors / List`
6. `Catalogue / Resources / List`
7. `Clients / List Active Clients`
8. `Clients / Create QA Client`
9. `Clients / Get Created Client`
10. `Clients / Create User Account for Created Client`
11. `Appointments / Form Options`
12. `Appointments / Create Individual Appointment`
13. `Appointments / Get Created Appointment`
14. `Appointments / Send Confirmation`
15. `Appointments / Change Status to CONFIRMED`
16. `Appointments / List Appointments`
17. `Audit / List Logs`
18. `Auth / Logout Admin`

## Client API order

1. Create a client account from admin first.
2. `Auth / Client Login` using the account created for the client.
3. `Profile / Me as Client`
4. `Client Area / Dashboard`
5. `Client Area / Future Appointments`
6. `Client Area / History Appointments`
7. `Security Negative / Client Cannot Access Admin Dashboard`
8. `Auth / Logout Client`

## Smoke pass criteria

- All 2xx expected requests return 2xx.
- Negative security tests return 401 or 403.
- Business-rule conflict tests return 409 or a typed business error.
- Error response format contains at least `status`, `code`, `message`, and `path`.
