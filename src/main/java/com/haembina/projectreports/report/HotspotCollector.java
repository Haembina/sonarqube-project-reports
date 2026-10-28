package com.haembina.projectreports.report;

import com.google.gson.JsonObject;
import com.haembina.projectreports.api.JsonFields;
import com.haembina.projectreports.api.SearchPager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class HotspotCollector {

    private static final String SEARCH = "api/hotspots/search";

    private final SearchPager pager;

    HotspotCollector(SearchPager pager) {
        this.pager = pager;
    }

    List<Report.Hotspot> collect(ReportTarget target, List<Report.FileMeasures> files) {
        Map<String, String> params = target.params("project");
        if (SearchPager.total(pager.first(SEARCH, params, 1)) <= SearchPager.RESULT_CAP) {
            return read(target, pager.all(SEARCH, params, "hotspots"));
        }
        List<Report.Hotspot> hotspots = new ArrayList<>();
        for (Report.FileMeasures file : files) {
            Map<String, String> inFile = target.params("project");
            inFile.put("files", file.path());
            hotspots.addAll(read(target, pager.all(SEARCH, inFile, "hotspots")));
        }
        return hotspots;
    }

    private static List<Report.Hotspot> read(ReportTarget target, List<JsonObject> found) {
        return found.stream()
            .map(hotspot -> new Report.Hotspot(
                JsonFields.text(hotspot, "key"), JsonFields.text(hotspot, "ruleKey"),
                target.path(JsonFields.text(hotspot, "component")), JsonFields.integer(hotspot, "line"),
                JsonFields.text(hotspot, "message"), JsonFields.text(hotspot, "securityCategory"),
                JsonFields.text(hotspot, "vulnerabilityProbability"), JsonFields.text(hotspot, "status"),
                JsonFields.text(hotspot, "resolution")))
            .toList();
    }
}
