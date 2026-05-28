#!/bin/bash
# node_orchestrator.sh — runs on a single cluster node. Reads shards.txt,
# picks the lines whose node-index matches this node's slot, and runs up
# to PARALLEL_WORKERS shard workers concurrently.
#
# USAGE: ./node_orchestrator.sh <node_index> <total_nodes> [parallel_workers]
#
# Shard assignment is round-robin: shard N goes to node (N % total_nodes).
# Within a node, up to PARALLEL_WORKERS shards run in parallel (default 3
# for 8-core / 16GB DigitalOcean droplets — each JVM XMX=5g, total 15GB).

set -euo pipefail

if [ "$#" -lt 2 ]; then
    echo "USAGE: $0 <node_index> <total_nodes> [parallel_workers]" >&2
    exit 2
fi

NODE_INDEX="$1"
TOTAL_NODES="$2"
PARALLEL_WORKERS="${3:-3}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHARDS_FILE="$SCRIPT_DIR/shards.txt"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

if [ ! -f "$SHARDS_FILE" ]; then
    echo "shards.txt not found at $SHARDS_FILE — run generate_shards.sh first" >&2
    exit 3
fi

LOG_DIR="milestone-reports/metrics/logs/cloud"
mkdir -p "$LOG_DIR"
NODE_LOG="$LOG_DIR/node${NODE_INDEX}.log"

echo "=============================================" | tee -a "$NODE_LOG"
echo "node_orchestrator: node=$NODE_INDEX/$TOTAL_NODES parallel=$PARALLEL_WORKERS" | tee -a "$NODE_LOG"
echo "  $(date)" | tee -a "$NODE_LOG"
echo "=============================================" | tee -a "$NODE_LOG"

# Build + install once before workers start. `install -DskipTests` puts
# every sibling module (vibes-core, vibes-fexpression, vibes-dsl, etc.)
# into ~/.m2/repository/ so worker.sh's `mvn dependency:build-classpath`
# can resolve them. Without this, dependency:build-classpath fails to
# find sibling JARs (they only exist in target/ otherwise) and workers
# silently die under set -e.
mvn -q install -pl vibes-testgeneration -am -DskipTests 2>&1 | tee -a "$NODE_LOG"

# Filter shards to those assigned to this node (round-robin by global index).
ASSIGNED_SHARDS=()
ASSIGNED_LINES=()
GLOBAL_IDX=0
while IFS= read -r line; do
    # Skip comments + blank lines
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "${line// }" ]] && continue
    OWNER=$((GLOBAL_IDX % TOTAL_NODES))
    if [ "$OWNER" -eq "$NODE_INDEX" ]; then
        ASSIGNED_SHARDS+=("$GLOBAL_IDX")
        ASSIGNED_LINES+=("$line")
    fi
    GLOBAL_IDX=$((GLOBAL_IDX + 1))
done < "$SHARDS_FILE"

echo "Assigned shards: ${#ASSIGNED_LINES[@]} (out of $GLOBAL_IDX total)" | tee -a "$NODE_LOG"
for line in "${ASSIGNED_LINES[@]}"; do
    echo "  $line" | tee -a "$NODE_LOG"
done

# Worker queue: launch up to PARALLEL_WORKERS concurrently, wait when full.
ACTIVE=0
SHARD_COUNT=${#ASSIGNED_LINES[@]}
for i in "${!ASSIGNED_LINES[@]}"; do
    line="${ASSIGNED_LINES[$i]}"
    # Parse: task SPL start end run_id shard_id
    read -r TASK SPL START END RUN SHARD <<< "$line"
    echo "[$(date '+%H:%M:%S')] launching ($((i+1))/$SHARD_COUNT): $TASK $SPL [$START,$END] run=$RUN shard=$SHARD" | tee -a "$NODE_LOG"

    nohup bash "$SCRIPT_DIR/worker.sh" "$TASK" "$SPL" "$START" "$END" "$RUN" "$SHARD" \
        > /dev/null 2>&1 &
    ACTIVE=$((ACTIVE + 1))

    if [ "$ACTIVE" -ge "$PARALLEL_WORKERS" ]; then
        # Block until one worker finishes, then continue
        wait -n
        ACTIVE=$((ACTIVE - 1))
    fi
done

# Drain remaining workers
wait
echo "[$(date '+%H:%M:%S')] node $NODE_INDEX complete — all $SHARD_COUNT shards finished" | tee -a "$NODE_LOG"
