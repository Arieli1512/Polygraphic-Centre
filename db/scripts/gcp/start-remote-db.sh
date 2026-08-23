#!/usr/bin/env bash
# Run from Cloud Shell (or any shell with gcloud authenticated and enough privileges).
# Resumes/starts the postgres-db-vm instance, then SSHes in (via IAP) and runs the
# repo's db/scripts/start.sh on the VM to bring up the postgres container.
set -euo pipefail

export PROJECT_ID="${PROJECT_ID:-drobnyd-b1d45}"
export REGION="${REGION:-us-east1}"
export ZONE="${ZONE:-us-east1-b}"
export NETWORK="${NETWORK:-default}"
export SUBNET="${SUBNET:-default}"
export VM_NAME="${VM_NAME:-postgres-db-vm}"
export STATIC_IP="${STATIC_IP:-10.142.0.3}"
export BACKEND_SA="${BACKEND_SA:-backend-runtime@${PROJECT_ID}.iam.gserviceaccount.com}"
export BUCKET_NAME="${BUCKET_NAME:-my-free-app-bucket-drobnyd-b1d45}"
export TOPIC_NAME="${TOPIC_NAME:-storage-uploads}"

# Path on the VM where the repo is checked out (shared location, see gcp_instructions.md section 2.7).
REMOTE_REPO_DIR="${REMOTE_REPO_DIR:-/opt/polygraphic-centre}"

status="$(gcloud compute instances describe "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --format='value(status)')"

echo "Current VM status: ${status}"

case "$status" in
    RUNNING)
        echo "VM is already running."
        ;;
    SUSPENDED)
        echo "Resuming suspended VM..."
        gcloud compute instances resume "$VM_NAME" \
            --project="$PROJECT_ID" \
            --zone="$ZONE"
        ;;
    TERMINATED)
        echo "Starting stopped VM..."
        gcloud compute instances start "$VM_NAME" \
            --project="$PROJECT_ID" \
            --zone="$ZONE"
        ;;
    *)
        echo "Unexpected VM status '${status}', attempting start..."
        gcloud compute instances start "$VM_NAME" \
            --project="$PROJECT_ID" \
            --zone="$ZONE"
        ;;
esac

echo "Waiting for VM to become RUNNING..."
for i in $(seq 1 30); do
    status="$(gcloud compute instances describe "$VM_NAME" \
        --project="$PROJECT_ID" \
        --zone="$ZONE" \
        --format='value(status)')"
    if [[ "$status" == "RUNNING" ]]; then
        break
    fi
    sleep 5
done

if [[ "$status" != "RUNNING" ]]; then
    echo "VM did not reach RUNNING state in time (last status: ${status})" >&2
    exit 1
fi

echo "Waiting for SSH to become available..."
for i in $(seq 1 30); do
    if gcloud compute ssh "$VM_NAME" \
        --project="$PROJECT_ID" \
        --zone="$ZONE" \
        --tunnel-through-iap \
        --command="echo ssh-ready" >/dev/null 2>&1; then
        break
    fi
    sleep 5
done

echo "Running db/scripts/start.sh on ${VM_NAME}..."
gcloud compute ssh "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --tunnel-through-iap \
    --command="cd '${REMOTE_REPO_DIR}' && bash db/scripts/start.sh"

echo "Database started."
