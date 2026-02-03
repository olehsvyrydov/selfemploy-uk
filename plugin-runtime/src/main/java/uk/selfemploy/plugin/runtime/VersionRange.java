package uk.selfemploy.plugin.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and matches semantic version ranges.
 *
 * <p>Supports npm-style version range syntax:</p>
 * <ul>
 *   <li>{@code 1.0.0} - Exact version match</li>
 *   <li>{@code ^1.0.0} - Compatible with 1.x.x (>=1.0.0 <2.0.0)</li>
 *   <li>{@code ^0.1.0} - Compatible with 0.1.x (>=0.1.0 <0.2.0)</li>
 *   <li>{@code ~1.2.0} - Patch updates only (>=1.2.0 <1.3.0)</li>
 *   <li>{@code >=1.0.0} - Minimum version</li>
 *   <li>{@code <2.0.0} - Maximum version (exclusive)</li>
 *   <li>{@code <=2.0.0} - Maximum version (inclusive)</li>
 *   <li>{@code >1.0.0} - Greater than version</li>
 *   <li>{@code >=1.0.0 <2.0.0} - Range with multiple constraints</li>
 * </ul>
 */
public final class VersionRange {

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z0-9.-]+))?(?:\\+([a-zA-Z0-9.-]+))?$"
    );

    private static final Pattern COMPARATOR_PATTERN = Pattern.compile(
        "(>=|<=|>|<|=)?\\s*(\\d+\\.\\d+\\.\\d+(?:-[a-zA-Z0-9.-]+)?)"
    );

    private static final Pattern CARET_PATTERN = Pattern.compile("^\\^(.+)$");
    private static final Pattern TILDE_PATTERN = Pattern.compile("^~(.+)$");

    private final String rangeString;
    private final List<Constraint> constraints;

    /**
     * Parses a version range string.
     *
     * @param rangeString the version range to parse
     * @throws IllegalArgumentException if the range is invalid
     */
    public VersionRange(String rangeString) {
        if (rangeString == null || rangeString.isBlank()) {
            throw new IllegalArgumentException("Version range must not be null or blank");
        }
        this.rangeString = rangeString.trim();
        this.constraints = parseRange(this.rangeString);
    }

    /**
     * Checks if the given version satisfies this range.
     *
     * @param version the version to check
     * @return true if the version satisfies all constraints
     */
    public boolean matches(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }

        int[] parsed = parseVersion(version.trim());
        if (parsed == null) {
            return false;
        }

        for (Constraint constraint : constraints) {
            if (!constraint.matches(parsed)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the original range string.
     */
    public String getRangeString() {
        return rangeString;
    }

    private List<Constraint> parseRange(String range) {
        List<Constraint> result = new ArrayList<>();

        // Handle caret range: ^1.0.0
        Matcher caretMatcher = CARET_PATTERN.matcher(range);
        if (caretMatcher.matches()) {
            String version = caretMatcher.group(1);
            int[] parsed = parseVersion(version);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid version in caret range: " + version);
            }

            // ^1.2.3 means >=1.2.3 <2.0.0 (or <0.2.0 if major is 0)
            result.add(new Constraint(Operator.GTE, parsed));
            if (parsed[0] == 0) {
                // ^0.x.y is more restrictive
                result.add(new Constraint(Operator.LT, new int[]{0, parsed[1] + 1, 0}));
            } else {
                result.add(new Constraint(Operator.LT, new int[]{parsed[0] + 1, 0, 0}));
            }
            return result;
        }

        // Handle tilde range: ~1.2.0
        Matcher tildeMatcher = TILDE_PATTERN.matcher(range);
        if (tildeMatcher.matches()) {
            String version = tildeMatcher.group(1);
            int[] parsed = parseVersion(version);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid version in tilde range: " + version);
            }

            // ~1.2.3 means >=1.2.3 <1.3.0
            result.add(new Constraint(Operator.GTE, parsed));
            result.add(new Constraint(Operator.LT, new int[]{parsed[0], parsed[1] + 1, 0}));
            return result;
        }

        // Handle comparator ranges: >=1.0.0 <2.0.0
        Matcher comparatorMatcher = COMPARATOR_PATTERN.matcher(range);
        while (comparatorMatcher.find()) {
            String op = comparatorMatcher.group(1);
            String version = comparatorMatcher.group(2);
            int[] parsed = parseVersion(version);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid version: " + version);
            }

            Operator operator = parseOperator(op);
            result.add(new Constraint(operator, parsed));
        }

        if (result.isEmpty()) {
            // Try exact version match
            int[] parsed = parseVersion(range);
            if (parsed != null) {
                result.add(new Constraint(Operator.EQ, parsed));
            } else {
                throw new IllegalArgumentException("Invalid version range: " + range);
            }
        }

        return result;
    }

    private int[] parseVersion(String version) {
        Matcher matcher = SEMVER_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return null;
        }
        return new int[]{
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3))
        };
    }

    private Operator parseOperator(String op) {
        if (op == null || op.isEmpty() || op.equals("=")) {
            return Operator.EQ;
        }
        return switch (op) {
            case ">=" -> Operator.GTE;
            case "<=" -> Operator.LTE;
            case ">" -> Operator.GT;
            case "<" -> Operator.LT;
            default -> Operator.EQ;
        };
    }

    @Override
    public String toString() {
        return "VersionRange[" + rangeString + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VersionRange other)) return false;
        return rangeString.equals(other.rangeString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rangeString);
    }

    private enum Operator {
        EQ, GT, GTE, LT, LTE
    }

    private record Constraint(Operator operator, int[] version) {
        boolean matches(int[] target) {
            int cmp = compareVersions(target, version);
            return switch (operator) {
                case EQ -> cmp == 0;
                case GT -> cmp > 0;
                case GTE -> cmp >= 0;
                case LT -> cmp < 0;
                case LTE -> cmp <= 0;
            };
        }

        private int compareVersions(int[] a, int[] b) {
            for (int i = 0; i < 3; i++) {
                if (a[i] != b[i]) {
                    return Integer.compare(a[i], b[i]);
                }
            }
            return 0;
        }
    }
}
