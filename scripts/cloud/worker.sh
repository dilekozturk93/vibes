#!/bin/bash
# worker.sh — runs a SINGLE shard. Invoked from node_orchestrator.sh
# with: <task> <SPL> <start_idx> <end_idx> <run_id> <shard_id>
#
# Sets the env vars the Java side expects (SHARD_ID, PRODUCT_START_IDX,
# PRODUCT_END_IDX, runID, RANDOM_SEED_COUNT, RQ_CSV_DIR / RQ1_CSV_DIR,
# XMX) and execs java.
#
# CSV outputs go to shard-suffixed filenames; no race conditions when
# multiple workers run concurrently on the same node.

set -euo pipefail

if [ "$#" -ne 6 ]; then
    echo "USAGE: $0 <task> <SPL> <start_idx> <end_idx> <run_id> <shard_id>" >&2
    exit 2
fi

TASK="$1"
SPL="$2"
START_IDX="$3"
END_IDX="$4"
RUN_ID="$5"
SHARD_ID="$6"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

# Per-worker JVM heap. 16 GB nodes / 3 workers ≈ 5 GB headroom each.
XMX="${XMX:-5g}"

# CSV output dirs (per-task to mirror the local layout).
RQ2_DIR="${RQ_CSV_DIR:-milestone-reports/metrics/shards/$SPL}"
RQ1_DIR="${RQ1_CSV_DIR:-milestone-reports/metrics/shards/$SPL}"
mkdir -p "$RQ2_DIR" "$RQ1_DIR"

# Per-shard log location.
LOG_DIR="milestone-reports/metrics/logs/cloud"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/${TASK}_${SPL}_shard${SHARD_ID}.log"

CP_FILE="$(mktemp)"
trap 'rm -f "$CP_FILE"' EXIT

# -am required because vibes-testgeneration depends on sibling modules
# (vibes-core/fexpression/dsl/selection) that are NOT installed to
# ~/.m2/repository/ — they only exist in the multi-module reactor. Without
# -am, dependency:build-classpath fails to resolve siblings and the
# worker silently dies under `set -e`.
mvn -q -pl vibes-testgeneration -am dependency:build-classpath \
    -DincludeScope=test -Dmdep.outputFile="$CP_FILE"
CP="vibes-testgeneration/target/classes:vibes-testgeneration/target/test-classes:$(cat "$CP_FILE")"

# Make sure test-compile has happened so target/test-classes is populated.
if [ ! -d "vibes-testgeneration/target/test-classes" ]; then
    mvn -q -pl vibes-testgeneration -am test-compile
fi

echo "[$(date '+%H:%M:%S')] worker start: task=$TASK SPL=$SPL range=[$START_IDX,$END_IDX] runID=$RUN_ID shard=$SHARD_ID xmx=$XMX" >> "$LOG_FILE"

case "$TASK" in
    rq2_mut)
        SHARD_ID="$SHARD_ID" \
        PRODUCT_START_IDX="$START_IDX" \
        PRODUCT_END_IDX="$END_IDX" \
        runID="$RUN_ID" \
        RQ_CSV_DIR="$RQ2_DIR" \
        RANDOM_SEED_COUNT="${RANDOM_SEED_COUNT:-100}" \
        java -Xmx"$XMX" -XX:+UseG1GC -cp "$CP" \
            be.vibes.testgeneration.experiment.PerProductMutationReportGenerator \
            "$SPL" >> "$LOG_FILE" 2>&1
        ;;
    rq1_scal)
        SHARD_ID="$SHARD_ID" \
        PRODUCT_START_IDX="$START_IDX" \
        PRODUCT_END_IDX="$END_IDX" \
        runID="$RUN_ID" \
        RQ1_CSV_DIR="$RQ1_DIR" \
        java -Xmx"$XMX" -XX:+UseG1GC -cp "$CP" \
            be.vibes.testgeneration.experiment.ExperimentRunner \
            "$SPL" >> "$LOG_FILE" 2>&1
        ;;
    *)
        echo "Unknown task: $TASK" >&2
        exit 3
        ;;
esac

echo "[$(date '+%H:%M:%S')] worker end: shard=$SHARD_ID exit=$?" >> "$LOG_FILE"
