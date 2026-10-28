package com.haembina.projectreports.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haembina.projectreports.api.JsonFields;
import com.haembina.projectreports.api.LocalApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

final class LineCollector {

    static final String UNCOVERED_LINES = "uncovered_lines";
    static final String UNCOVERED_CONDITIONS = "uncovered_conditions";

    private final LocalApi api;

    LineCollector(LocalApi api) {
        this.api = api;
    }

    List<Report.LineCoverage> collect(ReportTarget target, List<Report.FileMeasures> files) {
        List<Report.LineCoverage> coverage = new ArrayList<>();
        for (Report.FileMeasures file : files) {
            if (positive(file.measure(UNCOVERED_LINES)) || positive(file.measure(UNCOVERED_CONDITIONS))) {
                Report.LineCoverage lines = lines(target, file);
                if (!lines.uncovered().isEmpty() || !lines.partial().isEmpty()) {
                    coverage.add(lines);
                }
            }
        }
        return coverage;
    }

    private Report.LineCoverage lines(ReportTarget target, Report.FileMeasures file) {
        Map<String, String> params = target.params("key");
        params.put("key", file.key());
        List<Integer> uncovered = new ArrayList<>();
        List<Report.PartLine> partial = new ArrayList<>();
        for (JsonElement element : JsonFields.array(api.get("api/sources/lines", params), "sources")) {
            JsonObject line = element.getAsJsonObject();
            int number = line.get("line").getAsInt();
            Integer hits = JsonFields.integer(line, "lineHits");
            Integer conditions = JsonFields.integer(line, "conditions");
            int covered = Objects.requireNonNullElse(JsonFields.integer(line, "coveredConditions"), 0);
            if (hits != null && hits == 0) {
                uncovered.add(number);
            } else if (conditions != null && covered < conditions) {
                partial.add(new Report.PartLine(number, covered, conditions));
            }
        }
        return new Report.LineCoverage(file.path(), uncovered, partial);
    }

    static boolean positive(@Nullable String value) {
        return value != null && Double.parseDouble(value) > 0;
    }
}
