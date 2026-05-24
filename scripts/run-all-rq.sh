#!/bin/bash
# run-all-rq.sh — full-evaluation orchestrator (user-launched).
#
# Three-step sequence with strict gating: a Step 1 failure aborts Step 2.
#
#   Step 0: Clean stale CSVs in milestone-reports/metrics/ (smoke
#           artifacts from prior development; append-on-write writers
#           would otherwise mix smoke + real data).
#
#   Step 1: RQ2 + RQ3 single-pass run via run-rq2-rq3-mutation.sh.
#           - 5 SPLs (SVM, eMail, Elevator, BankAccountv2, SAS)
#           - RANDOM_SEED_COUNT=100 (paper-fair baseline, explicit)
#           - XMX=5g (user's machine: 8 GB RAM)
#           - Writes rq2-coverage-directed.csv + rq2-random-baseline.csv
#             + rq3-efficiency.csv (one shot, deterministic).
#           - If java exits non-zero, ABORTS the orchestrator (set -e);
#             Step 2 is NOT entered.
#
#   Step 2: RQ1 11-run scalability sweep via run-rq1-scalability.sh.
#           - 5 SPLs, both family-baseline + coverage-directed in each
#             of the 11 JVM invocations (sequential, fresh JVM each).
#           - XMX=5g per JVM.
#           - Family OOM is caught inside Java (ExperimentRunner line 150)
#             and recorded as Termination Reason=OOM in
#             rq1-family-baseline.csv; JVM does not crash; orchestrator
#             continues. SAS family baseline is expected to complete at
#             5g (FTS = 16 states / 27 transitions, well within budget).
#
# Logs go to milestone-reports/metrics/logs/ — gitignored.
#
# Inter-step status output: each step prints a one-line PASS/FAIL marker
# and the size of the produced CSV(s) so progress is visible without
# tailing logs.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

METRICS_DIR="milestone-reports/metrics"
mkdir -p "$METRICS_DIR/logs"

# Settings (override via env if you must — defaults are the paper-fair values)
export XMX="${XMX:-5g}"
export RANDOM_SEED_COUNT="${RANDOM_SEED_COUNT:-100}"
export N_RUNS="${N_RUNS:-11}"

echo "================================================================"
echo "  Full RQ pipeline — orchestrator"
echo "  XMX=${XMX}  RANDOM_SEED_COUNT=${RANDOM_SEED_COUNT}  N_RUNS=${N_RUNS}"
echo "  Started: $(date '+%Y-%m-%d %H:%M:%S')"
echo "================================================================"

# ---------- Step 0: cleanup ----------
echo ""
echo "--- Step 0: clean stale CSVs in ${METRICS_DIR}/ ---"
for f in phase0-prelim-metrics.csv \
         rq1-coverage-directed.csv \
         rq1-family-baseline.csv \
         rq2-coverage-directed.csv \
         rq2-random-baseline.csv \
         rq3-efficiency.csv; do
    if [ -f "${METRICS_DIR}/${f}" ]; then
        rm "${METRICS_DIR}/${f}"
        echo "    deleted ${f}"
    fi
done
# Wipe logs but keep the directory + its .gitignore intact.
find "${METRICS_DIR}/logs" -type f \( -name "*.log" -o -name "*.out" \) -delete 2>/dev/null || true
echo "Step 0 PASS."

# ---------- Step 1: RQ2 + RQ3 single-pass ----------
echo ""
echo "--- Step 1: RQ2 + RQ3 single-pass (5 SPLs × ${RANDOM_SEED_COUNT} seeds) ---"
STEP1_START=$(date +%s)
# Run; on failure, set -e aborts the orchestrator before Step 2.
# SPLS env var (comma-separated) → space-separated positional args
# for the mutation script (which iterates PerProductMutationReportGenerator
# with each SPL name as a positional argument).
if [ -n "${SPLS:-}" ]; then
    IFS=',' read -ra SPL_ARRAY <<< "$SPLS"
    scripts/run-rq2-rq3-mutation.sh "${SPL_ARRAY[@]}"
else
    scripts/run-rq2-rq3-mutation.sh
fi
STEP1_END=$(date +%s)
STEP1_MIN=$(( (STEP1_END - STEP1_START) / 60 ))
echo "Step 1 PASS (${STEP1_MIN} min)."
echo "    rq2-coverage-directed.csv: $(wc -l < ${METRICS_DIR}/rq2-coverage-directed.csv) lines"
echo "    rq2-random-baseline.csv:   $(wc -l < ${METRICS_DIR}/rq2-random-baseline.csv) lines"
echo "    rq3-efficiency.csv:        $(wc -l < ${METRICS_DIR}/rq3-efficiency.csv) lines"

# ---------- Step 2: RQ1 11-run sweep ----------
echo ""
echo "--- Step 2: RQ1 scalability sweep (${N_RUNS} runs × 5 SPLs) ---"
STEP2_START=$(date +%s)
# run-rq1-scalability.sh loops runID=1..N_RUNS internally, each a
# fresh JVM. Family OOM is caught in Java; the script does NOT die.
# SPLS env var → first positional arg (run-rq1-scalability.sh accepts
# a single comma-separated SPL list and exports it as SPLS for the
# ExperimentRunner's listEnv("SPLS", ...) reader).
if [ -n "${SPLS:-}" ]; then
    scripts/run-rq1-scalability.sh "$SPLS"
else
    scripts/run-rq1-scalability.sh
fi
STEP2_END=$(date +%s)
STEP2_MIN=$(( (STEP2_END - STEP2_START) / 60 ))
echo "Step 2 PASS (${STEP2_MIN} min)."
echo "    rq1-coverage-directed.csv: $(wc -l < ${METRICS_DIR}/rq1-coverage-directed.csv) lines"
echo "    rq1-family-baseline.csv:   $(wc -l < ${METRICS_DIR}/rq1-family-baseline.csv) lines"

# ---------- Summary ----------
TOTAL_MIN=$(( (STEP2_END - STEP1_START) / 60 ))
echo ""
echo "================================================================"
echo "  Full pipeline PASS — total ${TOTAL_MIN} min"
echo "  Finished: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  CSV outputs in ${METRICS_DIR}/"
echo "================================================================"
