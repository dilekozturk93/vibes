#!/bin/bash
# cluster_monitor.sh — one-shot status snapshot across every node in
# scripts/cloud/ips.txt. SSHes into each node, reads:
#   - active java processes (worker count)
#   - RAM / load
#   - currently running shard tags (from worker.sh log dir)
#   - completed shards (CSV file count under shards/)
#
# Run repeatedly (or wrap in `watch`) to track cluster progress.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found" >&2
    exit 1
fi
tr -d '\r' < "$IPS_FILE" | grep -v '^[[:space:]]*$' > "${IPS_FILE}.clean"
mapfile -t IPS < "${IPS_FILE}.clean"
rm -f "${IPS_FILE}.clean"

clear
echo "============================================================================"
echo "  VIBeS CLOUD CLUSTER MONITOR — $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================================"
printf "%-3s %-15s %-5s %-7s %-12s %-7s %-35s\n" \
    "#" "IP" "JAVA" "RAM%" "LOAD" "CSVs" "ACTIVE SHARDS"
echo "----------------------------------------------------------------------------"

for i in "${!IPS[@]}"; do
    ip="${IPS[$i]}"
    idx=$((i + 1))
    data=$(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
        root@"$ip" bash 2>/dev/null << 'REMOTE_EOF'
        JCOUNT=$(pgrep -c java 2>/dev/null || echo 0)
        # RAM used / total
        RAM_INFO=$(free -m | awk '/Mem:/ {printf "%d%%", $3*100/$2}')
        LOAD=$(uptime | awk -F'load average:' '{print $2}' | awk -F, '{print $1}' | xargs)
        # Active shards (worker logs touched in last 60s)
        ACTIVE=$(find /root/vibes/milestone-reports/metrics/logs/cloud \
            -name '*.log' -mmin -1 2>/dev/null \
            | xargs -I{} basename {} .log 2>/dev/null \
            | sed 's/rq[12]_[a-z]*_//' | head -3 | tr '\n' ' ')
        # Completed shard CSVs
        CSVS=$(find /root/vibes/milestone-reports/metrics/shards \
            -name '*.csv' 2>/dev/null | wc -l | xargs)
        echo "${JCOUNT}|${RAM_INFO}|${LOAD}|${CSVS}|${ACTIVE}"
REMOTE_EOF
    )
    if [ -z "$data" ]; then
        printf "%-3s %-15s %-5s %-7s %-12s %-7s %-35s\n" \
            "$idx" "$ip" "?" "?" "?" "?" "UNREACHABLE"
        continue
    fi
    IFS='|' read -r JC RAM LD CSV ACT <<< "$data"
    status="$([ "$JC" -gt 0 ] && echo "🟢" || echo "⚫")"
    printf "%s %-3s %-15s %-5s %-7s %-12s %-7s %-35s\n" \
        "$status" "$idx" "$ip" "$JC" "$RAM" "$LD" "$CSV" "${ACT:-idle}"
done
echo "============================================================================"
