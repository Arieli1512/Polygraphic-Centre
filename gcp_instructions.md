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

## 2. Run PostgreSQL on a hardened Debian VM (Docker, private only)

This section defines a safer baseline for running PostgreSQL in Docker on Compute Engine and connecting from Cloud Run.

### 2.1. Recommended network model

1. VM has no external IP.
2. Cloud Run reaches PostgreSQL through Serverless VPC Access connector.
3. PostgreSQL only listens on VM internal network.
4. Firewall allows port `5432` only from connector CIDR.

If you need shell access to a VM without external IP, use IAP SSH from Cloud Shell or gcloud (`--tunnel-through-iap`).

### 2.2. Use static internal IP for stability

Do not depend on ephemeral internal VM address for database connection strings.

Reserve a static internal IP in the subnet:

```bash
export PROJECT_ID="YOUR_PROJECT_ID"
export REGION="us-east1"
export ZONE="us-east1-b"
export NETWORK="default"
export SUBNET="default"

gcloud compute addresses create pg-internal-ip \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --subnet="$SUBNET"
```

Read the reserved IP:

```bash
gcloud compute addresses describe pg-internal-ip \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --format="value(address)"
```

### 2.3. Create or recreate VM without external IP (free-tier-oriented + hardened)

Cost-oriented defaults:

1. Prefer `e2-micro` in a free-tier eligible US region (`us-east1`, `us-west1`, or `us-central1`).
2. Use `pd-standard` (not SSD) and keep disk minimal (for example `20GB` if enough for your workload).
3. Keep no external IP and use IAP for admin access.
4. Enable Shielded VM and OS Login.

```bash
export VM_NAME="postgres-db-vm"
export STATIC_IP="REPLACE_WITH_RESERVED_INTERNAL_IP"

gcloud compute instances create "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --machine-type=e2-micro \
    --network-interface=network="$NETWORK",subnet="$SUBNET",private-network-ip="$STATIC_IP",no-address \
    --image-family=debian-12 \
    --image-project=debian-cloud \
    --boot-disk-type=pd-standard \
    --boot-disk-size=20GB \
    --tags=postgres-vm \
    --shielded-secure-boot \
    --shielded-vtpm \
    --shielded-integrity-monitoring \
    --metadata=enable-oslogin=TRUE,block-project-ssh-keys=TRUE
```

If the VM already exists and has an external IP, remove it:

```bash
gcloud compute instances delete-access-config "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --access-config-name="external-nat"
```

### 2.4. Configure outbound internet for private VM (required before apt install)

Without external IP and without Cloud NAT, `apt-get` and Docker package downloads will fail.

Create Cloud Router + Cloud NAT once per VPC/region:

```bash
gcloud compute routers create cr-nat-router \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --network="$NETWORK"

gcloud compute routers nats create cr-nat-config \
    --project="$PROJECT_ID" \
    --router=cr-nat-router \
    --router-region="$REGION" \
    --nat-all-subnet-ip-ranges \
    --auto-allocate-nat-external-ips
```

Verify NAT:

```bash
gcloud compute routers nats describe cr-nat-config \
    --project="$PROJECT_ID" \
    --router=cr-nat-router \
    --router-region="$REGION"
```

### 2.5. Install Docker and Compose plugin on Debian (timeout-resistant)

SSH into the VM (from Cloud Shell, optionally with IAP):

```bash
gcloud compute ssh "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --tunnel-through-iap
```

On the VM:

```bash
# If a stale Cloud SDK apt source exists and fails, remove it (not needed for runtime VM).
sudo rm -f /etc/apt/sources.list.d/google-cloud-sdk.list

# Force IPv4 for apt to avoid IPv6 reachability issues on some networks.
echo 'Acquire::ForceIPv4 "true";' | sudo tee /etc/apt/apt.conf.d/99force-ipv4 > /dev/null
echo 'Acquire::Retries "5";' | sudo tee /etc/apt/apt.conf.d/80retries > /dev/null

sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg git postgresql-client

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker

# Optional: run docker without sudo in new sessions.
sudo usermod -aG docker "$USER"

docker --version
docker compose version
psql --version
```

