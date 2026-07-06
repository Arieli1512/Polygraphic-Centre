# Backend to Cloud Run: ordered deployment runbook

## Current backend state

- The backend image can be built from [backend/Dockerfile](backend/Dockerfile).
- In `prod`, the app listens on `PORT` and uses Application Default Credentials for Firebase Admin.
- Local Firebase JSON keys are no longer required in the container image.

## Current infrastructure state assumed here

- Cloud Storage bucket already exists.
- Pub/Sub topic already exists.
- The Cloud Storage service agent already has permission to publish bucket notifications to that topic.
- The Pub/Sub subscription will be created only after the backend endpoint is deployed and reachable.

## Application gaps still not implemented

- Cloud Storage signed URLs are implemented in code (V4 PUT upload and V4 GET download).
- You still need runtime IAM + bucket CORS configuration during deployment.

## Execution order

Follow the steps in this order:

1. Enable required APIs.
2. Prepare the database network path from Cloud Run to the VM.
3. Create the backend runtime service account.
4. Create or update secrets in Secret Manager.
5. Verify bucket and Pub/Sub notification wiring.
6. Build and push the backend image.
7. Deploy the backend to Cloud Run.
8. Verify backend health, DB connectivity, and Firebase-backed login.
9. Create Pub/Sub push subscriptions after backend deploy and health verification.

## 1. Enable required APIs

```bash
export PROJECT_ID="YOUR_PROJECT_ID"
export REGION="us-east1"
export BACKEND_SA="backend-runtime@${PROJECT_ID}.iam.gserviceaccount.com"
export BUCKET_NAME="YOUR_BUCKET"
export TOPIC_NAME="storage-uploads"

gcloud services enable \
    run.googleapis.com \
    cloudbuild.googleapis.com \
    artifactregistry.googleapis.com \
    secretmanager.googleapis.com \
    vpcaccess.googleapis.com \
    iam.googleapis.com \
    iamcredentials.googleapis.com \
    pubsub.googleapis.com \
    storage.googleapis.com
```

## 2. Run PostgreSQL in Docker on the VM and keep it private

Your VM currently has:

- Internal IP: `10.142.0.2`
- External IP: `34.74.134.84`
- Region target for Cloud Run: `us-east1`

Do not connect Cloud Run to PostgreSQL over the VM external IP. Run PostgreSQL in a Docker container on the VM, publish it on the VM host port `5432`, and reach it from Cloud Run over the VM internal IP through a Serverless VPC Access connector.

Important repo note:

- The existing repo-level [docker-compose.yml](docker-compose.yml) is local-development oriented. It publishes `5433:5432` and uses default local credentials.
- For the VM deployment, use a VM-specific Compose file that publishes `5432:5432` and uses your real database credentials.

### 2.1. Create a VM-specific Compose setup

On the VM, create a working directory such as `/opt/polygraphic-centre-db` and place a Compose file there.

Example `compose.yaml` for the VM:

```yaml
services:
    postgres:
        image: postgres:18
        container_name: polygraphic-centre-postgres
        restart: unless-stopped
        environment:
            POSTGRES_DB: drobnyd
            POSTGRES_USER: drobnyd
            POSTGRES_PASSWORD: CHANGE_ME_DB_PASSWORD
        ports:
            - "5432:5432"
        volumes:
            - postgres_data:/var/lib/postgresql
            - ./init:/docker-entrypoint-initdb.d:ro

volumes:
    postgres_data:
```

Notes:

- For `postgres:18`, use `/var/lib/postgresql` as the volume mount point.
- The `init` scripts run only on first initialization of an empty data volume.
- If you want to reuse the SQL files from this repo, copy `db/init/` onto the VM next to that Compose file.
- Use a strong password here and store the same value in Secret Manager for the backend.

If you previously used `/var/lib/postgresql/data` with an older image or config and now see startup errors, do one of these:

1. No data to keep (fastest):
    - Stop and remove the container plus volume.
    - Update Compose to mount `/var/lib/postgresql`.
    - Start again to initialize a fresh cluster.

