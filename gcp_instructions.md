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

- Google Cloud Storage signed URL generation is not implemented yet.
- A Pub/Sub push webhook endpoint is not implemented yet.
- A Pub/Sub publisher client is not implemented yet.

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
9. Only after the webhook route exists in code, create the Pub/Sub push subscription.

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
            - postgres_data:/var/lib/postgresql/data
            - ./init:/docker-entrypoint-initdb.d:ro

volumes:
    postgres_data:
```

Notes:

- Use `/var/lib/postgresql/data`, not `/var/lib/postgresql`.
- The `init` scripts run only on first initialization of an empty data volume.
- If you want to reuse the SQL files from this repo, copy `db/init/` onto the VM next to that Compose file.
- Use a strong password here and store the same value in Secret Manager for the backend.

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

At this stage do not create the Pub/Sub push subscription yet. There is no webhook route in the backend today.

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
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,DB_URL=jdbc:postgresql://10.142.0.2:5432/drobnyd,APP_CORS_ALLOWED_ORIGINS=https://YOUR_FRONTEND_HOST" \
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

There is currently no webhook controller in the backend, so Pub/Sub push cannot work yet.

When you add it, choose one of these patterns:

1. Preferred: deploy a dedicated private Cloud Run receiver service for Pub/Sub.
2. Acceptable: keep a webhook route on the main backend, but validate the Pub/Sub OIDC token in the application because the service is otherwise public.

If you later create a dedicated private receiver service, wire Pub/Sub like this:

```bash
export PUBSUB_PUSH_SA="pubsub-push-invoker@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts create pubsub-push-invoker \
    --display-name="Pub/Sub push invoker"

gcloud run services add-iam-policy-binding backend-service \
    --region="$REGION" \
    --member="serviceAccount:${PUBSUB_PUSH_SA}" \
    --role="roles/run.invoker"

gcloud pubsub subscriptions create backend-upload-sub \
    --topic=storage-uploads \
    --push-endpoint="https://YOUR_SERVICE_URL/api/webhooks/pubsub" \
    --push-auth-service-account="$PUBSUB_PUSH_SA"
```

If the main backend remains public, `roles/run.invoker` is not enough by itself. The application route must validate the bearer token from Pub/Sub.

If you keep using the main backend service for Pub/Sub push, create the subscription only after:

1. The webhook controller exists.
2. The route is deployed.
3. The route validates Pub/Sub OIDC tokens.
4. You know the final push URL.

## 11. Bucket signed URLs

To generate signed URLs safely from Cloud Run, the backend code should use ADC-backed Google Cloud Storage credentials and sign without a JSON key file.

Before implementing that code, decide the exact behavior:

- upload signed URL only
- download signed URL only
- both
- object path naming rules
- URL expiry window

The runtime service account should only get bucket permissions matching that behavior.

## 12. Remaining implementation tasks

1. Add a storage service that issues V4 signed URLs using Cloud Storage SDK and the Cloud Run service account.
2. Add a Pub/Sub webhook controller with OIDC token validation.
3. Add a health check endpoint that verifies DB reachability without exposing internals.
4. Add a CI build that runs `backend/gradlew.bat test` and `gcloud builds submit`.