If `apt-get update` still times out, validate outbound egress from VM:

```bash
curl -I https://deb.debian.org
curl -I https://download.docker.com
```

### 2.6. Host hardening baseline (Debian)

On the VM:

```bash
sudo apt-get install -y unattended-upgrades fail2ban
sudo dpkg-reconfigure -plow unattended-upgrades
```

Recommended:

1. Keep SSH only through IAP or trusted admin CIDRs.
2. Keep `block-project-ssh-keys=TRUE` and use OS Login IAM for access control.
3. Keep only required ingress open (here only `5432` from connector CIDR).
4. Do not run unrelated workloads on this VM.
5. Rotate database credentials after incidents.

### 2.7. Clone repository and prepare Docker Compose on VM

On VM:

```bash
cd /opt
sudo mkdir -p polygraphic-centre
sudo chown "$USER":"$USER" polygraphic-centre

git clone https://github.com/YOUR_ORG/Polygraphic-Centre.git /opt/polygraphic-centre
cd /opt/polygraphic-centre
```

For database runtime, keep a VM-specific compose file instead of local-dev root compose.

Create `/opt/polygraphic-centre/deploy/compose.db.vm.yaml`:

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
            - ../db/init:/docker-entrypoint-initdb.d:ro

volumes:
    postgres_data:
```

### 2.8. Start and verify PostgreSQL container

```bash
cd /opt/polygraphic-centre
docker compose -f deploy/compose.db.vm.yaml up -d
docker compose -f deploy/compose.db.vm.yaml ps
docker logs polygraphic-centre-postgres --tail=100
sudo ss -ltnp | grep 5432

# Validate local DB connectivity from VM using psql client.
PGPASSWORD='CHANGE_ME_DB_PASSWORD' psql -h 127.0.0.1 -p 5432 -U drobnyd -d drobnyd -c 'select 1;'
```

### 2.9. Restrict firewall to connector CIDR only

Cost note:

- Serverless VPC Access connector can be a meaningful monthly cost driver even when VM sizing is free-tier-friendly.
- Monitor billing after enabling the connector and keep `--min-instances=0` on Cloud Run.

Create connector (if missing):

```bash
gcloud compute networks vpc-access connectors create cr-backend-connector \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --network="$NETWORK" \
    --range=10.8.0.0/28
```

Allow PostgreSQL only from connector range:

```bash
gcloud compute firewall-rules create allow-postgres-from-cloud-run \
    --project="$PROJECT_ID" \
    --network="$NETWORK" \
    --direction=INGRESS \
    --action=ALLOW \
    --rules=tcp:5432 \
    --source-ranges=10.8.0.0/28 \
    --target-tags=postgres-vm
```

Do not create broad `0.0.0.0/0` ingress on `5432`.

### 2.10. Cloud Run settings after VM changes

If VM uses reserved static internal IP, keep backend DB URL stable:

```text
DB_URL=jdbc:postgresql://STATIC_INTERNAL_IP:5432/drobnyd
```

If VM uses ephemeral internal IP and VM is recreated, update `DB_URL` in Cloud Run each time address changes.

Cloud Run VPC connector settings remain the same as long as:

1. same VPC network is used,
2. same connector region is used,
3. firewall allows connector CIDR to VM.

### 2.11. Match backend secrets to DB container

Cloud Run must use the same credentials configured in PostgreSQL container:

1. `DB_USERNAME` equals `POSTGRES_USER`.
2. `DB_PASSWORD` equals `POSTGRES_PASSWORD` (from Secret Manager).
3. `DB_URL` points to VM static internal IP and port `5432`.

### 2.12. Operations and recovery

1. Use regular logical backups (`pg_dump`/`pg_dumpall`) and copy backups off-VM.
2. Test restore procedure periodically.
3. Rebuild VM from clean image after suspected compromise.
4. Rotate DB password and `app-auth-jwt-secret` after incidents.

If migrating from older Postgres volume layouts and `postgres:18` fails startup:

1. If no data needed: recreate volume with new mount layout.
2. If data needed: perform logical dump from old setup, then restore into new clean container.

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
    --min-instances=0 \
    --max-instances=1 \
    --vpc-connector=cr-backend-connector \
    --vpc-egress=private-ranges-only \
    --allow-unauthenticated \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,DB_URL=jdbc:postgresql://10.142.0.2:5432/drobnyd,FIREBASE_PROJECT_ID=${PROJECT_ID},APP_CORS_ALLOWED_ORIGINS=https://YOUR_FRONTEND_HOST" \
    --set-secrets="DB_USERNAME=db-username:latest,DB_PASSWORD=db-password:latest,APP_AUTH_JWT_SECRET=app-auth-jwt-secret:latest"
```

