#!/bin/bash
# destroy_cluster.sh — deletes every DigitalOcean droplet tagged
# "vibes-cluster" (or $TAG). Stops the per-hour billing.
#
# Run THIS as soon as collect_results.sh finishes successfully and you
# have the aggregated CSVs locally — otherwise the cluster keeps burning
# ~$0.90/h.
#
# Usage:
#   ./destroy_cluster.sh                # default tag "vibes-cluster"
#   TAG=vibes-cluster-2 ./destroy_cluster.sh

set -euo pipefail
TAG="${TAG:-vibes-cluster}"

if ! command -v doctl >/dev/null; then
    echo "ERROR: doctl not installed." >&2
    exit 1
fi

DROPLETS=$(doctl compute droplet list --tag-name "$TAG" --format ID,Name --no-header)
if [ -z "$DROPLETS" ]; then
    echo "No droplets found with tag '$TAG'. Nothing to destroy."
    exit 0
fi

echo "==============================================="
echo "  ABOUT TO DESTROY THE FOLLOWING DROPLETS"
echo "  Tag: $TAG"
echo "==============================================="
echo "$DROPLETS"
echo ""
read -p "Type 'destroy' to confirm: " CONFIRM
if [ "$CONFIRM" != "destroy" ]; then
    echo "Aborted — no droplets destroyed."
    exit 0
fi

echo ""
echo ">>> Destroying droplets..."
doctl compute droplet delete --tag-name "$TAG" --force

# Verify
sleep 3
REMAINING=$(doctl compute droplet list --tag-name "$TAG" --no-header | wc -l | xargs)
if [ "$REMAINING" = "0" ]; then
    echo "All droplets with tag '$TAG' destroyed."
else
    echo "WARNING: $REMAINING droplets still listed — re-run or check DO console."
fi

# Optional: clear local ips.txt so a future provision starts clean
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"
if [ -f "$IPS_FILE" ]; then
    cp "$IPS_FILE" "$IPS_FILE.last"
    cat > "$IPS_FILE" <<'EOF'
# scripts/cloud/ips.txt — one DigitalOcean droplet IP per line.
# (Cleared after destroy_cluster.sh; previous IPs saved to ips.txt.last)
EOF
    echo "Reset $IPS_FILE (previous list saved to ips.txt.last)"
fi
