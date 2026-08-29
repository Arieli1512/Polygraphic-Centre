#!/usr/bin/env bash
# Run from Cloud Shell (or any shell with gcloud authenticated and enough privileges).
# SSHes into the postgres-db-vm instance (via IAP), runs the repo's db/scripts/stop.sh
# to bring the postgres container down, then stops the VM to save costs.
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

# What to do with the VM once the container is stopped: stop (default) or suspend.
VM_SHUTDOWN_MODE="${VM_SHUTDOWN_MODE:-stop}"

status="$(gcloud compute instances describe "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --format='value(status)')"

echo "Current VM status: ${status}"

if [[ "$status" != "RUNNING" ]]; then
    echo "VM is not running (status: ${status}); nothing to stop."
    exit 0
fi

echo "Running db/scripts/stop.sh on ${VM_NAME}..."
gcloud compute ssh "$VM_NAME" \
    --project="$PROJECT_ID" \
    --zone="$ZONE" \
    --tunnel-through-iap \
    --command="cd '${REMOTE_REPO_DIR}' && bash db/scripts/stop.sh"

case "$VM_SHUTDOWN_MODE" in
    suspend)
        echo "Suspending VM..."
        gcloud compute instances suspend "$VM_NAME" \
            --project="$PROJECT_ID" \
            --zone="$ZONE"
        ;;
    stop)
        echo "Stopping VM..."
        gcloud compute instances stop "$VM_NAME" \
            --project="$PROJECT_ID" \
            --zone="$ZONE"
        ;;
    *)
        echo "Unknown VM_SHUTDOWN_MODE='${VM_SHUTDOWN_MODE}' (expected 'suspend' or 'stop')" >&2
        exit 1
        ;;
esac

echo "Database stopped and VM ${VM_SHUTDOWN_MODE}ed."
