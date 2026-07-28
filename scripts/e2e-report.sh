#!/usr/bin/env bash
# Summarises a run of the e2e-tagged suite and enforces a ratchet.
#
# The e2e tag is excluded from PR CI because a large part of it is broken, so these tests rot
# unnoticed. This turns that into a number: the nightly run reports how many are broken and fails
# only when that number goes UP. A permanently red job teaches people to ignore it; a ratchet only
# speaks when something got worse.
#
# When the count goes down, lower scripts/e2e-baseline.txt in the same commit that fixed them.
set -uo pipefail

BASELINE_FILE="${BASELINE_FILE:-scripts/e2e-baseline.txt}"

read -r tests failures errors < <(
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
tests = failures = errors = 0
for path in glob.glob("*/target/surefire-reports/TEST-*.xml"):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    tests += int(root.get("tests", 0))
    failures += int(root.get("failures", 0))
    errors += int(root.get("errors", 0))
print(tests, failures, errors)
PY
)

broken=$((failures + errors))
# First bare number in the file; the rest of it is explanation for whoever changes it.
baseline=$(grep -m1 -oE '^[0-9]+' "$BASELINE_FILE" 2>/dev/null || true)
baseline=${baseline:-0}

summary=$(cat <<EOF
## E2E suite

| | |
|---|---|
| Tests run | ${tests} |
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

if [ "$tests" -eq 0 ]; then
    echo "::error::No e2e results were produced — the run did not execute any tests."
    exit 1
fi

if [ "$broken" -gt "$baseline" ]; then
    echo "::error::${broken} e2e tests are broken, up from a baseline of ${baseline}. Something got worse."
    exit 1
fi

if [ "$broken" -lt "$baseline" ]; then
    echo "::notice::${broken} broken, down from ${baseline}. Lower ${BASELINE_FILE} to ${broken} to hold the gain."
fi

echo "E2E ratchet holding: ${broken} broken, baseline ${baseline}."