2. Data must be preserved:
    - Start the old setup temporarily and take a logical backup (`pg_dump` or `pg_dumpall`).
    - Recreate the container with `postgres:18` and mount `/var/lib/postgresql`.
    - Restore the dump into the new cluster.

For production-like environments, use logical backup/restore or a proper major-version upgrade path (`pg_upgrade`) rather than forcing the old data directory into a new major image.

### 2.2. Start the container on the VM

From the VM directory containing the Compose file:

```bash
docker compose up -d
docker compose ps
```

Recommended checks:

```bash
docker logs polygraphic-centre-postgres --tail=100
sudo ss -ltnp | grep 5432
```

You want the host to be listening on `0.0.0.0:5432` or the VM private interface on `5432`.

### 2.3. Restrict network access to the database

You do not need the database exposed publicly just because the VM has an external IP.

Keep access private with these controls:

1. Use the VM internal IP from Cloud Run.
2. Open GCP firewall ingress only from the Serverless VPC Access connector CIDR.
3. Do not create a broad firewall rule for `0.0.0.0/0` to port `5432`.
4. Optionally add OS-level firewall rules on the VM as a second layer.

Example connector setup:

```bash
gcloud compute networks vpc-access connectors create cr-backend-connector \
    --region=us-east1 \
    --network=default \
    --range=10.8.0.0/28

gcloud compute firewall-rules create allow-postgres-from-cloud-run \
    --network=default \
    --direction=INGRESS \
    --action=ALLOW \
    --rules=tcp:5432 \
    --source-ranges=10.8.0.0/28 \
    --target-tags=postgres-vm
```

Apply the `postgres-vm` network tag to the VM, then point the backend to:

```text
jdbc:postgresql://10.142.0.2:5432/drobnyd
```

### 2.4. Match backend secrets to the container config

The backend deployment must use the same DB settings configured in the VM container:

- `DB_URL=jdbc:postgresql://10.142.0.2:5432/drobnyd`
- `DB_USERNAME=drobnyd`
- `DB_PASSWORD` must match `POSTGRES_PASSWORD` from the VM Compose file

If you keep `POSTGRES_USER=postgres` instead, then `DB_USERNAME` must also be `postgres`.

### 2.5. Persistence and lifecycle

The named Docker volume keeps database data across container restarts.

Be aware of these operational rules:

1. Changing `POSTGRES_PASSWORD` in Compose does not automatically rotate the password inside an already-initialized database volume.
2. Changing files in `init/` does not re-run initialization against an existing volume.
3. If you need to reinitialize from scratch, remove the volume explicitly and accept data loss.

For production-like operation on the VM, prefer backup and restore over volume deletion.

## 3. Use a dedicated Cloud Run runtime service account

```bash
gcloud iam service-accounts create backend-runtime \
    --display-name="Backend Cloud Run runtime"
```

Grant only the roles the backend needs today:

```bash
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${BACKEND_SA}" \
    --role="roles/secretmanager.secretAccessor"
```

Firebase note:

- For the current code path, which only verifies Firebase ID tokens, no additional Firebase-specific IAM role is required when Cloud Run runs in the same GCP project as Firebase.
- The Cloud Run runtime service account identity is used through Application Default Credentials.

For signed URLs later, grant the runtime account permission to sign as itself:

```bash
gcloud iam service-accounts add-iam-policy-binding "$BACKEND_SA" \
    --member="serviceAccount:${BACKEND_SA}" \
    --role="roles/iam.serviceAccountTokenCreator"
```

For Cloud Storage access later, grant bucket-scoped roles, not project-wide roles. Use the narrowest role that matches the final behavior:

- Upload-only signed URLs: `roles/storage.objectCreator`
- Download-only signed URLs: `roles/storage.objectViewer`
- Both upload and download: `roles/storage.objectAdmin`

Example:

```bash
gcloud storage buckets add-iam-policy-binding gs://YOUR_BUCKET \
    --member="serviceAccount:${BACKEND_SA}" \
    --role="roles/storage.objectAdmin"
```

## 4. Put secrets in Secret Manager

