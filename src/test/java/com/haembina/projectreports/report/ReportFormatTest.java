package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class ReportFormatTest {

    @Test
    void findsEachFormatByItsKey() {
        assertThat(ReportFormat.fromKey("json")).isEqualTo(ReportFormat.JSON);
        assertThat(ReportFormat.fromKey("markdown").extension()).isEqualTo("md");
        assertThat(ReportFormat.fromKey("html").mediaType()).isEqualTo("text/html; charset=utf-8");
        assertThat(ReportFormat.fromKey("csv").extension()).isEqualTo("zip");
        assertThat(ReportFormat.fromKey("sarif").mediaType()).isEqualTo("application/sarif+json");
        assertThat(ReportFormat.fromKey("pdf").mediaType()).isEqualTo("application/pdf");
        assertThat(ReportFormat.fromKey("epub").mediaType()).isEqualTo("application/epub+zip");
    }

    @Test
    void refusesAnUnknownKey() {
        assertThatThrownBy(() -> ReportFormat.fromKey("docx"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown report format: docx");
    }

    @Test
    void givesEachFormatItsRenderer() {
        assertThat(ReportFormat.JSON.renderer()).isInstanceOf(JsonReportRenderer.class);
        assertThat(ReportFormat.MARKDOWN.renderer()).isInstanceOf(MarkdownReportRenderer.class);
        assertThat(ReportFormat.HTML.renderer()).isInstanceOf(HtmlReportRenderer.class);
        assertThat(ReportFormat.CSV.renderer()).isInstanceOf(CsvReportRenderer.class);
        assertThat(ReportFormat.SARIF.renderer()).isInstanceOf(SarifReportRenderer.class);
        assertThat(ReportFormat.PDF.renderer()).isInstanceOf(PdfReportRenderer.class);
        assertThat(ReportFormat.EPUB.renderer()).isInstanceOf(EpubReportRenderer.class);
    }

    @Test
    void rendersJsonWithEverySectionAndItsGaps() {
        String json = new String(new JsonReportRenderer().render(SampleReport.full()), StandardCharsets.UTF_8);

        assertThat(json).contains("\"branch\": \"feature/x\"", "\"pullRequest\": null", "\"status\": \"ERROR\"",
            "\"metric\": \"ncloc\"", "\"value\": null", "\"rule\": \"javascript:S3516\"",
            "\"securityCategory\": \"weak-cryptography\"", "\"path\": \"src/app.js\"");
    }

    @Test
    void rendersJsonWithTheChosenSectionsAndEachLineAndBlock() {
        String json = new String(new JsonReportRenderer().render(
            SampleReport.detailed(EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS), false)),
            StandardCharsets.UTF_8);

        assertThat(json).contains("\"sections\": [\n    \"COVERAGE\",\n    \"DUPLICATIONS\"\n  ]",
            "\"uncovered\": [\n        2,", "\"line\": 6,", "\"copies\": [", "\"path\": \"src/b.js\"",
            "\"sourceHidden\": false");
    }
}
