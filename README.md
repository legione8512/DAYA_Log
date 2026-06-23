# DAYA Log

DAYA Log is a web application designed for managing a Pilates studio. The application provides a public landing page, an information page, authentication, role-based dashboards, appointment management, client management, services, instructors, resources, audit logs, profile management, password reset and email confirmation workflows.

The project is currently structured as a full-stack application with a React/Vite frontend, a Spring Boot backend and a PostgreSQL database hosted on Neon.

## Production URLs

Frontend:

```txt
https://daya.cyber-half.com
```

Backend API:

```txt
https://daya-log-backend.onrender.com/api
```

Backend health check:

```txt
https://daya-log-backend.onrender.com/api/health
```

Database:

```txt
Neon PostgreSQL
```

## Current hosting architecture

The frontend is hosted on the Romarg shared hosting account under the domain root:

```txt
daya.cyber-half.com
```

The backend is hosted on Render as a Docker-based Web Service.

The database remains hosted on Neon PostgreSQL. The frontend does not connect directly to Neon. All database access is handled by the Spring Boot backend.

Current production flow:

```txt
User browser / future mobile app
        ↓
https://daya.cyber-half.com
        ↓
https://daya-log-backend.onrender.com/api
        ↓
Neon PostgreSQL
```

## Main application routes

Public routes:

```txt
/                    Public landing page
/informatii          Application information page
/autentificare       Login page
/resetare-parola     Password reset request
/resetare-parola/confirmare
/confirmare-email
/neautorizat         Unauthorised access page
```

Protected admin routes:

```txt
/admin
/admin/programari
/admin/programari/noua
/admin/programari/:id
/admin/programari/:id/editeaza
/admin/clienti
/admin/clienti/:id
/admin/servicii
/admin/instructori
/admin/resurse
/admin/audit
```

Protected client routes:

```txt
/client
/client/programarile-mele
```

General protected routes:

```txt
/profil
/app                 Redirects authenticated users to the correct dashboard
```

## Features

Current implemented features include:

- Public landing page with DAYA logo.
- Application information page.
- User authentication.
- Role-based access control for ADMIN and CLIENT users.
- Admin dashboard.
- Client dashboard.
- Appointment management.
- Client management.
- Service management.
- Instructor management.
- Resource management.
- Audit log area.
- User profile page.
- Password reset flow.
- Email confirmation flow.
- Protected routes using authentication state.
- Backend health check endpoint.
- Neon PostgreSQL database integration.
- Flyway database migrations.
- Docker-based backend deployment on Render.

## Technology stack

Frontend:

```txt
React
Vite
React Router
JavaScript
CSS
```

Backend:

```txt
Java 21
Spring Boot
Spring Security
Spring Data JPA
Hibernate
Flyway
Maven
Docker
```

Database:

```txt
PostgreSQL hosted on Neon
```

Hosting/deployment:

```txt
Frontend: Romarg shared hosting
Backend: Render Web Service using Docker
Database: Neon PostgreSQL
```

## Project structure

Expected root structure:

```txt
DAYA_Log/
  backend/
    src/
    pom.xml
    mvnw
    mvnw.cmd
    Dockerfile

  frontend/
    public/
      DAYA_Logo.svg
    src/
      pages/
      routes/
      layouts/
      context/
      api/
    index.html
    package.json
    .env
    .env.production
```

## Frontend setup

Go to the frontend directory:

```powershell
cd "D:\Learning\GitHub\Daya Log\frontend"
```

Install dependencies:

```powershell
npm install
```

Run locally:

```powershell
npm run dev
```

Build for production:

```powershell
npm run build
```

The production build is generated in:

```txt
frontend/dist
```

Only the contents of `frontend/dist` should be uploaded to the Romarg hosting folder, not the `dist` folder itself.

Correct hosting structure:

```txt
daya.cyber-half.com/index.html
daya.cyber-half.com/assets/
daya.cyber-half.com/DAYA_Logo.svg
daya.cyber-half.com/.htaccess
```

Incorrect hosting structure:

```txt
daya.cyber-half.com/dist/index.html
```

## Frontend environment variables

Local development file:

```txt
frontend/.env
```

Example local value:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Production file:

```txt
frontend/.env.production
```

Current production value:

```env
VITE_API_BASE_URL=https://daya-log-backend.onrender.com/api
```

Important: do not place database credentials, JWT secrets or backend secrets inside frontend `.env` files. Vite variables using the `VITE_` prefix are exposed to the browser after build.

## Frontend page title and favicon

The browser tab title is configured in:

```txt
frontend/index.html
```

Current recommended configuration:

```html
<link rel="icon" type="image/svg+xml" href="/DAYA_Logo.svg" />
<title>DAYA</title>
```

The logo file should be placed in:

```txt
frontend/public/DAYA_Logo.svg
```

## React routing and hosting rewrite

Because this is a React single-page application, direct links such as `/autentificare`, `/admin` and `/informatii` must be redirected to `index.html` by the web server.

The hosting folder should contain this `.htaccess` file:

```apache
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteBase /

  RewriteRule ^index\.html$ - [L]

  RewriteCond %{REQUEST_FILENAME} !-f
  RewriteCond %{REQUEST_FILENAME} !-d
  RewriteRule . /index.html [L]
</IfModule>
```

