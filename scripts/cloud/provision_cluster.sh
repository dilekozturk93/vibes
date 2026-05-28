#!/bin/bash
# provision_cluster.sh — opens N DigitalOcean droplets via doctl,
# bakes a cloud-init user-data with Java/Maven/git pre-installed so the
# downstream setup_and_run.sh only has to git-clone + start the
# orchestrator (no apt-get wait).
#
# Side effects:
#   - creates droplets named "vibes-node-<i>" tagged "vibes-cluster"
#   - waits for them to reach "active" + SSH-ready
#   - writes their public IPv4 to scripts/cloud/ips.txt (overwriting it)
#
# Prerequisites:
#   - doctl installed and authenticated:    brew install doctl
#                                            doctl auth init
#   - SSH key registered with DigitalOcean (note its fingerprint or ID:
#                                            doctl compute ssh-key list)
#
# Usage:
#   SSH_KEY_ID=12345678 ./provision_cluster.sh                  # default 9 nodes
#   SSH_KEY_ID=12345678 NODE_COUNT=5 REGION=fra1 ./provision_cluster.sh
#
# Destroy with:  ./destroy_cluster.sh

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

NODE_COUNT="${NODE_COUNT:-9}"
SIZE="${SIZE:-s-8vcpu-16gb}"
REGION="${REGION:-nyc3}"
IMAGE="${IMAGE:-ubuntu-22-04-x64}"
TAG="${TAG:-vibes-cluster}"
PREFIX="${PREFIX:-vibes-node}"

if [ -z "${SSH_KEY_ID:-}" ]; then
    echo "ERROR: SSH_KEY_ID env var required." >&2
    echo "  Run: doctl compute ssh-key list" >&2
    echo "  Then: SSH_KEY_ID=<id-or-fingerprint> $0" >&2
    exit 1
fi
if ! command -v doctl >/dev/null; then
    echo "ERROR: doctl not installed. Install: brew install doctl  (then: doctl auth init)" >&2
    exit 1
fi

# Build cloud-init user-data: pre-install Java + Maven + git + dos2unix
# so setup_and_run.sh skips the slow apt-get step.
USER_DATA="$(cat <<'CLOUDINIT'
#cloud-config
package_update: true
package_upgrade: false
packages:
  - openjdk-11-jdk
  - maven
  - git
  - dos2unix
  - curl
  - rsync
runcmd:
  - update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java || true
  - echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64' >> /root/.bashrc
CLOUDINIT
)"

echo "==============================================="
echo "  PROVISIONING $NODE_COUNT VIBES CLUSTER NODES"
echo "  Size:    $SIZE"
echo "  Region:  $REGION"
echo "  Image:   $IMAGE"
echo "  Tag:     $TAG"
echo "  Prefix:  ${PREFIX}-1 .. ${PREFIX}-${NODE_COUNT}"
echo "  SSH key: $SSH_KEY_ID"
echo "==============================================="

NAMES=()
for i in $(seq 1 "$NODE_COUNT"); do
    NAMES+=("${PREFIX}-${i}")
done
JOINED_NAMES="$(IFS=' '; echo "${NAMES[*]}")"

# Create all droplets in one batch — much faster than per-droplet calls.
echo ">>> Creating droplets (one batch)..."
doctl compute droplet create $JOINED_NAMES \
    --size "$SIZE" \
    --region "$REGION" \
    --image "$IMAGE" \
    --ssh-keys "$SSH_KEY_ID" \
    --tag-name "$TAG" \
    --user-data "$USER_DATA" \
    --wait \
    --format ID,Name,PublicIPv4

# After --wait, droplets are "active" but cloud-init still running.
# Poll SSH readiness (port 22 reachable + root login working).
echo ""
echo ">>> Collecting public IPs..."
IPS=$(doctl compute droplet list --tag-name "$TAG" --format PublicIPv4 --no-header | grep -v '^$')

if [ -z "$IPS" ]; then
    echo "ERROR: no IPs found for tag '$TAG'." >&2
    exit 2
fi

echo "$IPS" > "$IPS_FILE"
echo "Wrote $(echo "$IPS" | wc -l | xargs) IPs to $IPS_FILE"

echo ""
echo ">>> Waiting for SSH readiness on each droplet (this takes 1-3 min while cloud-init runs)..."
for ip in $IPS; do
    printf "    %-15s " "$ip"
    for attempt in $(seq 1 60); do
        if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 -o BatchMode=yes \
            root@"$ip" "test -f /var/lib/cloud/instance/boot-finished" 2>/dev/null; then
            echo "ready"
            break
        fi
        sleep 5
        if [ "$attempt" -eq 60 ]; then
            echo "TIMEOUT (5 min) — proceed anyway, but verify manually"
        fi
    done
done

echo ""
echo "==============================================="
echo "  CLUSTER PROVISIONING COMPLETE"
echo "  $(echo "$IPS" | wc -l | xargs) droplets ready, IPs in $IPS_FILE"
echo "  Next:  scripts/cloud/setup_and_run.sh"
echo "  Cost:  ~\$0.10/h × $NODE_COUNT nodes = \$$(echo "scale=2; 0.10 * $NODE_COUNT" | bc)/h"
echo "  Destroy when done:  scripts/cloud/destroy_cluster.sh"
echo "==============================================="
