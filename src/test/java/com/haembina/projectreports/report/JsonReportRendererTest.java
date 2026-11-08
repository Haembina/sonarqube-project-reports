package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JsonReportRendererTest {

    @Test
    void writesTheLinksWithoutTheirRenderingHints() {
        Report report = SampleReport.linked(SampleReport.full(), SampleReport.githubLinks());
        String json = new String(new JsonReportRenderer().render(report), StandardCharsets.UTF_8);
        JsonObject links = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("links");

        assertThat(links.keySet()).containsExactlyInAnyOrder("repository", "repositoryUrl", "scopeUrl", "sonarUrl",
            "pullRequestTitle");
        assertThat(links.get("repository").getAsString()).isEqualTo("Acme/app");
    }
}
