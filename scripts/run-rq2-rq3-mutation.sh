#!/bin/bash
# run-rq2-rq3-mutation.sh — RQ2 + RQ3 single-pass runner orchestration.
#
# Runs PerProductMutationReportGenerator ONCE per SPL (mutation score
# and efficiency are deterministic given the fixed random-baseline seed
# set, so 11× repetition is wasteful). Appends to:
#   milestone-reports/metrics/rq2-coverage-directed.csv
#   milestone-reports/metrics/rq2-random-baseline.csv
#   milestone-reports/metrics/rq3-efficiency.csv
# and emits the per-product MD/HTML mutation reports as before.
#
# Usage:
#   scripts/run-rq2-rq3-mutation.sh                              # all 5 SPLs
#   scripts/run-rq2-rq3-mutation.sh SVM eMail                    # selected
#   RANDOM_SEED_COUNT=10 scripts/run-rq2-rq3-mutation.sh         # faster (debugging)
#   XMX=8g scripts/run-rq2-rq3-mutation.sh                       # bigger heap

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

XMX="${XMX:-4g}"
LOG_DIR="milestone-reports/metrics/logs/rq2-rq3"
mkdir -p "$LOG_DIR"

SPLS_ARGS=("$@")
if [ ${#SPLS_ARGS[@]} -eq 0 ]; then
    SPLS_ARGS=("SVM" "eMail" "Elevator" "BankAccountv2" "StudentAttendanceSystem")
fi

echo "=== RQ2+RQ3 single-pass run: SPLS=${SPLS_ARGS[*]}, RANDOM_SEED_COUNT=${RANDOM_SEED_COUNT:-100}, XMX=${XMX} ==="
echo "    CSV dir: milestone-reports/metrics/"
echo "    Log dir: ${LOG_DIR}/"

mvn -q -pl vibes-testgeneration -am test-compile

CP_FILE="$(mktemp)"
trap 'rm -f "$CP_FILE"' EXIT
mvn -q -pl vibes-testgeneration dependency:build-classpath \
    -DincludeScope=test -Dmdep.outputFile="$CP_FILE"
CP="vibes-testgeneration/target/classes:vibes-testgeneration/target/test-classes:$(cat "$CP_FILE")"

LOG_FILE="${LOG_DIR}/$(date +%Y%m%d-%H%M%S).log"
echo "--- mutation run -> ${LOG_FILE}"
java -Xmx"$XMX" -XX:+UseG1GC \
    -cp "$CP" \
    be.vibes.testgeneration.experiment.PerProductMutationReportGenerator \
    "${SPLS_ARGS[@]}" \
    > "$LOG_FILE" 2>&1

echo "=== RQ2+RQ3 complete ==="
for f in rq2-coverage-directed rq2-random-baseline rq3-efficiency; do
    p="milestone-reports/metrics/${f}.csv"
    if [ -f "$p" ]; then
        echo "    $(wc -l < "$p") lines in ${f}.csv"
    fi
done
