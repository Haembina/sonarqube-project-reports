package com.haembina.projectreports.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haembina.projectreports.api.ApiCallException;
import com.haembina.projectreports.api.JsonFields;
import com.haembina.projectreports.api.LocalApi;
import com.haembina.projectreports.api.SearchPager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReportCollector {

    static final List<String> METRICS = List.of(
        "ncloc", "new_lines",
        "coverage", "new_coverage", "lines_to_cover", "new_lines_to_cover",
        "duplicated_lines_density", "new_duplicated_lines_density", "duplicated_lines", "new_duplicated_lines",
        "bugs", "new_bugs", "vulnerabilities", "new_vulnerabilities", "code_smells", "new_code_smells",
        "security_hotspots", "new_security_hotspots",
        "reliability_rating", "new_reliability_rating", "security_rating", "new_security_rating",
        "sqale_rating", "new_maintainability_rating");

    private static final Set<ReportSection> FILE_USERS = EnumSet.of(ReportSection.ISSUES, ReportSection.HOTSPOTS,
        ReportSection.FILES, ReportSection.COVERAGE, ReportSection.DUPLICATIONS);
    private static final int FORBIDDEN = 403;

    private final LocalApi api;
    private final SearchPager pager;
    private final String sonarRoot;

    public ReportCollector(LocalApi api) {
        this(api, "");
    }

    public ReportCollector(LocalApi api, String sonarRoot) {
        this.api = api;
        this.pager = new SearchPager(api);
        this.sonarRoot = sonarRoot;
    }

    public Report collect(ReportTarget target) {
        return collect(target, ReportSection.all());
    }

    public Report collect(ReportTarget target, Set<ReportSection> sections) {
        Report.QualityGate gate = qualityGate(target);
        List<Report.Measure> measures = sections.contains(ReportSection.MEASURES) ? measures(target) : List.of();
        List<Report.FileMeasures> files = FILE_USERS.stream().anyMatch(sections::contains)
            ? new FileCollector(pager).collect(target) : List.of();
        List<Report.Issue> issues = sections.contains(ReportSection.ISSUES)
            ? new IssueCollector(pager).collect(target, files) : List.of();
        List<Report.Hotspot> hotspots = sections.contains(ReportSection.HOTSPOTS)
            ? new HotspotCollector(pager).collect(target, files) : List.of();
        List<Report.LineCoverage> coverage = List.of();
        List<Report.Duplication> duplications = List.of();
        boolean sourceHidden = false;
        try {
            if (sections.contains(ReportSection.COVERAGE)) {
                coverage = new LineCollector(api).collect(target, files);
            }
            if (sections.contains(ReportSection.DUPLICATIONS)) {
                duplications = new DuplicationCollector(api).collect(target, files);
            }
        } catch (ApiCallException failure) {
            if (failure.status() != FORBIDDEN) {
                throw failure;
            }
            sourceHidden = true;
        }
        return new Report(target, sections, gate, measures, issues, hotspots,
            sections.contains(ReportSection.FILES) ? files : List.of(), coverage, duplications, sourceHidden,
            new LinkCollector(api, sonarRoot).collect(target));
    }

    private Report.QualityGate qualityGate(ReportTarget target) {
        JsonObject status = api.get("api/qualitygates/project_status", target.params("projectKey"))
            .getAsJsonObject("projectStatus");
        List<Report.Condition> conditions = new ArrayList<>();
        for (JsonElement element : JsonFields.array(status, "conditions")) {
            JsonObject condition = element.getAsJsonObject();
            conditions.add(new Report.Condition(
                JsonFields.text(condition, "metricKey"), JsonFields.text(condition, "comparator"),
                JsonFields.text(condition, "errorThreshold"), JsonFields.text(condition, "actualValue"),
                JsonFields.text(condition, "status")));
        }
        return new Report.QualityGate(JsonFields.text(status, "status"), conditions);
    }

    private List<Report.Measure> measures(ReportTarget target) {
        Map<String, String> params = target.params("component");
        params.put("metricKeys", String.join(",", METRICS));
        JsonObject component = api.get("api/measures/component", params).getAsJsonObject("component");
        List<Report.Measure> measures = new ArrayList<>();
        for (JsonElement element : JsonFields.array(component, "measures")) {
            JsonObject measure = element.getAsJsonObject();
            measures.add(new Report.Measure(JsonFields.text(measure, "metric"), JsonFields.measureValue(measure)));
        }
        return measures;
    }
}
