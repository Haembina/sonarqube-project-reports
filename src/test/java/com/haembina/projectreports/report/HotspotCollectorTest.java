package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.api.FakeConnector;
import com.haembina.projectreports.api.LocalApi;
import com.haembina.projectreports.api.SearchPager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HotspotCollectorTest {

    private static final ReportTarget TARGET = new ReportTarget("acme", "dev", null);

    private static String hotspots(int total, String... keys) {
        StringBuilder items = new StringBuilder();
        for (String key : keys) {
            items.append(items.isEmpty() ? "" : ",").append("{\"key\":\"").append(key)
                .append("\",\"ruleKey\":\"js:S2245\",\"component\":\"acme:src/").append(key)
                .append(".js\",\"line\":2,\"message\":\"m\",\"securityCategory\":\"weak-cryptography\","
                    + "\"vulnerabilityProbability\":\"MEDIUM\",\"status\":\"TO_REVIEW\"}");
        }
        return "{\"paging\":{\"total\":" + total + "},\"hotspots\":[" + items + "]}";
    }

    @Test
    void readsEveryHotspotOfTheTargetUnderTheCap() {
        FakeConnector connector = new FakeConnector((path, params) -> hotspots(1, "a"));

        List<Report.Hotspot> found = new HotspotCollector(new SearchPager(new LocalApi(connector)))
            .collect(TARGET, List.of());

        assertThat(found).containsExactly(new Report.Hotspot("a", "js:S2245", "src/a.js", 2, "m",
            "weak-cryptography", "MEDIUM", "TO_REVIEW", null));
        assertThat(connector.calls().get(0)).containsEntry("path", "api/hotspots/search")
            .containsEntry("project", "acme").containsEntry("branch", "dev");
    }

    @Test
    void readsFileByFileOverTheCap() {
        FakeConnector connector = new FakeConnector((path, params) -> params.containsKey("files")
            ? hotspots(1, params.get("files").replace("src/", "").replace(".js", ""))
            : hotspots(15_000));

        List<Report.Hotspot> found = new HotspotCollector(new SearchPager(new LocalApi(connector))).collect(TARGET,
            List.of(new Report.FileMeasures("acme:src/a.js", "src/a.js", Map.of()),
                new Report.FileMeasures("acme:src/b.js", "src/b.js", Map.of())));

        assertThat(found).extracting(Report.Hotspot::key).containsExactly("a", "b");
    }
}
