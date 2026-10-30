package com.haembina.projectreports.report;

import com.haembina.projectreports.l10n.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

final class ReportTables {

    static final List<String> FILE_METRICS = List.of(
        "ncloc", "coverage", "uncovered_lines", "duplicated_lines", "duplicated_lines_density",
        FileCollector.OPEN_ISSUES);

    private ReportTables() {
    }

    record Table(String name, List<String> headers, List<List<String>> rows) {
    }

    static List<Table> of(Report report) {
        Map<ReportSection, Function<Report, Table>> tables = new EnumMap<>(ReportSection.class);
        tables.put(ReportSection.GATE, ReportTables::qualityGate);
        tables.put(ReportSection.MEASURES, ReportTables::measures);
        tables.put(ReportSection.ISSUES, ReportTables::issues);
        tables.put(ReportSection.HOTSPOTS, ReportTables::hotspots);
        tables.put(ReportSection.FILES, ReportTables::files);
        tables.put(ReportSection.COVERAGE, ReportTables::coverage);
        tables.put(ReportSection.DUPLICATIONS, ReportTables::duplications);
        return tables.entrySet().stream().filter(entry -> report.has(entry.getKey()))
            .map(entry -> entry.getValue().apply(report)).toList();
    }

    private static Table coverage(Report report) {
        String uncovered = Message.REPORT_LINE_UNCOVERED.text();
        String partlyCovered = Message.REPORT_LINE_PARTLY_COVERED.text();
        List<List<String>> rows = new ArrayList<>();
        for (Report.LineCoverage lines : report.coverage()) {
            lines.uncovered().forEach(line -> rows.add(cells(lines.path(), line.toString(), uncovered, "", "")));
            lines.partial().forEach(line -> rows.add(cells(lines.path(), String.valueOf(line.line()), partlyCovered,
                String.valueOf(line.covered()), String.valueOf(line.conditions()))));
        }
        return new Table("coverage", headers(Message.COLUMN_FILE, Message.COLUMN_LINE, Message.COLUMN_STATUS,
            Message.COLUMN_COVERED_CONDITIONS, Message.COLUMN_CONDITIONS), rows);
    }

    private static Table duplications(Report report) {
        List<List<String>> rows = new ArrayList<>();
        for (Report.Duplication block : report.duplications()) {
            for (Report.Place copy : block.copies()) {
                rows.add(cells(block.path(), String.valueOf(block.from()), String.valueOf(block.to()), copy.path(),
                    String.valueOf(copy.from()), String.valueOf(copy.to())));
            }
        }
        return new Table("duplications", headers(Message.COLUMN_FILE, Message.COLUMN_FROM, Message.COLUMN_TO,
            Message.COLUMN_COPY_FILE, Message.COLUMN_COPY_FROM, Message.COLUMN_COPY_TO), rows);
    }

    static String title(Report report) {
        ReportTarget target = report.target();
        if (target.pullRequest() != null) {
            return target.project() + ", pull request " + target.pullRequest();
        }
        if (target.branch() != null) {
            return target.project() + ", branch " + target.branch();
        }
        return target.project() + ", main branch";
    }

    private static Table qualityGate(Report report) {
        List<List<String>> rows = report.qualityGate().conditions().stream()
            .map(condition -> cells(condition.metric(), condition.comparator(), condition.threshold(),
                condition.actual(), condition.status()))
            .toList();
        return new Table("quality-gate", headers(Message.COLUMN_METRIC, Message.COLUMN_COMPARATOR,
            Message.COLUMN_THRESHOLD, Message.COLUMN_ACTUAL, Message.COLUMN_STATUS), rows);
    }

    private static Table measures(Report report) {
        List<List<String>> rows = report.measures().stream()
            .map(measure -> cells(measure.metric(), measure.value()))
            .toList();
        return new Table("measures", headers(Message.COLUMN_METRIC, Message.COLUMN_VALUE), rows);
    }

    private static Table issues(Report report) {
        List<List<String>> rows = report.issues().stream()
            .map(issue -> cells(issue.severity(), issue.type(), issue.rule(), issue.file(), line(issue.line()),
                issue.message(), issue.status(), issue.key()))
            .toList();
        return new Table("issues", headers(Message.COLUMN_SEVERITY, Message.COLUMN_TYPE, Message.COLUMN_RULE,
            Message.COLUMN_FILE, Message.COLUMN_LINE, Message.COLUMN_MESSAGE, Message.COLUMN_STATUS,
            Message.COLUMN_KEY), rows);
    }

    private static Table hotspots(Report report) {
        List<List<String>> rows = report.hotspots().stream()
            .map(hotspot -> cells(hotspot.vulnerabilityProbability(), hotspot.securityCategory(), hotspot.rule(),
                hotspot.file(), line(hotspot.line()), hotspot.message(), hotspot.status(), hotspot.key()))
            .toList();
        return new Table("hotspots", headers(Message.COLUMN_PROBABILITY, Message.COLUMN_CATEGORY,
            Message.COLUMN_RULE, Message.COLUMN_FILE, Message.COLUMN_LINE, Message.COLUMN_MESSAGE,
            Message.COLUMN_STATUS, Message.COLUMN_KEY), rows);
    }

    private static Table files(Report report) {
        List<List<String>> rows = report.files().stream()
            .map(file -> Stream.concat(Stream.of(file.path()),
                    FILE_METRICS.stream().map(metric -> Objects.requireNonNullElse(file.measure(metric), "")))
                .toList())
            .toList();
        return new Table("files", headers(Message.COLUMN_FILE, Message.METRIC_NCLOC, Message.METRIC_COVERAGE,
            Message.COLUMN_UNCOVERED_LINES, Message.METRIC_DUPLICATED_LINES, Message.METRIC_DUPLICATED_LINES_DENSITY,
            Message.COLUMN_OPEN_ISSUES), rows);
    }

    private static List<String> headers(Message... columns) {
        return Arrays.stream(columns).map(Message::text).toList();
    }

    private static String line(Integer line) {
        return line == null ? "" : line.toString();
    }

    private static List<String> cells(String... values) {
        return Arrays.stream(values).map(value -> Objects.requireNonNullElse(value, "")).toList();
    }
}
