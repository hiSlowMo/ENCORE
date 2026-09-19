# ENCORE Frontend

Vue 3 + Vite + TypeScript frontend for ENCORE.

## Local Development

```powershell
cd D:\ENCORE\encore-frontend
Copy-Item .env.example .env -Force
npm install
npm run dev
```

Default API base URL:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Open `http://localhost:5173`.

For production builds, leave `VITE_API_BASE_URL` unset or set it to `/` when the app is served by the included Nginx config. The browser will call same-origin `/api/...` and `/ws`, which Nginx proxies to the backend container.

## Credential Policy Regression Tests

After installing dependencies, run the standalone policy tests (Node.js 20+):

```sh
npm test
```

The tests load the real TypeScript policy using the existing compiler, without a browser,
backend, database or new test dependency. They cover ISO control-character rejection,
non-control Unicode inputs, nickname boundaries and existing username/password behavior.
Matching backend cases are in `CredentialPolicyTest`, run with `mvn test` from `encore-backend`.
These checks do not claim full Unicode normalization or end-to-end registration coverage.

## Production Build

```powershell
npm run build
```

The production build output is written to `dist/`.

## Docker Preview

The frontend Docker image is normally built through the root full compose file:

```powershell
cd D:\ENCORE
docker compose -f docker-compose.full.yml up --build frontend
```

The container serves the built app through Nginx and proxies `/api` and `/ws` to the backend service.

See the root [README](../README.md) for full-stack startup and demo flow instructions.