Notes:

- `--allow-unauthenticated` is usually required if the browser frontend talks directly to this backend.
- If you use Firebase Hosting or another separate frontend origin, keep `APP_CORS_ALLOWED_ORIGINS` exact.
- If `APP_CORS_ALLOWED_ORIGINS` must contain multiple origins (comma-separated), use custom dict delimiter syntax in `gcloud run services update`:

```bash
gcloud run services update backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --update-env-vars="^@^APP_CORS_ALLOWED_ORIGINS=https://YOUR_PROJECT_ID.web.app,https://YOUR_PROJECT_ID.firebaseapp.com"
```

- Do not leave `FIREBASE_PROJECT_ID` as a placeholder value. Use the real Firebase project id (usually `${PROJECT_ID}`).
- Because the frontend is cross-site relative to Cloud Run, production cookies should stay `Secure` and `SameSite=None`, which the `prod` profile already expects.
- To avoid session consistency issues while you rely on instance-local state, keep Cloud Run at `--max-instances=1`.
- Use `--min-instances=0` to stay scale-to-zero and reduce idle costs on free-tier-like usage.

For an already deployed service, you can apply scaling limits without redeploying image:

```bash
gcloud run services update backend-service \
    --region="$REGION" \
    --min-instances=0 \
    --max-instances=1
```

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

# Verify active runtime env values (especially CORS and Firebase project id)
gcloud run services describe backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --format="yaml(spec.template.spec.containers[0].env)"

