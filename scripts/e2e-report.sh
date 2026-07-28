#!/usr/bin/env bash
# Summarises a run of the e2e-tagged suite and enforces a ratchet.
#
# The e2e tag is excluded from PR CI because a large part of it is broken, so these tests rot
# unnoticed. This turns that into a number: the nightly run reports how many are broken and fails
# only when that number goes UP. A permanently red job teaches people to ignore it; a ratchet only
# speaks when something got worse.
#
# It checks how many tests RAN as well as how many failed. Counting only failures would score a run
# that never happened as a clean sweep: `mvn --fail-never` always exits 0, and a module whose tests
# fail to compile simply writes no reports, so the failure count collapses to nothing. The expected
# total also catches the opposite mistake — surefire reports left behind by an earlier, different
# run being added to this one's.
#
# scripts/e2e-baseline.txt holds both numbers. Lower `broken` in the same commit that fixes tests.
set -uo pipefail

BASELINE_FILE="${BASELINE_FILE:-scripts/e2e-baseline.txt}"

# How far the number of tests found may drift from the expected total before the result is treated
# as measuring something other than a whole suite run.
MIN_RATIO=90    # percent — below this, tests did not run
MAX_RATIO=110   # percent — above this, reports from another run are mixed in

read_setting() {
    grep -m1 -oE "^$1[[:space:]]*=[[:space:]]*[0-9]+" "$BASELINE_FILE" 2>/dev/null |
        grep -oE '[0-9]+$' || true
}

baseline=$(read_setting broken)
expected=$(read_setting tests)

if [ -z "$baseline" ] || [ -z "$expected" ]; then
    echo "::error::${BASELINE_FILE} must define both 'broken=' and 'tests='."
    exit 1
fi

counts=$(python3 - <<'PY'
import glob, sys, xml.etree.ElementTree as ET
tests = failures = errors = 0
try:
    for path in glob.glob("*/target/surefire-reports/TEST-*.xml"):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        tests += int(root.get("tests", 0))
        failures += int(root.get("failures", 0))
        errors += int(root.get("errors", 0))
except Exception as e:                                  # noqa: BLE001 - any failure must be loud
    print(f"count failed: {e}", file=sys.stderr)
    sys.exit(1)
print(tests, failures, errors)
PY
)

# Anything other than three integers means the count did not happen, which must not read as zero
# broken. Without this, an empty capture leaves the numbers empty, arithmetic treats them as 0, and
# the ratchet reports a clean sweep.
if ! [[ "$counts" =~ ^[0-9]+\ [0-9]+\ [0-9]+$ ]]; then
    echo "::error::Could not read the surefire reports; got '${counts}'."
    exit 1
fi
read -r tests failures errors <<< "$counts"

broken=$((failures + errors))
min_tests=$((expected * MIN_RATIO / 100))
max_tests=$((expected * MAX_RATIO / 100))

summary=$(cat <<EOF
## E2E suite

| | |
|---|---|
| Tests run | ${tests} (expected ~${expected}) |
| Failures | ${failures} |
| Errors | ${errors} |
| **Broken** | **${broken}** |
| Baseline | ${baseline} |
EOF
)

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    echo "$summary" >> "$GITHUB_STEP_SUMMARY"
fi
echo "$summary"

if [ "$tests" -lt "$min_tests" ]; then
    echo "::error::Only ${tests} tests ran, expected about ${expected}. The suite did not run —" \
         "a module whose tests fail to compile writes no reports, and that must not read as zero broken."
    exit 1
fi

if [ "$tests" -gt "$max_tests" ]; then
    echo "::error::${tests} tests found, expected about ${expected}. Reports from an earlier run are" \
         "being counted; re-run after 'mvn clean'."
    exit 1
fi

if [ "$broken" -gt "$baseline" ]; then
    echo "::error::${broken} e2e tests are broken, up from a baseline of ${baseline}. Something got worse."
    exit 1
fi

if [ "$broken" -lt "$baseline" ]; then
    echo "::notice::${broken} broken, down from ${baseline}. Lower 'broken' in ${BASELINE_FILE} to" \
         "${broken} to hold the gain."
fi

echo "E2E ratchet holding: ${broken} broken of ${tests}, baseline ${baseline}."
