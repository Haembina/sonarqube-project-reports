package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.api.FakeConnector;
import com.haembina.projectreports.api.LocalApi;
import com.haembina.projectreports.api.SearchPager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IssueCollectorTest {

    private static final ReportTarget TARGET = new ReportTarget("acme", null, "7");

    private static String issues(int total, String... keys) {
        StringBuilder items = new StringBuilder();
        for (String key : keys) {
            items.append(items.isEmpty() ? "" : ",").append("{\"key\":\"").append(key)
                .append("\",\"rule\":\"js:S1\",\"component\":\"acme:src/").append(key)
                .append(".js\",\"line\":4,\"message\":\"m\",\"status\":\"OPEN\"}");
        }
        return "{\"paging\":{\"total\":" + total + "},\"issues\":[" + items + "]}";
    }

    private static Report.FileMeasures file(String name, String open) {
        return new Report.FileMeasures("acme:src/" + name + ".js", "src/" + name + ".js",
            open == null ? Map.of() : Map.of("violations", open));
    }

    @Test
    void readsEveryOpenIssueOfTheTargetUnderTheCap() {
        FakeConnector connector = new FakeConnector((path, params) -> issues(2, "a", "b"));

        List<Report.Issue> found = new IssueCollector(new SearchPager(new LocalApi(connector))).collect(TARGET, List.of());

        assertThat(found).containsExactly(
            new Report.Issue("a", "js:S1", null, null, "src/a.js", 4, "m", "OPEN", null),
            new Report.Issue("b", "js:S1", null, null, "src/b.js", 4, "m", "OPEN", null));
        assertThat(connector.calls().get(0)).containsEntry("componentKeys", "acme").containsEntry("pullRequest", "7")
            .containsEntry("resolved", "false");
    }

    @Test
    void readsFileByFileOverTheCap() {
        FakeConnector connector = new FakeConnector((path, params) -> switch (params.get("componentKeys")) {
            case "acme" -> issues(12_000);
            case "acme:src/a.js" -> issues(1, "a");
            default -> issues(1, "c");
        });

        List<Report.Issue> found = new IssueCollector(new SearchPager(new LocalApi(connector)))
            .collect(TARGET, List.of(file("a", "1"), file("b", "0"), file("none", null), file("c", "1")));

        assertThat(found).extracting(Report.Issue::key).containsExactly("a", "c");
    }

    @Test
    void splitsAFileOverTheCapByRule() {
        FakeConnector connector = new FakeConnector((path, params) -> {
            if ("acme".equals(params.get("componentKeys"))) {
                return issues(12_000);
            }
            if (params.containsKey("facets")) {
                return "{\"paging\":{\"total\":11000},\"issues\":[],\"facets\":[{\"property\":\"rules\",\"values\":["
                    + "{\"val\":\"js:S1\",\"count\":6000},{\"val\":\"js:S2\",\"count\":5000}]}]}";
            }
            return issues(1, params.get("rules").replace("js:", ""));
        });

        List<Report.Issue> found = new IssueCollector(new SearchPager(new LocalApi(connector)))
            .collect(TARGET, List.of(file("big", "11000")));

        assertThat(found).extracting(Report.Issue::key).containsExactly("S1", "S2");
    }

    @Test
    void prefersTheNewIssueStatus() {
        FakeConnector connector = new FakeConnector((path, params) -> """
            {"paging": {"total": 1}, "issues": [{"key": "a", "rule": "js:S1", "message": "m",
              "status": "RESOLVED", "issueStatus": "ACCEPTED", "severity": "MAJOR", "type": "BUG",
              "creationDate": "2026-10-28T07:06:28+0000"}]}""");

        Report.Issue issue = new IssueCollector(new SearchPager(new LocalApi(connector))).collect(TARGET, List.of()).get(0);

        assertThat(issue).isEqualTo(new Report.Issue("a", "js:S1", "MAJOR", "BUG", null, null, "m", "ACCEPTED",
            "2026-10-28T07:06:28+0000"));
    }
}
