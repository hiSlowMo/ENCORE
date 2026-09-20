# ENCORE Ticketing System

ENCORE is a full-stack course-design ticketing system for theaters, campus venues, and performance spaces. It demonstrates the complete loop:

`show publishing -> user ticket purchase -> electronic ticket -> on-site check-in -> admin analytics`

## Features

- User show browsing, detail pages, visual seat selection, order confirmation, mock payment, and electronic tickets.
- Redis seat locking with WebSocket live seat updates.
- Three.js stage-seat preview above the 2D seat map.
- Redis-backed group-seat invitation flow with `user/123` hosting and `friend/123` joining.
- Check-in scanner with current schedule binding and time-window validation.
- Admin show/schedule/order CRUD, refunds, force check-in, dashboard metrics, and dashboard WebSocket refresh.
- Backend-ranked Top 8 recommendations on the home page.
- Chinese/English UI switching.

## Tech Stack

- Frontend: Vue 3, Vite, TypeScript, Vue Router, Pinia, Vue I18n, Element Plus, ECharts, Three.js, SCSS.
- Backend: Spring Boot 3, Java 17, Maven, MyBatis-Plus, Sa-Token, Spring WebSocket/STOMP, Knife4j.
- Infrastructure: MySQL 8.4, Redis 7.4, Docker Compose.

## Local Development Startup

Prerequisites:

- JDK 17+
- Maven 3.9+
- Node.js 20+
- Docker Desktop

Start MySQL and Redis from the repository root:

```powershell
cd D:\ENCORE
Copy-Item .env.example .env -Force
docker compose up -d mysql redis
```

The backend uses Flyway on startup. A fresh MySQL database is created or upgraded by
`src/main/resources/db/migration/V*.sql`; do not manually replay the legacy `db/init`
scripts against a deployed database.

Start the backend:

```powershell
cd D:\ENCORE\encore-backend
mvn spring-boot:run
```

Verify backend health:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Start the frontend:

```powershell
cd D:\ENCORE\encore-frontend
npm install
npm run dev
```

Open:

- Frontend: `http://localhost:5173`
- Backend API docs: `http://localhost:8080/doc.html`
- Backend health: `http://localhost:8080/api/health`

## Optional Full Docker Compose

The default `docker-compose.yml` starts only MySQL and Redis for local development. A full containerized preview is available:

```powershell
cd D:\ENCORE
docker compose -f docker-compose.full.yml up --build
```

Then open `http://localhost:5173`. The frontend container proxies `/api` and `/ws` to the backend container.

If local dev servers are already using `8080` or `5173`, stop them first or override `ENCORE_BACKEND_PORT` / `ENCORE_FRONTEND_PORT` in `.env`.

For server deployment, prefer serving the frontend and API from the same origin through Nginx. Build the frontend with `VITE_API_BASE_URL=/` or leave it unset in production; browser requests should go to `/api/...` on the server, not to `localhost:8080` on the visitor's machine.

### HTTPS and Group Invite Copy

For a formal public deployment, use a filed domain with HTTPS so browser clipboard APIs and WebSocket security rules work consistently. During course defense, if domain filing is not ready, the system can still be demonstrated over `http://IP:port`: group-seat invitation links remain visible in a selectable read-only field, and the copy button falls back to legacy selection-based copying when the modern Clipboard API is blocked. If automatic copy is still blocked by the browser, select the invite field and copy it manually.

## Demo Accounts

| Username | Password | Role | Use |
| --- | --- | --- | --- |
| `user` | `123` | User | Purchase tickets and host group-seat invitations |
| `friend` | `123` | User | Join group-seat invitations |
| `checker` | `123` | Checker | Check in ticket codes |
| `admin` | `123` | Admin | Manage shows, schedules, orders, and dashboard |
| `sysadmin` | `123` | System admin | Admin-equivalent access |

## Verification Commands

```powershell
cd D:\ENCORE\encore-backend
mvn test

cd D:\ENCORE\encore-frontend
npm test
npm run build
```

Current verification summary is recorded in [docs/test-report.md](docs/test-report.md). Demo flow guidance is in [docs/user-manual.md](docs/user-manual.md), and screenshot evidence for the purchase flow is in [docs/demo-evidence/2026-05-17-purchase-flow.md](docs/demo-evidence/2026-05-17-purchase-flow.md).

Server database synchronization and Flyway migration steps are documented in [docs/server-database-sync.md](docs/server-database-sync.md).
