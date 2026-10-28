package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.haembina.projectreports.api.FakeConnector;
import com.haembina.projectreports.api.LocalApi;
import com.haembina.projectreports.api.SearchPager;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileCollectorTest {

    @Test
    void readsEachFilesMeasuresOverallOrOnNewCode() {
        FakeConnector connector = new FakeConnector((path, params) -> """
            {"paging": {"total": 2}, "components": [
              {"key": "acme:src/a.js", "path": "src/a.js", "measures": [
                {"metric": "coverage", "value": "80.0"},
                {"metric": "violations", "period": {"index": 1, "value": "3"}},
                {"metric": "duplicated_lines"}]},
              {"key": "acme:src/b.js", "path": "src/b.js"}]}""");

        List<Report.FileMeasures> files = new FileCollector(new SearchPager(new LocalApi(connector)))
            .collect(new ReportTarget("acme", "dev", null));

        assertThat(files).extracting(Report.FileMeasures::path).containsExactly("src/a.js", "src/b.js");
        assertThat(files.get(0).measures()).containsExactly(entry("coverage", "80.0"), entry("violations", "3"));
        assertThat(files.get(1).measures()).isEmpty();
        assertThat(connector.calls().get(0)).containsEntry("path", "api/measures/component_tree")
            .containsEntry("component", "acme").containsEntry("branch", "dev")
            .containsEntry("qualifiers", "FIL,UTS")
            .containsEntry("metricKeys", String.join(",", FileCollector.METRICS));
    }
}
