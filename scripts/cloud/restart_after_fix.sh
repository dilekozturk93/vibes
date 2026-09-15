#!/bin/bash
# restart_after_fix.sh — after pushing a code fix, refresh every cluster
# node and restart its orchestrator. Used to recover from the silent
# worker failure that left orchestrators waiting on dead background
# subprocesses (mvn dependency:build-classpath fail before -am fix).

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

tr -d '\r' < "$IPS_FILE" | grep -v '^[[:space:]]*$' > "${IPS_FILE}.clean"
IPS=()
while IFS= read -r line; do
    [ -n "$line" ] && IPS+=("$line")
done < "${IPS_FILE}.clean"
rm -f "${IPS_FILE}.clean"

TOTAL=${#IPS[@]}
PARALLEL_WORKERS="${PARALLEL_WORKERS:-3}"

echo "Refreshing $TOTAL nodes (git pull + kill old workers + restart orchestrator)..."
for i in "${!IPS[@]}"; do
    ip="${IPS[$i]}"
    echo "  [$ip] node $i/$((TOTAL-1))"
    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=15 root@"$ip" \
        bash -s "$i" "$TOTAL" "$PARALLEL_WORKERS" << 'REMOTE_EOF' &
NODE_INDEX="$1"
TOTAL_NODES="$2"
PARALLEL_WORKERS="$3"

# Kill any stuck worker/orchestrator processes
pkill -9 -f node_orchestrator.sh 2>/dev/null || true
pkill -9 -f scripts/cloud/worker.sh 2>/dev/null || true
pkill -9 java 2>/dev/null || true
pkill -9 mvn 2>/dev/null || true

# Pull latest fix
cd /root/vibes
git fetch origin
git reset --hard origin/feat/product-test-generation
dos2unix scripts/cloud/*.sh 2>/dev/null || true
chmod +x scripts/cloud/*.sh

# Wipe stale logs so we can tell the new run apart
rm -rf /root/vibes/milestone-reports/metrics/logs/cloud/*.log
rm -f /root/node_orchestrator.log

# Re-launch orchestrator
nohup bash scripts/cloud/node_orchestrator.sh "$NODE_INDEX" "$TOTAL_NODES" "$PARALLEL_WORKERS" \
    > /root/node_orchestrator.log 2>&1 &

echo "Node $NODE_INDEX: restarted (orchestrator PID $!)"
REMOTE_EOF
done
wait

echo ""
echo "All $TOTAL nodes restarted. Wait 1-2 min then check:"
echo "  watch -n 30 scripts/cloud/cluster_monitor.sh"
