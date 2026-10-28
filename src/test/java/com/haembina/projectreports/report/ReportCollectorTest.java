package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.haembina.projectreports.api.ApiCallException;
import com.haembina.projectreports.api.FakeConnector;
import com.haembina.projectreports.api.LocalApi;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class ReportCollectorTest {

    private static final ReportTarget TARGET = new ReportTarget("acme", null, "7");
    private static final String EMPTY_SEARCH = "{\"paging\": {\"total\": 0}}";
    private static final List<String> LINK_PATHS = List.of("api/alm_settings/get_binding", "api/project_links/search",
        "api/settings/values", "api/project_pull_requests/list");

    private static List<String> reportCalls(FakeConnector connector) {
        return connector.calls().stream().map(call -> call.get("path")).filter(path -> !LINK_PATHS.contains(path)).toList();
    }

    private static BiFunction<String, Map<String, String>, String> server(String gate, String measures) {
        return (path, params) -> switch (path) {
            case "api/qualitygates/project_status" -> gate;
            case "api/measures/component" -> measures;
            case "api/measures/component_tree" -> """
                {"paging": {"total": 1}, "components": [{"key": "acme:src/a.js", "path": "src/a.js"}]}""";
            case "api/issues/search" -> """
                {"paging": {"total": 1}, "issues": [{"key": "i", "rule": "js:S1", "message": "m"}]}""";
            default -> EMPTY_SEARCH;
        };
    }

    @Test
    void collectsEverySectionOfItsTarget() {
        FakeConnector connector = new FakeConnector(server("""
            {"projectStatus": {"status": "ERROR", "conditions": [
              {"metricKey": "new_coverage", "comparator": "LT", "errorThreshold": "80",
               "actualValue": "61.5", "status": "ERROR"}]}}""", """
            {"component": {"measures": [
              {"metric": "ncloc", "value": "1200"},
              {"metric": "new_coverage", "period": {"index": 1, "value": "61.5"}}]}}"""));

        Report report = new ReportCollector(new LocalApi(connector)).collect(TARGET);

        assertThat(report.target()).isEqualTo(TARGET);
        assertThat(report.qualityGate()).isEqualTo(new Report.QualityGate("ERROR",
            List.of(new Report.Condition("new_coverage", "LT", "80", "61.5", "ERROR"))));
        assertThat(report.measures()).containsExactly(
            new Report.Measure("ncloc", "1200"), new Report.Measure("new_coverage", "61.5"));
        assertThat(report.files()).extracting(Report.FileMeasures::path).containsExactly("src/a.js");
        assertThat(report.issues()).extracting(Report.Issue::key).containsExactly("i");
        assertThat(report.hotspots()).isEmpty();
        assertThat(connector.calls()).filteredOn(call -> call.get("path").equals("api/measures/component"))
            .singleElement().satisfies(call -> assertThat(call)
                .containsEntry("component", "acme").containsEntry("pullRequest", "7")
                .containsEntry("metricKeys", String.join(",", ReportCollector.METRICS)));
    }

    @Test
    void asksOnlyForTheSectionsChosen() {
        FakeConnector connector = new FakeConnector((path, params) -> switch (path) {
            case "api/qualitygates/project_status" -> "{\"projectStatus\": {\"status\": \"OK\"}}";
            case "api/measures/component_tree" -> """
                {"paging": {"total": 1}, "components": [{"key": "acme:src/a.js", "path": "src/a.js",
                  "measures": [{"metric": "uncovered_lines", "value": "1"}, {"metric": "duplicated_lines", "value": "2"}]}]}""";
            case "api/sources/lines" -> "{\"sources\": [{\"line\": 3, \"lineHits\": 0}]}";
            default -> """
                {"duplications": [{"blocks": [{"from": 1, "size": 2, "_ref": "1"}, {"from": 5, "size": 2, "_ref": "1"}]}],
                 "files": {"1": {"key": "acme:src/a.js"}}}""";
        });

        Report report = new ReportCollector(new LocalApi(connector))
            .collect(TARGET, EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS));

        assertThat(report.sections()).containsExactly(ReportSection.COVERAGE, ReportSection.DUPLICATIONS);
        assertThat(report.coverage()).containsExactly(new Report.LineCoverage("src/a.js", List.of(3), List.of()));
        assertThat(report.duplications()).containsExactly(new Report.Duplication("src/a.js", 1, 2,
            List.of(new Report.Place("src/a.js", 5, 6))));
        assertThat(report.files()).isEmpty();
        assertThat(report.measures()).isEmpty();
        assertThat(report.sourceHidden()).isFalse();
        assertThat(reportCalls(connector)).containsExactly(
            "api/qualitygates/project_status", "api/measures/component_tree", "api/sources/lines",
            "api/duplications/show");
    }

    @Test
    void asksForNoFilesWhenNoChosenSectionNeedsThem() {
        FakeConnector connector = new FakeConnector(server("{\"projectStatus\": {}}", "{\"component\": {}}"));
        ReportCollector collector = new ReportCollector(new LocalApi(connector));

        collector.collect(TARGET, EnumSet.of(ReportSection.GATE));
        collector.collect(TARGET, EnumSet.of(ReportSection.COVERAGE));

        assertThat(reportCalls(connector)).containsExactly(
            "api/qualitygates/project_status", "api/qualitygates/project_status", "api/measures/component_tree");
    }

    @Test
    void saysTheLinesAreHiddenWhenTheUserMayNotSeeSource() {
        BiFunction<String, Map<String, String>, String> answers = server("{\"projectStatus\": {}}", "{\"component\": {}}");
        FakeConnector connector = new FakeConnector((path, params) -> path.startsWith("api/sources") ? FakeConnector.FORBIDDEN
            : answers.apply(path, params).replace("\"path\": \"src/a.js\"",
                "\"path\": \"src/a.js\", \"measures\": [{\"metric\": \"uncovered_lines\", \"value\": \"1\"}]"));

        Report report = new ReportCollector(new LocalApi(connector)).collect(TARGET);

        assertThat(report.sourceHidden()).isTrue();
        assertThat(report.coverage()).isEmpty();
        assertThat(report.duplications()).isEmpty();
    }

    @Test
    void passesOnAnyOtherFailureOfTheLineServices() {
        FakeConnector connector = new FakeConnector((path, params) -> switch (path) {
            case "api/qualitygates/project_status" -> "{\"projectStatus\": {}}";
            case "api/measures/component_tree" -> """
                {"paging": {"total": 1}, "components": [{"key": "acme:a.js", "path": "a.js",
                  "measures": [{"metric": "duplicated_lines", "value": "2"}]}]}""";
            default -> FakeConnector.MISSING;
        });
        ReportCollector collector = new ReportCollector(new LocalApi(connector));
        Set<ReportSection> duplications = EnumSet.of(ReportSection.DUPLICATIONS);

        assertThatThrownBy(() -> collector.collect(TARGET, duplications))
            .isInstanceOf(ApiCallException.class);
    }

    @Test
    void leavesOutWhatTheServerDoesNotAnswer() {
        FakeConnector connector = new FakeConnector(server("{\"projectStatus\": {}}", "{\"component\": {}}"));

        Report report = new ReportCollector(new LocalApi(connector)).collect(TARGET);

        assertThat(report.qualityGate()).isEqualTo(new Report.QualityGate(null, List.of()));
        assertThat(report.measures()).isEmpty();
    }
}
