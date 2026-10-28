package com.haembina.projectreports.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haembina.projectreports.api.JsonFields;
import com.haembina.projectreports.api.SearchPager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class IssueCollector {

    private static final String SEARCH = "api/issues/search";
    private static final String ISSUES = "issues";

    private final SearchPager pager;

    IssueCollector(SearchPager pager) {
        this.pager = pager;
    }

    List<Report.Issue> collect(ReportTarget target, List<Report.FileMeasures> files) {
        Map<String, String> params = query(target, target.project());
        if (SearchPager.total(pager.first(SEARCH, params, 1)) <= SearchPager.RESULT_CAP) {
            return read(target, pager.all(SEARCH, params, ISSUES));
        }
        List<Report.Issue> issues = new ArrayList<>();
        for (Report.FileMeasures file : files) {
            String open = file.measure(FileCollector.OPEN_ISSUES);
            if (open != null && Integer.parseInt(open) > 0) {
                issues.addAll(inFile(target, file, Integer.parseInt(open)));
            }
        }
        return issues;
    }

    private List<Report.Issue> inFile(ReportTarget target, Report.FileMeasures file, int open) {
        Map<String, String> params = query(target, file.key());
        if (open <= SearchPager.RESULT_CAP) {
            return read(target, pager.all(SEARCH, params, ISSUES));
        }
        Map<String, String> faceted = query(target, file.key());
        faceted.put("facets", "rules");
        JsonObject facet = JsonFields.array(pager.first(SEARCH, faceted, 1), "facets").iterator().next()
            .getAsJsonObject();
        List<Report.Issue> issues = new ArrayList<>();
        for (JsonElement value : JsonFields.array(facet, "values")) {
            Map<String, String> byRule = query(target, file.key());
            byRule.put("rules", JsonFields.text(value.getAsJsonObject(), "val"));
            issues.addAll(read(target, pager.all(SEARCH, byRule, ISSUES)));
        }
        return issues;
    }

    private static Map<String, String> query(ReportTarget target, String component) {
        Map<String, String> params = target.params("componentKeys");
        params.put("componentKeys", component);
        params.put("resolved", "false");
        params.put("s", "FILE_LINE");
        return params;
    }

    private static List<Report.Issue> read(ReportTarget target, List<JsonObject> found) {
        return found.stream()
            .map(issue -> new Report.Issue(
                JsonFields.text(issue, "key"), JsonFields.text(issue, "rule"), JsonFields.text(issue, "severity"),
                JsonFields.text(issue, "type"), target.path(JsonFields.text(issue, "component")),
                JsonFields.integer(issue, "line"), JsonFields.text(issue, "message"),
                JsonFields.text(issue, issue.has("issueStatus") ? "issueStatus" : "status"),
                JsonFields.text(issue, "creationDate")))
            .toList();
    }
}
