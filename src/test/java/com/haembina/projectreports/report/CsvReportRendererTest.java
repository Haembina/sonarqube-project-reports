package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class CsvReportRendererTest {

    private static Map<String, String> unzip(byte[] zip) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                files.put(entry.getName(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    @Test
    void writesOneFilePerSection() throws IOException {
        Map<String, String> files = unzip(new CsvReportRenderer().render(SampleReport.full()));

        assertThat(files).containsOnlyKeys("quality-gate.csv", "measures.csv", "issues.csv", "hotspots.csv",
            "files.csv", "coverage.csv", "duplications.csv");
        assertThat(files.get("quality-gate.csv").split("\r\n")).containsExactly(
            "\"Metric\",\"Comparator\",\"Threshold\",\"Actual\",\"Status\"",
            "\"new_coverage\",\"LT\",\"80\",\"61.5\",\"ERROR\"");
        assertThat(files.get("issues.csv"))
            .contains("\"BLOCKER\",\"CODE_SMELL\",\"javascript:S3516\",\"src/app.js\",\"2\"")
            .contains("\"'=HYPERLINK(\"\"x\"\")\"")
            .contains("\"line\nbreak\"");
        assertThat(files.get("hotspots.csv")).contains("\"Make sure \"\"random\"\" is safe\"");
    }

    @Test
    void writesOnlyTheChosenSectionsWithEachLineAndBlock() throws IOException {
        Map<String, String> files = unzip(new CsvReportRenderer().render(
            SampleReport.detailed(EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS), false)));

        assertThat(files).containsOnlyKeys("coverage.csv", "duplications.csv");
        assertThat(files.get("coverage.csv").split("\r\n")).containsExactly(
            "\"File\",\"Line\",\"Status\",\"Covered conditions\",\"Conditions\"",
            "\"src/a.js\",\"2\",\"uncovered\",\"\",\"\"", "\"src/a.js\",\"3\",\"uncovered\",\"\",\"\"",
            "\"src/a.js\",\"4\",\"uncovered\",\"\",\"\"", "\"src/a.js\",\"9\",\"uncovered\",\"\",\"\"",
            "\"src/a.js\",\"6\",\"partly covered\",\"1\",\"2\"");
        assertThat(files.get("duplications.csv").split("\r\n")).containsExactly(
            "\"File\",\"From\",\"To\",\"Copy file\",\"Copy from\",\"Copy to\"",
            "\"src/a.js\",\"10\",\"24\",\"src/b.js\",\"2\",\"16\"");
    }

    @Test
    void quotesEveryFieldAndDefusesFormulas() {
        assertThat(CsvReportRenderer.field("plain")).isEqualTo("\"plain\"");
        assertThat(CsvReportRenderer.field("")).isEqualTo("\"\"");
        assertThat(CsvReportRenderer.field("-12.5")).isEqualTo("\"-12.5\"");
        assertThat(CsvReportRenderer.field("+cmd")).isEqualTo("\"'+cmd\"");
        assertThat(CsvReportRenderer.field("@SUM(B2:B9)")).isEqualTo("\"'@SUM(B2:B9)\"");
    }

    @Test
    void reportsAWriteThatFails() {
        OutputStream broken = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("disk full");
            }
        };

        Report report = SampleReport.full();

        assertThatThrownBy(() -> CsvReportRenderer.write(report, broken))
            .isInstanceOf(UncheckedIOException.class)
            .hasRootCauseMessage("disk full");
    }
}
