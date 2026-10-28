package com.haembina.projectreports.report.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public final class ReportIcons {

    public static final String GATE = "shield-check-section";
    public static final String MEASURES = "chart-bar-section";
    public static final String ISSUES = "alert-circle-section";
    public static final String HOTSPOTS = "flame-section";
    public static final String FILES = "files-section";
    public static final String COVERAGE_LINES = "umbrella-section";
    public static final String DUPLICATED_BLOCKS = "copy-section";
    public static final String PASS = "check-pass";
    public static final String FAIL = "x-fail";
    public static final String NONE = "minus-none";

    static final String CODE = "code-row";
    static final String COVERAGE = "umbrella-row";
    static final String TO_COVER = "list-check-row";
    static final String DUPLICATION = "copy-row";
    static final String BUG = "bug-row";
    static final String VULNERABILITY = "lock-open-row";
    static final String SMELL = "radioactive-row";
    static final String HOTSPOT = "flame-row";

    private static final Map<String, String> TYPES = Map.of("BUG", BUG, "VULNERABILITY", VULNERABILITY,
        "CODE_SMELL", SMELL, "SECURITY_HOTSPOT", HOTSPOT);
    private static final Map<String, String> SEVERITIES = Map.of("BLOCKER", "circle-x-row",
        "CRITICAL", "alert-octagon-row", "MAJOR", "alert-triangle-row", "MINOR", "circle-arrow-down-row",
        "INFO", "info-circle-row");

    public static final List<String> ALL = List.of(GATE, MEASURES, ISSUES, HOTSPOTS, FILES, COVERAGE_LINES,
        DUPLICATED_BLOCKS, PASS, FAIL, NONE, CODE,
        COVERAGE, TO_COVER, DUPLICATION, BUG, VULNERABILITY, SMELL, HOTSPOT, "circle-x-row", "alert-octagon-row",
        "alert-triangle-row", "circle-arrow-down-row", "info-circle-row");

    private ReportIcons() {
    }

    public static String forTone(String tone) {
        return switch (tone) {
            case "pass" -> PASS;
            case "fail" -> FAIL;
            default -> NONE;
        };
    }

    static String forType(@Nullable String type) {
        return TYPES.getOrDefault(Objects.requireNonNullElse(type, ""), "");
    }

    static String forSeverity(@Nullable String severity) {
        return SEVERITIES.getOrDefault(Objects.requireNonNullElse(severity, ""), "");
    }
}