## Backend setup

Go to the backend directory:

```powershell
cd "D:\Learning\GitHub\Daya Log\backend"
```

Build locally:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Generated JAR:

```txt
backend/target/dayalog-backend-0.0.1-SNAPSHOT.jar
```

Run locally:

```powershell
java -jar target/dayalog-backend-0.0.1-SNAPSHOT.jar
```

## Backend environment variables

The backend uses environment variables for database access, JWT configuration, cookies, CORS and mail settings.

Required production variables:

```env
DB_URL=jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
DB_USERNAME=YOUR_NEON_USERNAME
DB_PASSWORD=YOUR_NEON_PASSWORD

JWT_ACCESS_SECRET=YOUR_LONG_RANDOM_ACCESS_SECRET
JWT_REFRESH_SECRET=YOUR_LONG_RANDOM_REFRESH_SECRET

APP_BASE_URL=https://daya.cyber-half.com
APP_CORS_ALLOWED_ORIGINS=https://daya.cyber-half.com
APP_COOKIES_SECURE=true

MAIL_FROM=contact@hypso25.ro
```

Do not commit real values for these variables to GitHub.

## Backend production port

For Render deployment, the backend must read the server port from Render’s `PORT` variable.

Recommended `application.yml` configuration:

```yaml
server:
  port: ${PORT:${SERVER_PORT:8080}}
```

This allows:

- Render to provide the port using `PORT`.
- Local development to use `SERVER_PORT` if needed.
- Default fallback to `8080`.

## Backend health check

The backend health endpoint is:

```txt
/api/health
```

Expected response:

```txt
DAYA Log backend is running
```

This route must be public in Spring Security.

## Render deployment

Render service type:

```txt
Web Service
```

Language/runtime:

```txt
Docker
```

Repository:

```txt
GitHub repository containing the DAYA Log project
```

Root directory:

```txt
backend
```

Dockerfile path:

```txt
./Dockerfile
```

Health check path:

```txt
/api/health
```

Instance type used during development:

```txt
Free
```

Important note: Render Free services may sleep after inactivity. The first request after a period of inactivity can be slow while the service wakes up.

## Backend Dockerfile

The backend Dockerfile should be located at:

```txt
backend/Dockerfile
```

Expected content:

```dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/dayalog-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Database

The application uses Neon PostgreSQL.

The backend connects to Neon using JDBC through `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

Example JDBC format:

```env
DB_URL=jdbc:postgresql://ep-example.eu-central-1.aws.neon.tech/neondb?sslmode=require
```

Do not expose the Neon connection string to the frontend.

Flyway is enabled and manages database migrations from:

```txt
backend/src/main/resources/db/migration
```

## Authentication and security notes

The application uses Spring Security and role-based route protection.

Known roles:

```txt
ADMIN
CLIENT
```

Do not document real production passwords in this README. If the initial seeded admin account is used for development, change its password before using the application publicly.

## Git workflow

Before pushing changes, check the repository state:

```powershell
git status
```

If there are changes:

```powershell
git add .
git commit -m "Describe the change"
git push
```

If a rebase is in progress, resolve conflicts first, then:

```powershell
git add .
git rebase --continue
git push
```

To check for unresolved merge markers:

```powershell
Get-ChildItem -Recurse -Include *.java,*.yml,*.jsx,*.js | Select-String -Pattern "<<<<<<<|=======|>>>>>>>"
```

## Deployment workflow

Frontend deployment:

```powershell
cd "D:\Learning\GitHub\Daya Log\frontend"
npm run build
```

Then upload the contents of:

```txt
frontend/dist
```

to:

```txt
daya.cyber-half.com
```

Backend deployment:

```powershell
cd "D:\Learning\GitHub\Daya Log"
git add .
git commit -m "Backend update"
git push
```

Render auto-deploys from GitHub when auto-deploy is enabled.

## Current development status

Completed:

- Frontend deployed to `https://daya.cyber-half.com`.
- Backend deployed to Render.
- Backend connected to Neon.
- Public `/api/health` endpoint confirmed working.
- Login confirmed working with the deployed frontend and backend.
- Public landing page added.
- Application information page added.
- Favicon and browser title changed to DAYA.

Next planned step:

```txt
Capacitor wrapper for Android and later iOS
```

## Planned mobile wrapper

The intended mobile strategy is to use Capacitor to wrap the existing React frontend.

Planned Android steps:

```powershell
cd "D:\Learning\GitHub\Daya Log\frontend"
npm install @capacitor/core @capacitor/cli
npx cap init
npm install @capacitor/android
npx cap add android
npm run build
npx cap sync android
npx cap open android
```

The mobile app should use the production backend API:

```env
VITE_API_BASE_URL=https://daya-log-backend.onrender.com/api
```

Do not start Capacitor until the web version is stable and login works online.

## Important reminders

- Never commit `.env` files containing real secrets.
- Do not expose Neon credentials in the frontend.
- Keep `APP_CORS_ALLOWED_ORIGINS` aligned with the production frontend domain.
- Keep `APP_COOKIES_SECURE=true` in production.
- Use HTTPS for frontend and backend.
- Rebuild and re-upload the frontend after every change to React code or frontend environment variables.
- Push backend changes to GitHub so Render can redeploy.
