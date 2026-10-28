package com.haembina.projectreports.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

final class SarifReportRenderer implements ReportRenderer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> ERRORS = Set.of("BLOCKER", "CRITICAL");
    private static final Set<String> NOTES = Set.of("MINOR", "INFO");

    @Override
    public byte[] render(Report report) {
        Set<String> rules = new TreeSet<>();
        JsonArray results = new JsonArray();
        for (Report.Issue issue : report.issues()) {
            rules.add(issue.rule());
            JsonObject result = result(issue.rule(), level(issue.severity()), issue.message(), issue.file(),
                issue.line());
            result.add("properties", properties("issue", issue.key(), issue.type()));
            results.add(result);
        }
        for (Report.Hotspot hotspot : report.hotspots()) {
            rules.add(hotspot.rule());
            JsonObject result = result(hotspot.rule(), "note", hotspot.message(), hotspot.file(), hotspot.line());
            result.add("properties", properties("securityHotspot", hotspot.key(), hotspot.securityCategory()));
            results.add(result);
        }
        JsonArray ruleArray = new JsonArray();
        for (String rule : rules) {
            JsonObject descriptor = new JsonObject();
            descriptor.addProperty("id", rule);
            ruleArray.add(descriptor);
        }
        JsonObject driver = new JsonObject();
        driver.addProperty("name", "SonarQube");
        driver.add("rules", ruleArray);
        JsonObject tool = new JsonObject();
        tool.add("driver", driver);
        JsonObject automation = new JsonObject();
        automation.addProperty("id", ReportTables.title(report));
        JsonObject run = new JsonObject();
        run.add("tool", tool);
        run.add("automationDetails", automation);
        run.add("results", results);
        JsonArray runs = new JsonArray();
        runs.add(run);
        JsonObject sarif = new JsonObject();
        sarif.addProperty("$schema", "https://json.schemastore.org/sarif-2.1.0.json");
        sarif.addProperty("version", "2.1.0");
        sarif.add("runs", runs);
        return GSON.toJson(sarif).getBytes(StandardCharsets.UTF_8);
    }

    static String level(String severity) {
        if (severity == null) {
            return "warning";
        }
        if (ERRORS.contains(severity)) {
            return "error";
        }
        return NOTES.contains(severity) ? "note" : "warning";
    }

    private static JsonObject result(String rule, String level, String message, String file, Integer line) {
        JsonObject result = new JsonObject();
        result.addProperty("ruleId", rule);
        result.addProperty("level", level);
        JsonObject text = new JsonObject();
        text.addProperty("text", message);
        result.add("message", text);
        if (file != null) {
            JsonObject artifact = new JsonObject();
            artifact.addProperty("uri", file);
            JsonObject physical = new JsonObject();
            physical.add("artifactLocation", artifact);
            if (line != null && line > 0) {
                JsonObject region = new JsonObject();
                region.addProperty("startLine", line);
                physical.add("region", region);
            }
            JsonObject location = new JsonObject();
            location.add("physicalLocation", physical);
            JsonArray locations = new JsonArray();
            locations.add(location);
            result.add("locations", locations);
        }
        return result;
    }

    private static JsonObject properties(String kind, String key, String category) {
        JsonObject properties = new JsonObject();
        properties.addProperty("sonarKind", kind);
        properties.addProperty("sonarKey", key);
        properties.addProperty("sonarCategory", category);
        return properties;
    }
}