Never pass database passwords or JWT secrets inline in `gcloud run deploy`.

Create secrets:

```bash
printf 'drobnyd' | gcloud secrets create db-username --data-file=-
printf 'CHANGE_ME' | gcloud secrets create db-password --data-file=-
printf 'CHANGE_ME_LONG_RANDOM_VALUE' | gcloud secrets create app-auth-jwt-secret --data-file=-
```

If the secrets already exist, add new versions instead:

```bash
printf 'new-value' | gcloud secrets versions add db-password --data-file=-
```

## 5. Verify bucket and Pub/Sub notification wiring

Because the bucket and topic already exist, verify these items before deployment:

1. The bucket is in the intended region or multi-region for your workload.
2. The Pub/Sub topic exists.
3. The Cloud Storage service agent can publish to the topic.
4. The bucket notification exists and targets that topic.

If you still need to grant the publish permission explicitly, the member is typically the project-specific Cloud Storage service agent:

```bash
gcloud pubsub topics add-iam-policy-binding "$TOPIC_NAME" \
    --member="serviceAccount:service-PROJECT_NUMBER@gs-project-accounts.iam.gserviceaccount.com" \
    --role="roles/pubsub.publisher"
```

If the bucket notification itself does not exist yet, create it:

```bash
gcloud storage buckets notifications create "gs://${BUCKET_NAME}" \
    --topic="$TOPIC_NAME" \
    --event-types=OBJECT_FINALIZE
```

For object naming, prefer client-scoped prefixes. The backend now uses:

```text
clients/{clientId}/orders/{yyyy}/{mm}/{dd}/{uuid}-{sanitizedFileName}.pdf
```

This is better than a flat bucket namespace because it helps with lifecycle rules, audits, and operational queries.

## 6. Build and push the image

From the repo root:

```bash
gcloud artifacts repositories create polygraphic-centre \
    --repository-format=docker \
    --location="$REGION"

gcloud builds submit backend \
    --tag "$REGION-docker.pkg.dev/$PROJECT_ID/polygraphic-centre/backend:latest"
```

`gcloud builds submit` is preferable here because it avoids depending on a local Docker daemon.

## 7. Deploy Cloud Run

Set non-secret values as env vars and inject secrets from Secret Manager:

```bash
gcloud run deploy backend-service \
    --image="$REGION-docker.pkg.dev/$PROJECT_ID/polygraphic-centre/backend:latest" \
    --region="$REGION" \
    --platform=managed \
    --service-account="$BACKEND_SA" \
    --vpc-connector=cr-backend-connector \
    --vpc-egress=private-ranges-only \
    --allow-unauthenticated \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,DB_URL=jdbc:postgresql://10.142.0.2:5432/drobnyd,FIREBASE_PROJECT_ID=YOUR_FIREBASE_PROJECT_ID,APP_CORS_ALLOWED_ORIGINS=https://YOUR_FRONTEND_HOST" \
    --set-secrets="DB_USERNAME=db-username:latest,DB_PASSWORD=db-password:latest,APP_AUTH_JWT_SECRET=app-auth-jwt-secret:latest"
```

Notes:

- `--allow-unauthenticated` is usually required if the browser frontend talks directly to this backend.
- If you use Firebase Hosting or another separate frontend origin, keep `APP_CORS_ALLOWED_ORIGINS` exact.
- Because the frontend is cross-site relative to Cloud Run, production cookies should stay `Secure` and `SameSite=None`, which the `prod` profile already expects.

## 8. Verify the deployment before adding Pub/Sub push

Check these in order:

1. Cloud Run revision starts successfully.
2. Backend can connect to PostgreSQL over the VM internal IP.
3. Secret Manager values are being injected correctly.
4. Firebase-backed login works end to end.
5. CORS and secure cookies work from the real frontend origin.

Recommended quick checks:

```bash
gcloud run services describe backend-service --region="$REGION"
gcloud run services logs read backend-service --region="$REGION" --limit=100
```

Only after these checks pass should you add external event delivery.

## 9. Firebase Admin on Cloud Run

