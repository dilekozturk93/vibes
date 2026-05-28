#!/bin/bash
# generate_shards.sh — produces shards.txt manifest from measured per-product
# costs. One line per work unit; consumed by node_orchestrator.sh on each
# cluster node.
#
# Format (whitespace-separated):
#   <task>  <SPL>  <start_idx>  <end_idx>  <run_id>  <shard_id>
#
# - task ∈ {rq2_mut, rq1_scal}
# - start_idx, end_idx: 1-based, half-open [start, end] inclusive
#   (matches the SHARD_ID / PRODUCT_START_IDX / PRODUCT_END_IDX semantics
#   in PerProductMutationReportGenerator + ExperimentRunner)
# - run_id: 1 for rq2_mut; 1..11 for rq1_scal (RQ1 is the 11-run scalability sweep)
# - shard_id: short alphanumeric tag used in CSV filenames
#   (e.g. svia07, sas01, te_r03)
#
# Cost model (measured, single thread, 100×3 random ensemble where applicable):
#   Syngovia: ~23.5 min/product  (DOMINATES)
#   SAS:      ~2 sec/product
#   HockertyShirts / Tesla: ~9 sec/product
#   BAv2 / Elevator / eMail / SVM: sub-second

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="$SCRIPT_DIR/shards.txt"

cat > "$OUT" <<'EOF'
# task         SPL                       start  end   runId  shard_id
# RQ2 mutation (one runId per shard; deterministic up to random seed loop)
EOF

# Syngovia: 400 products, 23.5 min each, split into 20 shards × 20 products
# Each shard ≈ 470 min = 7.8h wall-clock on one worker.
for i in $(seq 0 19); do
    start=$((i * 20))
    end=$((start + 20))
    printf "%-13s %-25s %-6d %-5d %-6d svia%02d\n" \
        "rq2_mut" "Syngovia" "$start" "$end" "1" "$i" >> "$OUT"
done

# SAS: 2664 products, ~2 sec each, split into 2 shards × 1332 products
# Each shard ≈ 44 min.
for i in 0 1; do
    start=$((i * 1332))
    end=$((start + 1332))
    printf "%-13s %-25s %-6d %-5d %-6d sas%02d\n" \
        "rq2_mut" "StudentAttendanceSystem" "$start" "$end" "1" "$i" >> "$OUT"
done

# HockertyShirts: 416 products, ~9 sec each, split into 2 shards
for i in 0 1; do
    start=$((i * 208))
    end=$((start + 208))
    printf "%-13s %-25s %-6d %-5d %-6d hs%02d\n" \
        "rq2_mut" "HockertyShirts" "$start" "$end" "1" "$i" >> "$OUT"
done

# Tesla: 400 products, ~9 sec each, split into 2 shards
for i in 0 1; do
    start=$((i * 200))
    end=$((start + 200))
    printf "%-13s %-25s %-6d %-5d %-6d te%02d\n" \
        "rq2_mut" "Tesla" "$start" "$end" "1" "$i" >> "$OUT"
done

# Small SPLs (BAv2, Elevator, eMail, SVM): single shard each, full range.
printf "%-13s %-25s %-6d %-5d %-6d bav200\n" \
    "rq2_mut" "BankAccountv2" "0" "999999" "1" >> "$OUT"
printf "%-13s %-25s %-6d %-5d %-6d el00\n" \
    "rq2_mut" "Elevator" "0" "999999" "1" >> "$OUT"
printf "%-13s %-25s %-6d %-5d %-6d em00\n" \
    "rq2_mut" "eMail" "0" "999999" "1" >> "$OUT"
printf "%-13s %-25s %-6d %-5d %-6d svm00\n" \
    "rq2_mut" "SVM" "0" "999999" "1" >> "$OUT"

echo "" >> "$OUT"
echo "# RQ1 scalability sweep (11 runs × 8 SPLs)" >> "$OUT"
# RQ1: 11 runs × 8 SPLs = 88 work units. Each run/SPL is one shard (no
# per-product subsplitting — the RQ1 sweep is dominated by the family
# baseline + per-product timing cells, which are small per SPL).
SPLS_RQ1=(SVM eMail Elevator BankAccountv2 StudentAttendanceSystem Syngovia Tesla HockertyShirts)
for spl in "${SPLS_RQ1[@]}"; do
    short=$(echo "$spl" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]//g' | cut -c1-6)
    for run in $(seq 1 11); do
        printf "%-13s %-25s %-6d %-5d %-6d %s_r%02d\n" \
            "rq1_scal" "$spl" "0" "999999" "$run" "$short" "$run" >> "$OUT"
    done
done

# Summary
total=$(grep -cE '^[a-z]' "$OUT")
rq2=$(grep -cE '^rq2_mut' "$OUT")
rq1=$(grep -cE '^rq1_scal' "$OUT")
echo ""
echo "Generated $OUT"
echo "  Total work units: $total"
echo "  RQ2 mutation shards: $rq2 (Syngovia 20 + SAS 2 + HS 2 + Te 2 + 4 small)"
echo "  RQ1 scalability shards: $rq1 (8 SPL × 11 run)"
