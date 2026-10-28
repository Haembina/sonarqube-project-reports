package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.api.FakeConnector;
import com.haembina.projectreports.api.LocalApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LineCollectorTest {

    private static final ReportTarget TARGET = new ReportTarget("acme", "dev", null);

    @Test
    void listsUncoveredAndPartlyCoveredLinesOfFilesThatHaveThem() {
        FakeConnector connector = new FakeConnector((path, params) -> """
            {"sources": [
              {"line": 1, "lineHits": 1},
              {"line": 2, "lineHits": 0},
              {"line": 3, "lineHits": 0, "conditions": 2},
              {"line": 4, "lineHits": 2, "conditions": 2, "coveredConditions": 1},
              {"line": 5, "lineHits": 1, "conditions": 2, "coveredConditions": 2},
              {"line": 6, "lineHits": 1, "conditions": 4},
              {"line": 7}]}""");
        List<Report.FileMeasures> files = List.of(
            new Report.FileMeasures("acme:a.js", "a.js", Map.of(LineCollector.UNCOVERED_LINES, "2")),
            new Report.FileMeasures("acme:b.js", "b.js", Map.of(LineCollector.UNCOVERED_LINES, "0")),
            new Report.FileMeasures("acme:c.js", "c.js", Map.of(LineCollector.UNCOVERED_CONDITIONS, "3.0")),
            new Report.FileMeasures("acme:d.js", "d.js", Map.of()));

        List<Report.LineCoverage> coverage =
            new LineCollector(new LocalApi(connector)).collect(TARGET, files);

        Report.LineCoverage expected = new Report.LineCoverage("a.js", List.of(2, 3),
            List.of(new Report.PartLine(4, 1, 2), new Report.PartLine(6, 0, 4)));
        assertThat(coverage).containsExactly(expected,
            new Report.LineCoverage("c.js", expected.uncovered(), expected.partial()));
        assertThat(connector.calls()).extracting(call -> call.get("key")).containsExactly("acme:a.js", "acme:c.js");
        assertThat(connector.calls().get(0)).containsEntry("path", "api/sources/lines").containsEntry("branch", "dev");
    }

    @Test
    void skipsAFileWhoseLinesAreAllCoveredAndKeepsOneOnlyPartlyCovered() {
        List<Report.LineCoverage> coverage = new LineCollector(FakeConnector.api((path, params) ->
            params.get("key").equals("acme:a.js") ? "{\"sources\": [{\"line\": 1, \"lineHits\": 1}]}"
                : "{\"sources\": [{\"line\": 4, \"lineHits\": 1, \"conditions\": 2, \"coveredConditions\": 1}]}"))
            .collect(TARGET, List.of(
                new Report.FileMeasures("acme:a.js", "a.js", Map.of(LineCollector.UNCOVERED_LINES, "1")),
                new Report.FileMeasures("acme:b.js", "b.js", Map.of(LineCollector.UNCOVERED_CONDITIONS, "1"))));

        assertThat(coverage).containsExactly(
            new Report.LineCoverage("b.js", List.of(), List.of(new Report.PartLine(4, 1, 2))));
    }
}
