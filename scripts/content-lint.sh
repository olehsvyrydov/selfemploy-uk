#!/usr/bin/env bash
#
# Content lint — fails the build on user-facing content defects that the live UI review
# turned up and that must not silently return.
#
# Rules enforced (each prints `file:line: [rule] reason`):
#   alert            raw `new Alert(` under src/main outside AppDialog.java (invisible-dialog risk)
#   fxml-placeholder `[icon]`-style bracket text placeholders left in FXML (icon migration leftovers)
#   enum-token       internal ALL_CAPS_UNDERSCORE enum tokens leaking into user copy
#   gbp-rate         hardcoded £<amount> rate literals in .java user strings (should be data-driven)
#   todo             TODO / XXX / FIXME left in src/main
#
# Known pre-existing violations that belong to other in-flight tasks are waived via
# scripts/content-lint-allow.txt (documented, per-rule). New violations are always caught.
#
# Usage: scripts/content-lint.sh [repo-root]
# Exit status: 0 = clean, 1 = one or more violations.

set -u

ROOT="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
ALLOW_FILE="$ROOT/scripts/content-lint-allow.txt"

# Collect the main-source roots once (all Maven modules), excluding build output.
mapfile -t MAIN_DIRS < <(find "$ROOT" -type d -path '*/src/main' -not -path '*/target/*' | sort)

violations=0

# Is a raw "file:line:content" hit waived by the allowlist for the given rule?
# Allow entries: `rule|path-substring|content-substring`  (content-substring may be empty).
is_waived() {
    local rule="$1" hit="$2"
    [ -f "$ALLOW_FILE" ] || return 1
    local line pathfrag contentfrag
    while IFS= read -r line; do
        case "$line" in ''|\#*) continue ;; esac
        IFS='|' read -r arule pathfrag contentfrag <<< "$line"
        # trim surrounding whitespace
        arule="${arule#"${arule%%[![:space:]]*}"}"; arule="${arule%"${arule##*[![:space:]]}"}"
        pathfrag="${pathfrag#"${pathfrag%%[![:space:]]*}"}"; pathfrag="${pathfrag%"${pathfrag##*[![:space:]]}"}"
        contentfrag="${contentfrag#"${contentfrag%%[![:space:]]*}"}"; contentfrag="${contentfrag%"${contentfrag##*[![:space:]]}"}"
        [ "$arule" = "$rule" ] || continue
        [ -n "$pathfrag" ] && [[ "$hit" != *"$pathfrag"* ]] && continue
        [ -n "$contentfrag" ] && [[ "$hit" != *"$contentfrag"* ]] && continue
        return 0
    done < "$ALLOW_FILE"
    return 1
}

# Emit surviving (non-waived) hits for a rule with a human-readable reason.
emit() {
    local rule="$1" reason="$2"; shift 2
    local hit rel
    while IFS= read -r hit; do
        [ -z "$hit" ] && continue
        if is_waived "$rule" "$hit"; then continue; fi
        rel="${hit#"$ROOT"/}"
        echo "$rel: [$rule] $reason"
        violations=$((violations + 1))
    done
}

for dir in "${MAIN_DIRS[@]}"; do
    # --- alert: raw JavaFX Alert outside the sanctioned wrapper ---------------------------
    emit "alert" "raw JavaFX Alert — use uk.selfemploy.ui.component.AppDialog instead" < <(
        grep -rnI --include='*.java' 'new Alert(' "$dir" 2>/dev/null | grep -v '/AppDialog.java:'
    )

    # --- fxml-placeholder: bracket icon placeholders left in FXML -------------------------
    emit "fxml-placeholder" "bracket text placeholder — replace with a real icon/label" < <(
        grep -rnIE 'text="\[[^]"]+\]"' --include='*.fxml' "$dir" 2>/dev/null
    )

    # --- enum-token: internal ALL_CAPS_UNDERSCORE tokens in user copy ---------------------
    emit "enum-token" "internal enum token in user-facing text — use a friendly label" < <(
        grep -rnIE '=[^=]*[A-Z][A-Z0-9]+_[A-Z0-9_]+' --include='messages*.properties' "$dir" 2>/dev/null
        grep -rnIE 'text="[^"]*[A-Z][A-Z0-9]+_[A-Z0-9_]+[^"]*"' --include='*.fxml' "$dir" 2>/dev/null
    )

    # --- gbp-rate: hardcoded £ rate literals in .java strings -----------------------------
    # Only non-comment lines (skip Javadoc/// examples) and only amounts with a non-zero
    # digit (so "£0.00" empty-value fallbacks are not flagged).
    emit "gbp-rate" "hardcoded £ amount in a user string — drive it from tax-year data" < <(
        grep -rnIE '£[0-9,.]*[1-9]' --include='*.java' "$dir" 2>/dev/null \
            | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)'
    )

    # --- todo: leftover markers in shipped source ----------------------------------------
    emit "todo" "TODO/XXX/FIXME left in src/main — resolve or track it, don't ship it" < <(
        grep -rnIE '\b(TODO|XXX|FIXME)\b' --include='*.java' "$dir" 2>/dev/null
    )
done

echo
if [ "$violations" -gt 0 ]; then
    echo "content-lint: FAILED with $violations violation(s)."
    exit 1
fi
echo "content-lint: OK — no content violations."
exit 0