Do not mount or copy `firebase-service-account.json` into Cloud Run.

The backend now uses Application Default Credentials in `prod`. That means:

- Cloud Run runtime service account identity is the credential source.
- No static JSON key is required for Firebase token verification.
- Local development can still use `firebase.admin.service-account-resource=firebase-service-account.json` with ADC disabled.

## 10. Pub/Sub to backend

The backend now includes Pub/Sub push webhook routes:

- `/api/webhooks/pubsub/storage-uploads`
- `/api/webhooks/pubsub/order-events`

The backend also publishes domain events to Pub/Sub topics:

- `order-events`
- `notification-events`

Recommended wiring:

1. Keep your existing `storage-uploads` topic for GCS object finalize events.
2. Create two additional topics: `order-events` and `notification-events`.
3. Push `storage-uploads` to `/api/webhooks/pubsub/storage-uploads`.
4. Push `order-events` to `/api/webhooks/pubsub/order-events`.
5. Configure retries and Dead Letter Topic for both push subscriptions.

Example (main backend service as push target with OIDC):

```bash
export PUBSUB_PUSH_SA="pubsub-push-invoker@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts create pubsub-push-invoker \
    --display-name="Pub/Sub push invoker"

gcloud run services add-iam-policy-binding backend-service \
    --region="$REGION" \
    --member="serviceAccount:${PUBSUB_PUSH_SA}" \
    --role="roles/run.invoker"

gcloud pubsub topics create order-events
gcloud pubsub topics create notification-events

gcloud pubsub topics create storage-uploads-dlq
gcloud pubsub topics create order-events-dlq

gcloud pubsub subscriptions create backend-storage-uploads-sub \
    --topic=storage-uploads \
    --push-endpoint="https://YOUR_SERVICE_URL/api/webhooks/pubsub/storage-uploads" \
    --push-auth-service-account="$PUBSUB_PUSH_SA" \
    --dead-letter-topic=storage-uploads-dlq \
    --max-delivery-attempts=10 \
    --min-retry-delay=10s \
    --max-retry-delay=600s

gcloud pubsub subscriptions create backend-order-events-sub \
    --topic=order-events \
    --push-endpoint="https://YOUR_SERVICE_URL/api/webhooks/pubsub/order-events" \
    --push-auth-service-account="$PUBSUB_PUSH_SA" \
    --dead-letter-topic=order-events-dlq \
    --max-delivery-attempts=10 \
    --min-retry-delay=10s \
    --max-retry-delay=600s
```

If the main backend remains public, `roles/run.invoker` is not enough by itself. Keep OIDC validation enabled in app config (`pubsub.require-oidc=true`) and set `pubsub.webhook-audience` to your Cloud Run URL.

Create subscriptions only after:

1. The webhook controller exists.
2. The route is deployed.
3. The route validates Pub/Sub OIDC tokens.
4. You know the final push URL.

## 11. Bucket signed URLs

Signed URLs are generated by the backend using Cloud Storage V4 signatures.

- Upload: `PUT` signed URL returned by `POST /api/v1/files/upload-requests`
- Download: `GET` signed URL returned by `POST /api/v1/employee/orders/{orderId}/download-link`
- Object key format: `clients/{clientId}/orders/{yyyy}/{mm}/{dd}/{uuid}-{sanitizedFileName}`

Runtime requirements:

1. Cloud Run runtime account can sign blobs (`roles/iam.serviceAccountTokenCreator` on itself).
2. Cloud Run runtime account has bucket object permissions (viewer/creator/admin according to needs).
3. Bucket CORS allows browser `PUT` from frontend origin.

Example CORS file (`cors.json`):

```json
[
    {
        "origin": ["https://YOUR_FRONTEND_HOST"],
        "method": ["PUT", "GET", "HEAD", "OPTIONS"],
        "responseHeader": ["Content-Type", "x-goog-meta-request-id", "x-goog-meta-client-id", "x-goog-resumable"],
        "maxAgeSeconds": 3600
    }
]
```

Apply it:

