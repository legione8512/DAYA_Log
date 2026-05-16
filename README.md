# DAYA Log QA Manual Pack

This pack contains the manual QA material for validating the DAYA Log MVP end-to-end before release.

## Files

- `docs/QA_MANUAL_FLOW.md` — step-by-step browser QA for ADMIN and CLIENT flows.
- `docs/API_SMOKE_TEST_FLOW.md` — API-focused smoke flow using Postman.
- `docs/BUG_REPORT_TEMPLATE.md` — bug report template for failed QA cases.
- `docs/UAT_SIGNOFF_TEMPLATE.md` — sign-off template for final acceptance.
- `postman/DAYA_Log_MVP.postman_collection.json` — Postman collection for the backend API.
- `postman/DAYA_Log_Local.postman_environment.json` — local Postman environment variables.

## Recommended order

1. Start backend.
2. Start frontend.
3. Run `npm run lint` and `npm run build` in `frontend/`.
4. Complete the browser QA flow in `QA_MANUAL_FLOW.md`.
5. Run the API smoke flow in Postman.
6. Record all issues using the bug template.
7. Complete UAT sign-off only after all P0/P1 blockers are closed.
