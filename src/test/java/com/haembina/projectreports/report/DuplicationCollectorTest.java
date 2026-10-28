package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.api.FakeConnector;
import com.haembina.projectreports.api.LocalApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DuplicationCollectorTest {

    private static final ReportTarget TARGET = new ReportTarget("acme", null, "7");

    private static Report.FileMeasures file(String path, String duplicated) {
        return new Report.FileMeasures("acme:" + path, path, Map.of(DuplicationCollector.DUPLICATED_LINES, duplicated));
    }

    @Test
    void listsEachBlockWithEveryOtherPlaceItAppears() {
        FakeConnector connector = new FakeConnector((path, params) -> """
            {"duplications": [
              {"blocks": [{"from": 10, "size": 15, "_ref": "1"}, {"from": 2, "size": 15, "_ref": "2"},
                          {"from": 40, "size": 3, "_ref": "1"}]},
              {"blocks": [{"from": 1, "size": 1, "_ref": "2"}, {"from": 5, "size": 2, "_ref": "9"}]},
              {"blocks": [{"from": 3, "size": 4}]}],
             "files": {"1": {"key": "acme:src/app.js"}, "2": {"key": "acme:src/copy.js"}}}""");

        List<Report.Duplication> duplications = new DuplicationCollector(new LocalApi(connector))
            .collect(TARGET, List.of(file("src/app.js", "18"), file("src/none.js", "0")));

        assertThat(duplications).containsExactly(new Report.Duplication("src/app.js", 10, 24,
            List.of(new Report.Place("src/copy.js", 2, 16), new Report.Place("src/app.js", 40, 42))));
        assertThat(connector.calls()).singleElement().satisfies(call -> assertThat(call)
            .containsEntry("path", "api/duplications/show").containsEntry("key", "acme:src/app.js")
            .containsEntry("pullRequest", "7"));
    }

    @Test
    void answersNothingForAnEmptyAnswer() {
        assertThat(new DuplicationCollector(FakeConnector.api((path, params) -> "{}"))
            .collect(TARGET, List.of(file("src/app.js", "3")))).isEmpty();
    }
}
