#!/bin/bash
# run-rq1-scalability.sh — RQ1 scalability runner orchestration.
#
# Launches the ExperimentRunner main 11 times sequentially, each in a
# FRESH JVM, with runID=1..11 in the environment. Each invocation appends
# to milestone-reports/metrics/rq1-coverage-directed.csv and
# milestone-reports/metrics/rq1-family-baseline.csv; the header is
# written only on the first call (when the file is empty), per the
# ESG-Fx TestPipelineMeasurementWriter convention.
#
# Fresh JVMs are mandatory: 11 iterations inside one JVM would
# contaminate timings with warm-up effects, generational-GC carry-over,
# and (on long SAS runs) progressively growing tenured-gen pressure.
# Each JVM gets its own clean cache state.
#
# Usage:
#   scripts/run-rq1-scalability.sh                # all 5 SPLs, runs 1..11
#   scripts/run-rq1-scalability.sh "SVM,eMail"    # filter SPLs (comma list)
#   N_RUNS=3 scripts/run-rq1-scalability.sh        # 3 runs instead of 11
#   XMX=8g scripts/run-rq1-scalability.sh          # bigger heap
#
# Output: CSV files in milestone-reports/metrics/ (append mode), per-run
# logs in milestone-reports/metrics/logs/rq1/runID-NN.log .
#
# Inspired by the user's ESG-Fx-side files/bashscripts/RQ2_*.sh.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

N_RUNS="${N_RUNS:-11}"
XMX="${XMX:-4g}"
SPL_FILTER="${1:-}"
LOG_DIR="milestone-reports/metrics/logs/rq1"
mkdir -p "$LOG_DIR"

echo "=== RQ1 scalability run: ${N_RUNS} runs, XMX=${XMX}, SPLS=${SPL_FILTER:-<all>} ==="
echo "    CSV dir: milestone-reports/metrics/"
echo "    Log dir: ${LOG_DIR}/"

# Build the project once up front so we don't measure compile time.
mvn -q -pl vibes-testgeneration -am test-compile

# Resolve classpath via Maven once and cache it.
CP_FILE="$(mktemp)"
trap 'rm -f "$CP_FILE"' EXIT
mvn -q -pl vibes-testgeneration dependency:build-classpath \
    -DincludeScope=test -Dmdep.outputFile="$CP_FILE"
CP="vibes-testgeneration/target/classes:vibes-testgeneration/target/test-classes:$(cat "$CP_FILE")"

# Optional SPL filter env var (read by ExperimentRunner).
if [ -n "$SPL_FILTER" ]; then
    export SPLS="$SPL_FILTER"
fi

for runID in $(seq 1 "$N_RUNS"); do
    LOG_FILE="${LOG_DIR}/runID-$(printf '%02d' "$runID").log"
    echo "--- run ${runID}/${N_RUNS} -> ${LOG_FILE}"
    runID="$runID" java -Xmx"$XMX" -XX:+UseG1GC \
        -cp "$CP" \
        be.vibes.testgeneration.experiment.ExperimentRunner \
        > "$LOG_FILE" 2>&1
    echo "    done"
done

echo "=== RQ1 complete: $(wc -l < milestone-reports/metrics/rq1-coverage-directed.csv) lines in rq1-coverage-directed.csv ==="
echo "                $(wc -l < milestone-reports/metrics/rq1-family-baseline.csv) lines in rq1-family-baseline.csv"