```bash
gcloud storage buckets update "gs://${BUCKET_NAME}" --cors-file=cors.json
```

## 12. Remaining implementation tasks

1. Add a health check endpoint that verifies DB reachability without exposing internals.
2. Add a CI build that runs `backend/gradlew.bat test` and `gcloud builds submit`.

## 13. Actions for your project (`drobnyd-b1d45`)

Use this section when you are ready to deploy.

### 13.1. Set project-specific variables

```bash
export PROJECT_ID="drobnyd-b1d45"
export REGION="us-east1"
export BUCKET_NAME="my-free-app-bucket-drobnyd-b1d45"
export BACKEND_SERVICE_URL="https://YOUR_BACKEND_CLOUD_RUN_URL"
export PUBSUB_PUSH_SA="pubsub-push-invoker@${PROJECT_ID}.iam.gserviceaccount.com"
```

### 13.2. Ensure required Pub/Sub topics exist

You already have `storage-uploads`.

Create the remaining topics:

```bash
gcloud pubsub topics create order-events --project="$PROJECT_ID"
gcloud pubsub topics create notification-events --project="$PROJECT_ID"
gcloud pubsub topics create storage-uploads-dlq --project="$PROJECT_ID"
gcloud pubsub topics create order-events-dlq --project="$PROJECT_ID"
```

### 13.3. Ensure GCS bucket notification exists for your bucket

```bash
gcloud storage buckets notifications create "gs://${BUCKET_NAME}" \
    --topic="storage-uploads" \
    --event-types=OBJECT_FINALIZE \
    --project="$PROJECT_ID"
```

### 13.4. Create Pub/Sub push identity and permissions

```bash
gcloud iam service-accounts create pubsub-push-invoker \
    --display-name="Pub/Sub push invoker" \
    --project="$PROJECT_ID"

gcloud run services add-iam-policy-binding backend-service \
    --region="$REGION" \
    --member="serviceAccount:${PUBSUB_PUSH_SA}" \
    --role="roles/run.invoker" \
    --project="$PROJECT_ID"

gcloud iam service-accounts add-iam-policy-binding "backend-runtime@${PROJECT_ID}.iam.gserviceaccount.com" \
    --member="serviceAccount:backend-runtime@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/iam.serviceAccountTokenCreator" \
    --project="$PROJECT_ID"

gcloud storage buckets add-iam-policy-binding "gs://${BUCKET_NAME}" \
    --member="serviceAccount:backend-runtime@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/storage.objectAdmin" \
    --project="$PROJECT_ID"
```

### 13.5. Create push subscriptions with retry + DLQ

```bash
gcloud pubsub subscriptions create backend-storage-uploads-sub \
    --topic=storage-uploads \
    --push-endpoint="${BACKEND_SERVICE_URL}/api/webhooks/pubsub/storage-uploads" \
    --push-auth-service-account="$PUBSUB_PUSH_SA" \
    --dead-letter-topic=storage-uploads-dlq \
    --max-delivery-attempts=10 \
    --min-retry-delay=10s \
    --max-retry-delay=600s \
    --project="$PROJECT_ID"

gcloud pubsub subscriptions create backend-order-events-sub \
    --topic=order-events \
    --push-endpoint="${BACKEND_SERVICE_URL}/api/webhooks/pubsub/order-events" \
    --push-auth-service-account="$PUBSUB_PUSH_SA" \
    --dead-letter-topic=order-events-dlq \
    --max-delivery-attempts=10 \
    --min-retry-delay=10s \
    --max-retry-delay=600s \
    --project="$PROJECT_ID"
```

### 13.6. Deploy env vars for section 7

Set these on Cloud Run service:

