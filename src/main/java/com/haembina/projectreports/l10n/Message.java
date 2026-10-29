package com.haembina.projectreports.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public enum Message {
    PAGE_TITLE("page.title"),

    API_CONTROLLER("api.controller"),
    API_DOWNLOAD("api.download"),
    API_PROJECT("api.project"),
    API_BRANCH("api.branch"),
    API_PULL_REQUEST("api.pull_request"),
    API_FORMAT("api.format"),
    API_SECTIONS("api.sections"),
    API_NO_BRANCH_PLUGIN("api.no_branch_plugin"),
    API_BRANCH_AND_PULL_REQUEST("api.branch_and_pull_request"),
    API_UNKNOWN_FORMAT("api.unknown_format"),
    API_UNKNOWN_SECTION("api.unknown_section"),

    SETTINGS_BRANCH_URL("settings.branch_url.name"),
    SETTINGS_BRANCH_URL_DESCRIPTION("settings.branch_url.description"),
    SETTINGS_PULL_REQUEST_URL("settings.pull_request_url.name"),
    SETTINGS_PULL_REQUEST_URL_DESCRIPTION("settings.pull_request_url.description"),

    REPORT_DEVELOPED_BY("report.developed_by"),
    REPORT_OPEN_IN_SONARQUBE("report.open_in_sonarqube"),
    REPORT_SOURCE_HIDDEN("report.source_hidden"),
    REPORT_GATE("report.gate"),
    REPORT_MAIN_BRANCH("report.scope.main_branch"),
    REPORT_BRANCH("report.scope.branch"),
    REPORT_PULL_REQUEST("report.scope.pull_request"),
    REPORT_PULL_REQUEST_TITLED("report.scope.pull_request_titled"),
    REPORT_PASSED("report.status.passed"),
    REPORT_FAILED("report.status.failed"),
    REPORT_NOT_COMPUTED("report.status.not_computed"),
    REPORT_AT_LEAST("report.comparator.lt"),
    REPORT_AT_MOST("report.comparator.gt"),
    REPORT_PARTLY_COVERED_LINE("report.partly_covered_line"),
    REPORT_LINE_UNCOVERED("report.line.uncovered"),
    REPORT_LINE_PARTLY_COVERED("report.line.partly_covered"),

    HEADING_CONDITIONS("report.heading.conditions"),
    HEADING_MEASURES("report.heading.measures"),
    HEADING_ISSUES("report.heading.issues"),
    HEADING_HOTSPOTS("report.heading.hotspots"),
    HEADING_FILES("report.heading.files"),
    HEADING_COVERAGE("report.heading.coverage"),
    HEADING_DUPLICATIONS("report.heading.duplications"),

    EMPTY_CONDITIONS("report.empty.conditions"),
    EMPTY_MEASURES("report.empty.measures"),
    EMPTY_ISSUES("report.empty.issues"),
    EMPTY_HOTSPOTS("report.empty.hotspots"),
    EMPTY_FILES("report.empty.files"),
    EMPTY_COVERAGE("report.empty.coverage"),
    EMPTY_DUPLICATIONS("report.empty.duplications"),

    COLUMN_CONDITION("report.column.condition"),
    COLUMN_REQUIRED("report.column.required"),
    COLUMN_ACTUAL("report.column.actual"),
    COLUMN_STATUS("report.column.status"),
    COLUMN_METRIC("report.column.metric"),
    COLUMN_OVERALL("report.column.overall"),
    COLUMN_NEW_CODE("report.column.new_code"),
    COLUMN_SEVERITY("report.column.severity"),
    COLUMN_TYPE("report.column.type"),
    COLUMN_ISSUE("report.column.issue"),
    COLUMN_PROBABILITY("report.column.probability"),
    COLUMN_CATEGORY("report.column.category"),
    COLUMN_HOTSPOT("report.column.hotspot"),
    COLUMN_FILE("report.column.file"),
    COLUMN_UNCOVERED_LINES("report.column.uncovered_lines"),
    COLUMN_PARTLY_COVERED("report.column.partly_covered"),
    COLUMN_LINES("report.column.lines"),
    COLUMN_ALSO_IN("report.column.also_in"),
    COLUMN_OPEN_ISSUES("report.column.open_issues"),
    COLUMN_RULE("report.column.rule"),
    COLUMN_LOCATION("report.column.location"),
    COLUMN_MESSAGE("report.column.message"),
    COLUMN_LINE("report.column.line"),
    COLUMN_KEY("report.column.key"),
    COLUMN_COVERED_CONDITIONS("report.column.covered_conditions"),
    COLUMN_CONDITIONS("report.column.conditions"),
    COLUMN_FROM("report.column.from"),
    COLUMN_TO("report.column.to"),
    COLUMN_COPY_FILE("report.column.copy_file"),
    COLUMN_COPY_FROM("report.column.copy_from"),
    COLUMN_COPY_TO("report.column.copy_to"),
    COLUMN_COMPARATOR("report.column.comparator"),
    COLUMN_THRESHOLD("report.column.threshold"),
    COLUMN_VALUE("report.column.value"),

    METRIC_NCLOC("report.metric.ncloc"),
    METRIC_NEW_LINES("report.metric.new_lines"),
    METRIC_COVERAGE("report.metric.coverage"),
    METRIC_NEW_COVERAGE("report.metric.new_coverage"),
    METRIC_LINES_TO_COVER("report.metric.lines_to_cover"),
    METRIC_DUPLICATED_LINES("report.metric.duplicated_lines"),
    METRIC_DUPLICATED_LINES_DENSITY("report.metric.duplicated_lines_density"),
    METRIC_NEW_DUPLICATED_LINES_DENSITY("report.metric.new_duplicated_lines_density"),
    METRIC_VIOLATIONS("report.metric.violations"),
    METRIC_NEW_VIOLATIONS("report.metric.new_violations"),
    METRIC_BUGS("report.metric.bugs"),
    METRIC_NEW_BUGS("report.metric.new_bugs"),
    METRIC_VULNERABILITIES("report.metric.vulnerabilities"),
    METRIC_NEW_VULNERABILITIES("report.metric.new_vulnerabilities"),
    METRIC_CODE_SMELLS("report.metric.code_smells"),
    METRIC_NEW_CODE_SMELLS("report.metric.new_code_smells"),
    METRIC_SECURITY_HOTSPOTS("report.metric.security_hotspots"),
    METRIC_RELIABILITY_RATING("report.metric.reliability_rating"),
    METRIC_NEW_RELIABILITY_RATING("report.metric.new_reliability_rating"),
    METRIC_SECURITY_RATING("report.metric.security_rating"),
    METRIC_NEW_SECURITY_RATING("report.metric.new_security_rating"),
    METRIC_SQALE_RATING("report.metric.sqale_rating"),
    METRIC_NEW_MAINTAINABILITY_RATING("report.metric.new_maintainability_rating"),
    METRIC_SECURITY_HOTSPOTS_REVIEWED("report.metric.security_hotspots_reviewed"),
    METRIC_NEW_SECURITY_HOTSPOTS_REVIEWED("report.metric.new_security_hotspots_reviewed");

    static final String PREFIX = "haembinaprojectreports.";
    static final String FOLDER = "/org/sonar/l10n/";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d{1,2})\\}");

    private final String key;

    Message(String key) {
        this.key = PREFIX + key;
    }

    public String key() {
        return key;
    }

    public String text(Object... values) {
        return in(Language.DEFAULT, values);
    }

    public String in(Language language, Object... values) {
        return PLACEHOLDER.matcher(Tables.ALL.get(language).get(key)).replaceAll(found -> {
            int index = Integer.parseInt(found.group(1));
            return Matcher.quoteReplacement(index < values.length ? String.valueOf(values[index]) : found.group());
        });
    }

    static Map<String, String> table(Language language) {
        return checked(language, read(Message.class.getResourceAsStream(FOLDER + language.table()), language));
    }

    static Map<String, String> read(@Nullable InputStream resource, Language language) {
        if (resource == null) {
            throw new IllegalStateException("The plugin jar holds no " + FOLDER + language.table());
        }
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        Map<String, String> table = new HashMap<>();
        properties.stringPropertyNames().forEach(name -> table.put(name, properties.getProperty(name)));
        return Map.copyOf(table);
    }

    static Map<String, String> checked(Language language, Map<String, String> table) {
        List<String> missing = Arrays.stream(values()).map(Message::key).filter(name -> !table.containsKey(name))
            .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(language.table() + " has no " + String.join(", ", missing));
        }
        return table;
    }

    private static final class Tables {

        static final Map<Language, Map<String, String>> ALL = load();

        private Tables() {
        }

        private static Map<Language, Map<String, String>> load() {
            Map<Language, Map<String, String>> tables = new EnumMap<>(Language.class);
            for (Language language : Language.values()) {
                tables.put(language, table(language));
            }
            return tables;
        }
    }
}