# Verify startup CORS configuration line
gcloud run services logs read backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --limit=200 | grep -i "CORS configured for origins"
```

If login fails with Firebase audience mismatch (`incorrect "aud" claim`), verify:

1. `FIREBASE_PROJECT_ID` equals the real Firebase project id.
2. Frontend Firebase config points to the same project id.
3. No stale placeholder value like `YOUR_FIREBASE_PROJECT_ID` remains in Cloud Run env vars.

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

Runtime verification for Pub/Sub setup:

```bash
gcloud pubsub topics list --project="$PROJECT_ID"
gcloud pubsub subscriptions list --project="$PROJECT_ID"
```

## 11. Bucket signed URLs

Signed URLs are generated by the backend using Cloud Storage V4 signatures.

- Upload: `PUT` signed URL returned by `POST /api/v1/files/upload-requests`
- Download: `GET` signed URL returned by `POST /api/v1/employee/orders/{orderId}/download-link`
- Object key format: `clients/{clientId}/orders/{yyyy}/{mm}/{dd}/{uuid}-{sanitizedFileName}`

Runtime requirements:

1. Cloud Run runtime account can sign blobs (`roles/iam.serviceAccountTokenCreator` on itself).
2. Cloud Run runtime account has bucket object permissions (viewer/creator/admin according to needs).
3. Bucket CORS allows browser `PUT` from frontend origin.
4. Cloud Run env includes `GCS_BUCKET_NAME` (required), `GCS_PROJECT_ID` (recommended), and `GCS_SIGNING_SERVICE_ACCOUNT_EMAIL` (recommended fallback for IAM signing).

If `GCS_BUCKET_NAME` is missing, upload URL generation fails with:

```text
java.lang.IllegalStateException: gcs.bucket-name must be configured for signed URL generation
```

Example CORS file (`cors.json`):

```json
[
    {
        "origin": [
            "https://YOUR_PROJECT_ID.web.app",
            "https://YOUR_PROJECT_ID.firebaseapp.com",
            "http://localhost:5173",
            "http://127.0.0.1:5173"
        ],
        "method": ["PUT", "GET", "HEAD", "OPTIONS"],
        "responseHeader": ["Content-Type", "x-goog-meta-request-id", "x-goog-meta-client-id", "x-goog-resumable", "x-goog-request-id"],
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
- `GCS_PROJECT_ID=drobnyd-b1d45`
- `GCS_BUCKET_NAME=my-free-app-bucket-drobnyd-b1d45`
- `GCS_UPLOAD_ROOT_PREFIX=clients`
- `GCS_SIGNING_SERVICE_ACCOUNT_EMAIL=backend-runtime@drobnyd-b1d45.iam.gserviceaccount.com`
- `APP_AUTH_COOKIE_DOMAIN=.firebaseapp.com` (or `.web.app` for Firebase Hosting rewrite)
- `MAIL_ENABLED=true` (only when SMTP credentials are configured)

For SMTP, set also:

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM_ADDRESS`

For existing deployments, update critical auth/CORS vars with delimiter-safe syntax:

```bash
gcloud run services update backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --update-env-vars="^@^SPRING_PROFILES_ACTIVE=prod@FIREBASE_PROJECT_ID=${PROJECT_ID}@APP_CORS_ALLOWED_ORIGINS=https://${PROJECT_ID}.web.app,https://${PROJECT_ID}.firebaseapp.com@APP_AUTH_COOKIE_SECURE=true@APP_AUTH_COOKIE_SAME_SITE=None"
```

Then verify applied values:

```bash
gcloud run services describe backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --format="yaml(spec.template.spec.containers[0].env)"
```

Quick sanity checks for integration flags:

1. `PUBSUB_ENABLED=true` and `PUBSUB_PROJECT_ID` set to your project.
2. `MAIL_ENABLED=true` only when SMTP settings are present and valid.
3. `GCS_BUCKET_NAME` is set (required for signed URLs).

### 13.7. Fix "Pub/Sub disabled" and enable end-to-end notifications

If logs show:

```text
Pub/Sub disabled - skipping publish for topic=order-events
```

then backend runtime config is incomplete. Apply all integration flags in one update:

```bash
gcloud run services update backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --update-env-vars="^@^SPRING_PROFILES_ACTIVE=prod@FIREBASE_PROJECT_ID=${PROJECT_ID}@PUBSUB_ENABLED=true@PUBSUB_PROJECT_ID=${PROJECT_ID}@PUBSUB_ORDER_EVENTS_TOPIC_NAME=order-events@PUBSUB_NOTIFICATION_EVENTS_TOPIC_NAME=notification-events@PUBSUB_STORAGE_UPLOADS_TOPIC_NAME=storage-uploads@PUBSUB_REQUIRE_OIDC=true@PUBSUB_WEBHOOK_AUDIENCE=${BACKEND_SERVICE_URL}@GCS_PROJECT_ID=${PROJECT_ID}@GCS_BUCKET_NAME=${BUCKET_NAME}@GCS_UPLOAD_ROOT_PREFIX=clients@GCS_SIGNING_SERVICE_ACCOUNT_EMAIL=backend-runtime@${PROJECT_ID}.iam.gserviceaccount.com"
```

Enable email dispatch only when SMTP credentials are configured:

```bash
gcloud run services update backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --update-env-vars="MAIL_ENABLED=true,MAIL_FROM_ADDRESS=YOUR_FROM_ADDRESS,MAIL_HOST=YOUR_SMTP_HOST,MAIL_PORT=587,MAIL_USERNAME=YOUR_SMTP_USERNAME" \
    --set-secrets="MAIL_PASSWORD=mail-password:latest"
```

If you do not yet have SMTP credentials, keep:

```text
MAIL_ENABLED=false
```

to test Pub/Sub and webhook flow without real mail sending.

Verify active values on Cloud Run revision:

```bash
gcloud run services describe backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --format="yaml(spec.template.spec.containers[0].env)"
```

Verify startup logs include effective Pub/Sub config:

```bash
gcloud run services logs read backend-service \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --limit=200 | grep -i "Pub/Sub configuration"
```

Expected startup log pattern:

```text
Pub/Sub configuration: enabled=true projectIdPresent=true ...
```

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
- `APP_AUTH_COOKIE_DOMAIN=YOUR_PROJECT_ID.web.app` when users access `web.app`
- `APP_AUTH_COOKIE_DOMAIN=YOUR_PROJECT_ID.firebaseapp.com` when users access `firebaseapp.com`

**Critical cookie domain note**: Cookie domain must match the hostname users actually browse. If users are on `web.app`, do not set `.firebaseapp.com`.

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

## 15. End-to-end integration tests (Pub/Sub, storage, mail)

Use this checklist after backend + frontend deployment is healthy.

### 15.1. Confirm implemented event flow

From current backend implementation:

1. Creating/changing orders publishes events to `order-events`.
2. Storage finalize webhook (`/api/webhooks/pubsub/storage-uploads`) republishes `FILE_UPLOADED` to `order-events`.
3. `order-events` webhook (`/api/webhooks/pubsub/order-events`) invokes notification worker.
4. Notification worker sends email only for selected order events and publishes dispatch records to `notification-events`.

### 15.2. Validate Pub/Sub infrastructure

```bash
gcloud pubsub topics list --project="$PROJECT_ID"
gcloud pubsub subscriptions list --project="$PROJECT_ID"
```

Required topics:

1. `storage-uploads`
2. `order-events`
3. `notification-events`

Required push subscriptions:

1. `backend-storage-uploads-sub` -> `${BACKEND_SERVICE_URL}/api/webhooks/pubsub/storage-uploads`
2. `backend-order-events-sub` -> `${BACKEND_SERVICE_URL}/api/webhooks/pubsub/order-events`

### 15.3. Validate OIDC-protected push webhooks

Ensure these env vars are set:

1. `PUBSUB_REQUIRE_OIDC=true`
2. `PUBSUB_WEBHOOK_AUDIENCE=${BACKEND_SERVICE_URL}`

Expected behavior:

1. Missing/invalid bearer token on webhook -> `401`.
2. Valid Pub/Sub OIDC push -> `204`.

### 15.4. Validate mail integration settings

Required when `MAIL_ENABLED=true`:

1. `MAIL_HOST`
2. `MAIL_PORT`
3. `MAIL_USERNAME`
4. `MAIL_PASSWORD`
5. `MAIL_FROM_ADDRESS`

If SMTP is not configured, keep `MAIL_ENABLED=false` and expect dispatch status `DISABLED` in logs/events.

### 15.5. Functional E2E test scenarios

1. Client creates order:
    - Expect `ORDER_CREATED` publish to `order-events`.
    - Expect webhook processing log in backend.
    - Expect notification worker action (`SENT`, `FAILED`, or `DISABLED`).
2. Employee changes status:
    - Expect `ORDER_STATUS_CHANGED` publish and downstream processing.
3. Employee marks in-progress:
    - Expect `ORDER_IN_PROGRESS` publish and downstream processing.
4. Employee reports issue:
    - Expect `ORDER_ISSUE_REPORTED` publish and downstream processing.
5. Client uploads file to GCS:
    - Expect GCS finalize event -> `storage-uploads` topic -> storage webhook -> `FILE_UPLOADED` event on `order-events`.

### 15.6. Log-based verification commands

```bash
gcloud run services logs read backend-service --project="$PROJECT_ID" --region="$REGION" --limit=300
```

Look for:

1. `Publishing ... event for orderId=...`
2. `Received order-event webhook messageId=...`
3. `Handling order event for notifications ...`
4. `Published event to topic=notification-events ...`
5. For storage flow: `Received storage finalize ...` followed by `Publishing file-uploaded event ...`
