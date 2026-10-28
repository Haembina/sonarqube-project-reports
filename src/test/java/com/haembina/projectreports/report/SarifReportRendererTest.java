package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class SarifReportRendererTest {

    private static String render(Report report) {
        return new String(new SarifReportRenderer().render(report), StandardCharsets.UTF_8);
    }

    private static Schema schema() throws IOException {
        try (InputStream input = SarifReportRendererTest.class.getResourceAsStream("/sarif/sarif-2.1.0.json")) {
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(new String(input.readAllBytes(), StandardCharsets.UTF_8), InputFormat.JSON);
        }
    }

    @Test
    void isValidAgainstTheSarifSchema() throws IOException {
        Schema schema = schema();

        assertThat(schema.validate(render(SampleReport.full()), InputFormat.JSON)).isEmpty();
        assertThat(schema.validate(render(SampleReport.empty()), InputFormat.JSON)).isEmpty();
    }

    @Test
    void writesEachIssueAndHotspotAsAResultWithItsLocation() {
        JsonObject run = JsonParser.parseString(render(SampleReport.full())).getAsJsonObject()
            .getAsJsonArray("runs").get(0).getAsJsonObject();

        assertThat(run.getAsJsonObject("automationDetails").get("id").getAsString())
            .isEqualTo("acme, branch feature/x");
        assertThat(run.getAsJsonObject("tool").getAsJsonObject("driver").getAsJsonArray("rules"))
            .extracting(rule -> rule.getAsJsonObject().get("id").getAsString())
            .containsExactly("javascript:S1135", "javascript:S1481", "javascript:S2245", "javascript:S3516");
        assertThat(run.getAsJsonArray("results")).hasSize(4);
        JsonObject first = run.getAsJsonArray("results").get(0).getAsJsonObject();
        assertThat(first.get("level").getAsString()).isEqualTo("error");
        assertThat(first.getAsJsonArray("locations").get(0).getAsJsonObject().getAsJsonObject("physicalLocation")
            .getAsJsonObject("region").get("startLine").getAsInt()).isEqualTo(2);
        assertThat(run.getAsJsonArray("results").get(1).getAsJsonObject().has("locations")).isFalse();
        assertThat(run.getAsJsonArray("results").get(2).getAsJsonObject().getAsJsonArray("locations").get(0)
            .getAsJsonObject().getAsJsonObject("physicalLocation").has("region")).isFalse();
        JsonObject hotspot = run.getAsJsonArray("results").get(3).getAsJsonObject();
        assertThat(hotspot.get("level").getAsString()).isEqualTo("note");
        assertThat(hotspot.getAsJsonObject("properties").get("sonarKind").getAsString()).isEqualTo("securityHotspot");
    }

    @Test
    void holdsNoResultWhenNeitherIssuesNorHotspotsAreChosen() {
        JsonObject run = JsonParser.parseString(render(SampleReport.detailed(EnumSet.of(ReportSection.COVERAGE),
            false))).getAsJsonObject().getAsJsonArray("runs").get(0).getAsJsonObject();

        assertThat(run.getAsJsonArray("results")).isEmpty();
    }

    @Test
    void locatesAFileWithoutALine() {
        Report report = new Report(new ReportTarget("acme", null, null), new Report.QualityGate("OK", List.of()),
            List.of(), List.of(new Report.Issue("i", "js:S1", "MAJOR", "BUG", "src/a.js", null, "m", "OPEN", null)),
            List.of(), List.of());

        JsonObject location = JsonParser.parseString(render(report)).getAsJsonObject().getAsJsonArray("runs").get(0)
            .getAsJsonObject().getAsJsonArray("results").get(0).getAsJsonObject().getAsJsonArray("locations").get(0)
            .getAsJsonObject().getAsJsonObject("physicalLocation");

        assertThat(location.getAsJsonObject("artifactLocation").get("uri").getAsString()).isEqualTo("src/a.js");
        assertThat(location.has("region")).isFalse();
        assertThat(ReportTables.title(report)).isEqualTo("acme, main branch");
    }

    @Test
    void mapsSeverityToLevel() {
        assertThat(SarifReportRenderer.level("BLOCKER")).isEqualTo("error");
        assertThat(SarifReportRenderer.level("CRITICAL")).isEqualTo("error");
        assertThat(SarifReportRenderer.level("MAJOR")).isEqualTo("warning");
        assertThat(SarifReportRenderer.level(null)).isEqualTo("warning");
        assertThat(SarifReportRenderer.level("MINOR")).isEqualTo("note");
        assertThat(SarifReportRenderer.level("INFO")).isEqualTo("note");
    }
}
