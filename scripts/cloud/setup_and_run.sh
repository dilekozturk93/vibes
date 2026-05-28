#!/bin/bash
# setup_and_run.sh — provisions the cluster + dispatches workers.
# Reads scripts/cloud/ips.txt (one IP per line), and on each IP:
#   1. apt-get install Java/Maven/git
#   2. git clone the VIBeS fork (and switch to the branch under test)
#   3. generate shards.txt
#   4. start node_orchestrator.sh in nohup
#
# After this script returns, monitor progress with cluster_monitor.sh.
# When all nodes are done, collect_results.sh aggregates the shard CSVs
# back to milestone-reports/metrics/.
#
# Inspired by ESG-Fx Setup_And_Run_DigitalOcean.sh — same SSH-bootstrap
# pattern, adapted to VIBeS layout.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"
LOG_DIR="milestone-reports/metrics/logs/cloud/setup"
mkdir -p "$LOG_DIR"

# Required: VIBeS fork URL + branch.
GIT_REPO="${GIT_REPO:-https://github.com/dilekozturk93/vibes.git}"
GIT_BRANCH="${GIT_BRANCH:-feat/product-test-generation}"
PARALLEL_WORKERS="${PARALLEL_WORKERS:-3}"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE" >&2
    echo "Create it with one IP per line — one DigitalOcean droplet per IP." >&2
    exit 1
fi

# Normalize line endings, strip blanks
tr -d '\r' < "$IPS_FILE" | grep -v '^[[:space:]]*$' > "${IPS_FILE}.clean"
mapfile -t IP_LIST < "${IPS_FILE}.clean"
rm -f "${IPS_FILE}.clean"

TOTAL_NODES=${#IP_LIST[@]}
if [ "$TOTAL_NODES" -eq 0 ]; then
    echo "ERROR: ips.txt has no usable IP" >&2
    exit 1
fi

echo "==============================================="
echo "VIBeS CLOUD CLUSTER PROVISIONING"
echo "Nodes:   $TOTAL_NODES (each runs $PARALLEL_WORKERS parallel workers)"
echo "Repo:    $GIT_REPO"
echo "Branch:  $GIT_BRANCH"
echo "==============================================="

launch_node() {
    local node_index="$1"
    local ip="${IP_LIST[$node_index]}"
    local log_file="$LOG_DIR/node${node_index}_${ip}.log"

    echo "   [$(date '+%H:%M:%S')] Configuring node $node_index → $ip (log: $log_file)"

    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
        root@"$ip" bash -s "$node_index" "$TOTAL_NODES" "$PARALLEL_WORKERS" "$GIT_REPO" "$GIT_BRANCH" \
        > "$log_file" 2>&1 << 'REMOTE_EOF' &
NODE_INDEX="$1"
TOTAL_NODES="$2"
PARALLEL_WORKERS="$3"
REPO_URL="$4"
BRANCH="$5"

set -e
export DEBIAN_FRONTEND=noninteractive
export NEEDRESTART_MODE=a
export NEEDRESTART_SUSPEND=1

echo "[1/5] Waiting for cloud-init + dpkg locks..."
cloud-init status --wait 2>/dev/null || true
while fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1; do sleep 5; done

echo "[2/5] Installing OpenJDK 11 + Maven + git..."
apt-get update -y -qq
apt-get install -y -qq openjdk-11-jdk maven git dos2unix curl

echo "[3/5] Cloning repo $REPO_URL (branch $BRANCH)..."
rm -rf /root/vibes
cd /root
git clone --depth 1 --branch "$BRANCH" "$REPO_URL"
cd /root/vibes

echo "[4/5] Generating shards.txt + preparing scripts..."
dos2unix scripts/cloud/*.sh 2>/dev/null || true
chmod +x scripts/cloud/*.sh
bash scripts/cloud/generate_shards.sh

echo "[5/5] Starting node_orchestrator (node=$NODE_INDEX/$TOTAL_NODES, parallel=$PARALLEL_WORKERS)..."
nohup bash scripts/cloud/node_orchestrator.sh "$NODE_INDEX" "$TOTAL_NODES" "$PARALLEL_WORKERS" \
    > /root/node_orchestrator.log 2>&1 &

echo "Node $NODE_INDEX provisioning complete — orchestrator running in background."
REMOTE_EOF
}

for i in "${!IP_LIST[@]}"; do
    launch_node "$i"
    sleep 1   # avoid SSH thundering herd
done

echo ""
echo "All $TOTAL_NODES SSH bootstrap commands dispatched in parallel."
echo "Setup logs:    $LOG_DIR/"
echo "Monitor with:  scripts/cloud/cluster_monitor.sh"
echo "Collect with:  scripts/cloud/collect_results.sh   (after monitor shows all nodes idle)"
wait
echo "Setup wave complete."
