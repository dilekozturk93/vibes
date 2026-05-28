#!/bin/bash
# collect_results.sh — pulls per-shard CSVs from every cluster node
# back to the local repo, then aggregates them into unified rq1/rq2/rq3
# CSVs that the local analysis pipeline can consume directly.
#
# Local layout after this script:
#   milestone-reports/metrics/shards/<SPL>/<task>_<SPL>_shard<NN>.csv  (raw)
#   milestone-reports/metrics/rq{1,2,3}-*.csv                            (aggregated)

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found" >&2
    exit 1
fi
tr -d '\r' < "$IPS_FILE" | grep -v '^[[:space:]]*$' > "${IPS_FILE}.clean"
IPS=()
while IFS= read -r line; do
    [ -n "$line" ] && IPS+=("$line")
done < "${IPS_FILE}.clean"
rm -f "${IPS_FILE}.clean"

SHARDS_LOCAL="$REPO_ROOT/milestone-reports/metrics/shards"
mkdir -p "$SHARDS_LOCAL"

echo "==============================================="
echo "COLLECTING SHARD CSVs FROM ${#IPS[@]} NODES"
echo "==============================================="

for ip in "${IPS[@]}"; do
    echo ">>> $ip"
    # rsync everything under milestone-reports/metrics/shards/
    rsync -avq -e "ssh -o StrictHostKeyChecking=no -o ConnectTimeout=15" \
        root@"$ip":/root/vibes/milestone-reports/metrics/shards/ \
        "$SHARDS_LOCAL/" 2>&1 | tail -3
    # Also grab cloud-mode logs for the audit trail.
    mkdir -p "$REPO_ROOT/milestone-reports/metrics/logs/cloud/$ip"
    rsync -avq -e "ssh -o StrictHostKeyChecking=no -o ConnectTimeout=15" \
        root@"$ip":/root/vibes/milestone-reports/metrics/logs/cloud/ \
        "$REPO_ROOT/milestone-reports/metrics/logs/cloud/$ip/" 2>&1 | tail -1
done

echo ""
echo "==============================================="
echo "AGGREGATING SHARD CSVs → unified rq{1,2,3}-*.csv"
echo "==============================================="

# Aggregate per CSV-task. Each shard CSV has the same header; we keep
# the first header, drop the rest, and concat all data rows.
aggregate() {
    local task="$1"
    local out="$REPO_ROOT/milestone-reports/metrics/${task}.csv"
    local first=true
    local shard_files
    mapfile -t shard_files < <(find "$SHARDS_LOCAL" -name "${task}_*.csv" | sort)
    if [ "${#shard_files[@]}" -eq 0 ]; then
        echo "  WARNING: no shards for $task"
        return
    fi
    > "$out"
    for f in "${shard_files[@]}"; do
        if $first; then
            cat "$f" >> "$out"
            first=false
        else
            tail -n +2 "$f" >> "$out"
        fi
    done
    local rows=$(wc -l < "$out")
    echo "  $task.csv: $rows lines from ${#shard_files[@]} shards"
}

aggregate "rq2-coverage-directed"
aggregate "rq2-random-baseline"
aggregate "rq3-efficiency"
aggregate "rq1-coverage-directed"
aggregate "rq1-family-baseline"

echo ""
echo "==============================================="
echo "DONE — aggregated CSVs in milestone-reports/metrics/"
echo "Shard-level CSVs preserved in milestone-reports/metrics/shards/"
echo "Logs in milestone-reports/metrics/logs/cloud/"
echo "==============================================="
