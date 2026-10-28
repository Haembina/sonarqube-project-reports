package com.haembina.projectreports.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haembina.projectreports.api.JsonFields;
import com.haembina.projectreports.api.SearchPager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FileCollector {

    static final String OPEN_ISSUES = "violations";
    static final List<String> METRICS = List.of(
        "ncloc", "coverage", "lines_to_cover", "uncovered_lines", "uncovered_conditions",
        "duplicated_lines", "duplicated_lines_density", OPEN_ISSUES);

    private final SearchPager pager;

    FileCollector(SearchPager pager) {
        this.pager = pager;
    }

    List<Report.FileMeasures> collect(ReportTarget target) {
        Map<String, String> params = target.params("component");
        params.put("qualifiers", "FIL,UTS");
        params.put("metricKeys", String.join(",", METRICS));
        params.put("s", "path");
        List<Report.FileMeasures> files = new ArrayList<>();
        for (JsonObject component : pager.all("api/measures/component_tree", params, "components")) {
            Map<String, String> measures = new LinkedHashMap<>();
            for (JsonElement element : JsonFields.array(component, "measures")) {
                JsonObject measure = element.getAsJsonObject();
                String value = JsonFields.measureValue(measure);
                if (value != null) {
                    measures.put(JsonFields.text(measure, "metric"), value);
                }
            }
            files.add(new Report.FileMeasures(
                JsonFields.text(component, "key"), JsonFields.text(component, "path"), measures));
        }
        return files;
    }
}
