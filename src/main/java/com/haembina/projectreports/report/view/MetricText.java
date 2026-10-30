package com.haembina.projectreports.report.view;

import com.haembina.projectreports.l10n.Language;
import com.haembina.projectreports.l10n.Message;
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
        pair(Message.METRIC_NCLOC, ReportIcons.CODE, "ncloc", "new_lines"),
        pair(Message.METRIC_COVERAGE, ReportIcons.COVERAGE, COVERAGE, "new_coverage"),
        pair(Message.METRIC_LINES_TO_COVER, ReportIcons.TO_COVER, "lines_to_cover", "new_lines_to_cover"),
        pair(Message.METRIC_DUPLICATED_LINES, ReportIcons.DUPLICATION, "duplicated_lines", "new_duplicated_lines"),
        pair(Message.METRIC_DUPLICATED_LINES_DENSITY, ReportIcons.DUPLICATION, "duplicated_lines_density",
            "new_duplicated_lines_density"),
        pair(Message.METRIC_BUGS, ReportIcons.BUG, "bugs", "new_bugs"),
        pair(Message.METRIC_VULNERABILITIES, ReportIcons.VULNERABILITY, "vulnerabilities", "new_vulnerabilities"),
        pair(Message.METRIC_CODE_SMELLS, ReportIcons.SMELL, "code_smells", "new_code_smells"),
        pair(Message.METRIC_SECURITY_HOTSPOTS, ReportIcons.HOTSPOT, "security_hotspots", "new_security_hotspots"),
        pair(Message.METRIC_RELIABILITY_RATING, ReportIcons.BUG, "reliability_rating", "new_reliability_rating"),
        pair(Message.METRIC_SECURITY_RATING, ReportIcons.VULNERABILITY, "security_rating", "new_security_rating"),
        pair(Message.METRIC_SQALE_RATING, ReportIcons.SMELL, "sqale_rating", "new_maintainability_rating"));

    private static final Map<String, Message> CONDITION_LABELS = Map.ofEntries(
        Map.entry("ncloc", Message.METRIC_NCLOC),
        Map.entry("new_lines", Message.METRIC_NEW_LINES),
        Map.entry(COVERAGE, Message.METRIC_COVERAGE),
        Map.entry("new_coverage", Message.METRIC_NEW_COVERAGE),
        Map.entry("duplicated_lines_density", Message.METRIC_DUPLICATED_LINES_DENSITY),
        Map.entry("new_duplicated_lines_density", Message.METRIC_NEW_DUPLICATED_LINES_DENSITY),
        Map.entry("new_violations", Message.METRIC_NEW_VIOLATIONS),
        Map.entry("violations", Message.METRIC_VIOLATIONS),
        Map.entry("bugs", Message.METRIC_BUGS),
        Map.entry("new_bugs", Message.METRIC_NEW_BUGS),
        Map.entry("vulnerabilities", Message.METRIC_VULNERABILITIES),
        Map.entry("new_vulnerabilities", Message.METRIC_NEW_VULNERABILITIES),
        Map.entry("code_smells", Message.METRIC_CODE_SMELLS),
        Map.entry("new_code_smells", Message.METRIC_NEW_CODE_SMELLS),
        Map.entry("reliability_rating", Message.METRIC_RELIABILITY_RATING),
        Map.entry("new_reliability_rating", Message.METRIC_NEW_RELIABILITY_RATING),
        Map.entry("security_rating", Message.METRIC_SECURITY_RATING),
        Map.entry("new_security_rating", Message.METRIC_NEW_SECURITY_RATING),
        Map.entry("sqale_rating", Message.METRIC_SQALE_RATING),
        Map.entry("new_maintainability_rating", Message.METRIC_NEW_MAINTAINABILITY_RATING),
        Map.entry("security_hotspots_reviewed", Message.METRIC_SECURITY_HOTSPOTS_REVIEWED),
        Map.entry("new_security_hotspots_reviewed", Message.METRIC_NEW_SECURITY_HOTSPOTS_REVIEWED));

    private static final String RATINGS = "ABCDE";
    private static final String NONE = "—";

    private MetricText() {
    }

    private static Pair pair(Message label, String icon, String overall, String newCode) {
        return new Pair(label.text(), icon, overall, newCode);
    }

    public static String label(String metric) {
        Message label = CONDITION_LABELS.get(metric);
        return label == null ? metric : label.text();
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
        return NumberFormat.getIntegerInstance(Language.DEFAULT.locale()).format(number.longValue());
    }

    public static String comparator(String comparator) {
        return switch (comparator) {
            case "LT" -> Message.REPORT_AT_LEAST.text();
            case "GT" -> Message.REPORT_AT_MOST.text();
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