- `FIREBASE_PROJECT_ID=drobnyd-b1d45`
- `PUBSUB_PROJECT_ID=drobnyd-b1d45`
- `PUBSUB_ENABLED=true`
- `PUBSUB_ORDER_EVENTS_TOPIC_NAME=order-events`
- `PUBSUB_NOTIFICATION_EVENTS_TOPIC_NAME=notification-events`
- `PUBSUB_STORAGE_UPLOADS_TOPIC_NAME=storage-uploads`
- `PUBSUB_REQUIRE_OIDC=true`
- `PUBSUB_WEBHOOK_AUDIENCE=${BACKEND_SERVICE_URL}`
- `GCS_BUCKET_NAME=my-free-app-bucket-drobnyd-b1d45`
- `GCS_UPLOAD_ROOT_PREFIX=clients`
- `GCS_SIGNING_SERVICE_ACCOUNT_EMAIL=backend-runtime@drobnyd-b1d45.iam.gserviceaccount.com`
- `MAIL_ENABLED=true` (only when SMTP credentials are configured)

For SMTP, set also:

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM_ADDRESS`

## 14. Frontend deployment on Firebase Hosting (recommended)

This approach is the simplest integration for your current stack and free-tier goals:

1. Frontend static files are served from Firebase Hosting.
2. Requests to `/api/**` are rewritten to Cloud Run `backend-service`.
3. Browser calls stay same-origin from frontend perspective (`/api/...`), which reduces CORS and cookie complexity.

### 14.1. Files already prepared in this repo

- [frontend/firebase.json](frontend/firebase.json): serves `dist/`
- [frontend/firebase.json](frontend/firebase.json): rewrites `/api/**` to Cloud Run service `backend-service` in `us-east1`
- [frontend/firebase.json](frontend/firebase.json): rewrites all other routes to `/index.html` (SPA fallback)
- [frontend/.firebaserc.example](frontend/.firebaserc.example): template for Firebase project binding
- [frontend/src/api/api.ts](frontend/src/api/api.ts): defaults API base URL to `/api` if env var is not set

If your Cloud Run backend service name or region differs, update [frontend/firebase.json](frontend/firebase.json).

### 14.2. One-time Firebase setup for Hosting

From `frontend/`:

```bash
firebase login
firebase use YOUR_PROJECT_ID
```

Or copy [frontend/.firebaserc.example](frontend/.firebaserc.example) to `.firebaserc` and set your project id.

Enable Hosting in Firebase console if not yet enabled.

### 14.3. Build and deploy frontend

From `frontend/`:

```bash
npm ci
npm run build
firebase deploy --only hosting
```

On this Windows setup, prefer `npm.cmd` instead of `npm` if PowerShell blocks `npm.ps1`:

```powershell
npm.cmd ci
npm.cmd run build
firebase deploy --only hosting
```

### 14.4. Backend settings when using Hosting rewrites

Even with rewrites, keep backend origin restrictions explicit.

Set on Cloud Run:

- `APP_CORS_ALLOWED_ORIGINS=https://YOUR_PROJECT_ID.web.app,https://YOUR_PROJECT_ID.firebaseapp.com`
- `APP_AUTH_COOKIE_SECURE=true`
- `APP_AUTH_COOKIE_SAME_SITE=None`

Because Firebase Hosting forwards requests to Cloud Run, your backend still receives browser traffic through HTTPS and cookies remain valid.

### 14.5. Frontend env values (production)

For production build, use Firebase config from your Firebase project and keep API base URL as relative path:

```text
VITE_API_BASE_URL=/api
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
VITE_FIREBASE_MEASUREMENT_ID=...
```

The repo [frontend/.env.example](frontend/.env.example) remains local-dev oriented (`http://localhost:8080/api`).

### 14.6. Deploy order with backend

Use this order to avoid broken rewrites:

1. Deploy backend Cloud Run first.
2. Confirm backend URL works and healthy.
3. Deploy Firebase Hosting.
4. Verify frontend calls `/api/auth/csrf` and `/api/auth/session` through Hosting domain.

### 14.7. Verification checklist after frontend deploy

1. Open `https://YOUR_PROJECT_ID.web.app`.
2. In browser DevTools Network, confirm API requests go to `https://YOUR_PROJECT_ID.web.app/api/...`.
3. Confirm login works and cookies are set.
4. Confirm hard refresh on non-root route (for example `/signin`) works (SPA rewrite).
5. Confirm no CORS errors in browser console.
