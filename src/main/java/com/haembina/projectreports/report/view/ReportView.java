package com.haembina.projectreports.report.view;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportTarget;
import com.haembina.projectreports.report.ReportSection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public record ReportView(String project, String scope, Gate gate, List<Condition> conditions, List<Measure> measures,
    List<Issue> issues, List<Hotspot> hotspots, List<File> files, Set<ReportSection> sections,
    List<CoverageRow> coverage, List<DuplicationRow> duplications, boolean sourceHidden, Report.Links links) {

    public static final String PLUGIN_NAME = "Haembina Project Reports";
    public static final String PLUGIN_URL = "https://github.com/Haembina/sonarqube-project-reports";
    public static final String BRAND_NAME = "Haembina";
    public static final String BRAND_URL = "https://haembina.com";
    public static final String MARK_URL = "https://haembina.com/assets/branding/logo.png";
    public static final String ASSET_URL =
        "https://raw.githubusercontent.com/Haembina/sonarqube-project-reports/main/src/main/resources/report/";

    private static final String NONE = "—";

    public ReportView {
        conditions = List.copyOf(conditions);
        measures = List.copyOf(measures);
        issues = List.copyOf(issues);
        hotspots = List.copyOf(hotspots);
        files = List.copyOf(files);
        sections = Set.copyOf(sections);
        coverage = List.copyOf(coverage);
        duplications = List.copyOf(duplications);
    }

    public ReportView(String project, String scope, Gate gate, List<Condition> conditions, List<Measure> measures,
        List<Issue> issues, List<Hotspot> hotspots, List<File> files) {
        this(project, scope, gate, conditions, measures, issues, hotspots, files, ReportSection.all(), List.of(),
            List.of(), false, Report.Links.NONE);
    }

    public ReportView(String project, String scope, Gate gate, List<Condition> conditions, List<Measure> measures,
        List<Issue> issues, List<Hotspot> hotspots, List<File> files, Set<ReportSection> sections,
        List<CoverageRow> coverage, List<DuplicationRow> duplications, boolean sourceHidden) {
        this(project, scope, gate, conditions, measures, issues, hotspots, files, sections, coverage, duplications,
            sourceHidden, Report.Links.NONE);
    }

    public boolean has(ReportSection section) {
        return sections.contains(section);
    }

    public record Gate(String label, String tone) {
    }

    public record CoverageRow(String path, String uncovered, String partial) {
    }

    public record DuplicationRow(String path, String lines, String copies) {
    }

    public record Condition(String metric, String expected, String actual, String status, String tone) {
    }

    public record Measure(String label, String icon, String overall, String newCode, boolean rating) {
    }

    public record Issue(String severity, String severityIcon, String type, String typeIcon, String rule,
        String location, String message) {
    }

    public record Hotspot(String probability, String category, String rule, String location, String message,
        String status) {
    }

    public record File(String path, String lines, String coverage, String uncovered, String duplicated,
        String issues) {
    }

    public static ReportView of(Report report) {
        Map<String, String> values = new HashMap<>();
        report.measures().forEach(measure -> values.put(measure.metric(), measure.value()));
        return new ReportView(report.target().project(), scope(report.target(), report.links().pullRequestTitle()), gate(report.qualityGate().status()),
            report.qualityGate().conditions().stream().map(ReportView::condition).toList(),
            MetricText.PAIRS.stream()
                .filter(pair -> values.containsKey(pair.overall()) || values.containsKey(pair.newCode()))
                .map(pair -> new Measure(pair.label(), pair.icon(),
                    MetricText.value(pair.overall(), values.get(pair.overall())),
                    MetricText.value(pair.newCode(), values.get(pair.newCode())), MetricText.isRating(pair.overall())))
                .toList(),
            report.issues().stream().map(issue -> new Issue(MetricText.words(issue.severity()),
                ReportIcons.forSeverity(issue.severity()),
                MetricText.words(issue.type()), ReportIcons.forType(issue.type()), issue.rule(),
                location(issue.file(), issue.line()), issue.message()))
                .toList(),
            report.hotspots().stream().map(hotspot -> new Hotspot(MetricText.words(hotspot.vulnerabilityProbability()),
                MetricText.words(hotspot.securityCategory()), hotspot.rule(), location(hotspot.file(), hotspot.line()),
                hotspot.message(), MetricText.words(hotspot.status())))
                .toList(),
            report.files().stream().map(file -> new File(file.path(),
                MetricText.value("ncloc", file.measure("ncloc")), MetricText.value("coverage", file.measure("coverage")),
                MetricText.value("uncovered_lines", file.measure("uncovered_lines")),
                MetricText.value("duplicated_lines_density", file.measure("duplicated_lines_density")),
                MetricText.value("violations", file.measure("violations"))))
                .toList(),
            report.sections(),
            report.coverage().stream().map(lines -> new CoverageRow(lines.path(), LineRanges.text(lines.uncovered()),
                lines.partial().stream().map(LineRanges::partial).reduce((one, two) -> one + ", " + two)
                    .orElse(NONE)))
                .toList(),
            report.duplications().stream().map(block -> new DuplicationRow(block.path(),
                LineRanges.span(block.from(), block.to()),
                block.copies().stream().map(copy -> copy.path() + " " + LineRanges.span(copy.from(), copy.to()))
                    .reduce((one, two) -> one + "; " + two).orElse(NONE)))
                .toList(),
            report.sourceHidden(), report.links());
    }

    static String scope(ReportTarget target) {
        return scope(target, null);
    }

    static String scope(ReportTarget target, @Nullable String pullRequestTitle) {
        if (target.pullRequest() != null) {
            return pullRequestTitle == null || pullRequestTitle.isBlank()
                ? Message.REPORT_PULL_REQUEST.text(target.pullRequest())
                : Message.REPORT_PULL_REQUEST_TITLED.text(target.pullRequest(), pullRequestTitle);
        }
        return target.branch() != null ? Message.REPORT_BRANCH.text(target.branch())
            : Message.REPORT_MAIN_BRANCH.text();
    }

    static Gate gate(@Nullable String status) {
        return switch (Objects.requireNonNullElse(status, "NONE")) {
            case "OK" -> new Gate(Message.REPORT_PASSED.text(), "pass");
            case "ERROR" -> new Gate(Message.REPORT_FAILED.text(), "fail");
            default -> new Gate(Message.REPORT_NOT_COMPUTED.text(), "none");
        };
    }

    private static Condition condition(Report.Condition condition) {
        String metric = condition.metric();
        boolean failed = "ERROR".equals(condition.status());
        return new Condition(MetricText.label(metric),
            MetricText.comparator(condition.comparator()) + " " + MetricText.value(metric, condition.threshold()),
            MetricText.value(metric, condition.actual()),
            failed ? Message.REPORT_FAILED.text() : Message.REPORT_PASSED.text(), failed ? "fail" : "pass");
    }

    static String location(@Nullable String file, @Nullable Integer line) {
        if (file == null) {
            return NONE;
        }
        return line == null || line <= 0 ? file : file + ":" + line;
    }
}
