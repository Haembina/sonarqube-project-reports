package com.haembina.projectreports.report.view;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

public final class MetricText {

    private static final String COVERAGE = "coverage";

    public record Pair(String label, String icon, String overall, String newCode) {
    }

    static final List<Pair> PAIRS = List.of(
        new Pair("Lines of code", ReportIcons.CODE, "ncloc", "new_lines"),
        new Pair("Coverage", ReportIcons.COVERAGE, COVERAGE, "new_coverage"),
        new Pair("Lines to cover", ReportIcons.TO_COVER, "lines_to_cover", "new_lines_to_cover"),
        new Pair("Duplicated lines", ReportIcons.DUPLICATION, "duplicated_lines", "new_duplicated_lines"),
        new Pair("Duplicated lines (%)", ReportIcons.DUPLICATION, "duplicated_lines_density",
            "new_duplicated_lines_density"),
        new Pair("Bugs", ReportIcons.BUG, "bugs", "new_bugs"),
        new Pair("Vulnerabilities", ReportIcons.VULNERABILITY, "vulnerabilities", "new_vulnerabilities"),
        new Pair("Code smells", ReportIcons.SMELL, "code_smells", "new_code_smells"),
        new Pair("Security hotspots", ReportIcons.HOTSPOT, "security_hotspots", "new_security_hotspots"),
        new Pair("Reliability rating", ReportIcons.BUG, "reliability_rating", "new_reliability_rating"),
        new Pair("Security rating", ReportIcons.VULNERABILITY, "security_rating", "new_security_rating"),
        new Pair("Maintainability rating", ReportIcons.SMELL, "sqale_rating", "new_maintainability_rating"));

    private static final Map<String, String> CONDITION_LABELS = Map.ofEntries(
        Map.entry("ncloc", "Lines of code"),
        Map.entry("new_lines", "New lines"),
        Map.entry(COVERAGE, "Coverage"),
        Map.entry("new_coverage", "Coverage on new code"),
        Map.entry("duplicated_lines_density", "Duplicated lines (%)"),
        Map.entry("new_duplicated_lines_density", "Duplicated lines on new code (%)"),
        Map.entry("new_violations", "Issues on new code"),
        Map.entry("violations", "Issues"),
        Map.entry("bugs", "Bugs"),
        Map.entry("new_bugs", "Bugs on new code"),
        Map.entry("vulnerabilities", "Vulnerabilities"),
        Map.entry("new_vulnerabilities", "Vulnerabilities on new code"),
        Map.entry("code_smells", "Code smells"),
        Map.entry("new_code_smells", "Code smells on new code"),
        Map.entry("reliability_rating", "Reliability rating"),
        Map.entry("new_reliability_rating", "Reliability rating on new code"),
        Map.entry("security_rating", "Security rating"),
        Map.entry("new_security_rating", "Security rating on new code"),
        Map.entry("sqale_rating", "Maintainability rating"),
        Map.entry("new_maintainability_rating", "Maintainability rating on new code"),
        Map.entry("security_hotspots_reviewed", "Security hotspots reviewed"),
        Map.entry("new_security_hotspots_reviewed", "Security hotspots reviewed on new code"));

    private static final String RATINGS = "ABCDE";
    private static final String NONE = "—";

    private MetricText() {
    }

    public static String label(String metric) {
        return CONDITION_LABELS.getOrDefault(metric, metric);
    }

    public static boolean isRating(String metric) {
        return metric.endsWith("_rating");
    }

    public static boolean isPercent(String metric) {
        return metric.contains(COVERAGE) || metric.endsWith("_density") || metric.endsWith("_reviewed");
    }

    public static String value(String metric, @Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        BigDecimal number;
        try {
            number = new BigDecimal(raw);
        } catch (NumberFormatException notANumber) {
            return raw;
        }
        if (isRating(metric)) {
            return String.valueOf(RATINGS.charAt(Math.clamp(number.longValue() - 1, 0, RATINGS.length() - 1)));
        }
        if (isPercent(metric)) {
            return number.stripTrailingZeros().toPlainString() + " %";
        }
        return NumberFormat.getIntegerInstance(Locale.ENGLISH).format(number.longValue());
    }

    public static String comparator(String comparator) {
        return switch (comparator) {
            case "LT" -> "at least";
            case "GT" -> "at most";
            default -> comparator;
        };
    }

    public static String words(@Nullable String constant) {
        if (constant == null || constant.isBlank()) {
            return NONE;
        }
        String lower = constant.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
