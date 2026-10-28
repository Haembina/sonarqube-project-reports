package com.haembina.projectreports.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

final class ReportTables {

    private static final String RULE = "Rule";
    private static final String FILE = "File";
    private static final String LINE = "Line";
    private static final String MESSAGE = "Message";
    private static final String STATUS = "Status";
    private static final String KEY = "Key";

    static final List<String> FILE_METRICS = List.of(
        "ncloc", "coverage", "uncovered_lines", "duplicated_lines", "duplicated_lines_density",
        FileCollector.OPEN_ISSUES);

    private ReportTables() {
    }

    record Table(String name, String title, List<String> headers, List<List<String>> rows) {
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
        List<List<String>> rows = new ArrayList<>();
        for (Report.LineCoverage lines : report.coverage()) {
            lines.uncovered().forEach(line -> rows.add(cells(lines.path(), line.toString(), "uncovered", "", "")));
            lines.partial().forEach(line -> rows.add(cells(lines.path(), String.valueOf(line.line()), "partly covered",
                String.valueOf(line.covered()), String.valueOf(line.conditions()))));
        }
        return new Table("coverage", "Uncovered lines (" + rows.size() + ")",
            List.of(FILE, LINE, STATUS, "Covered conditions", "Conditions"), rows);
    }

    private static Table duplications(Report report) {
        List<List<String>> rows = new ArrayList<>();
        for (Report.Duplication block : report.duplications()) {
            for (Report.Place copy : block.copies()) {
                rows.add(cells(block.path(), String.valueOf(block.from()), String.valueOf(block.to()), copy.path(),
                    String.valueOf(copy.from()), String.valueOf(copy.to())));
            }
        }
        return new Table("duplications", "Duplicated blocks (" + rows.size() + ")",
            List.of(FILE, "From", "To", "Copy file", "Copy from", "Copy to"), rows);
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

    static String gateStatus(Report report) {
        return Objects.requireNonNullElse(report.qualityGate().status(), "NONE");
    }

    private static Table qualityGate(Report report) {
        List<List<String>> rows = report.qualityGate().conditions().stream()
            .map(condition -> cells(condition.metric(), condition.comparator(), condition.threshold(),
                condition.actual(), condition.status()))
            .toList();
        return new Table("quality-gate", "Quality gate: " + gateStatus(report),
            List.of("Metric", "Comparator", "Threshold", "Actual", STATUS), rows);
    }

    private static Table measures(Report report) {
        List<List<String>> rows = report.measures().stream()
            .map(measure -> cells(measure.metric(), measure.value()))
            .toList();
        return new Table("measures", "Measures", List.of("Metric", "Value"), rows);
    }

    private static Table issues(Report report) {
        List<List<String>> rows = report.issues().stream()
            .map(issue -> cells(issue.severity(), issue.type(), issue.rule(), issue.file(), line(issue.line()),
                issue.message(), issue.status(), issue.key()))
            .toList();
        return new Table("issues", "Issues (" + rows.size() + ")",
            List.of("Severity", "Type", RULE, FILE, LINE, MESSAGE, STATUS, KEY), rows);
    }

    private static Table hotspots(Report report) {
        List<List<String>> rows = report.hotspots().stream()
            .map(hotspot -> cells(hotspot.vulnerabilityProbability(), hotspot.securityCategory(), hotspot.rule(),
                hotspot.file(), line(hotspot.line()), hotspot.message(), hotspot.status(), hotspot.key()))
            .toList();
        return new Table("hotspots", "Security hotspots (" + rows.size() + ")",
            List.of("Probability", "Category", RULE, FILE, LINE, MESSAGE, STATUS, KEY), rows);
    }

    private static Table files(Report report) {
        List<List<String>> rows = report.files().stream()
            .map(file -> Stream.concat(Stream.of(file.path()),
                    FILE_METRICS.stream().map(metric -> Objects.requireNonNullElse(file.measure(metric), "")))
                .toList())
            .toList();
        return new Table("files", "Files (" + rows.size() + ")", List.of(FILE, "Lines of code", "Coverage",
            "Uncovered lines", "Duplicated lines", "Duplicated lines (%)", "Open issues"), rows);
    }

    private static String line(Integer line) {
        return line == null ? "" : line.toString();
    }

    private static List<String> cells(String... values) {
        return Arrays.stream(values).map(value -> Objects.requireNonNullElse(value, "")).toList();
    }
}
