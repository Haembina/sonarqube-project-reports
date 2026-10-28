package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.haembina.projectreports.report.view.ReportView;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class EpubReportRendererTest {

    private static final Instant WRITTEN = Instant.parse("2026-10-28T12:30:45.678Z");

    private record Entry(int method, String text) {
    }

    private static Map<String, Entry> unzip(byte[] epub) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(epub))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(),
                    new Entry(entry.getMethod(), new String(zip.readAllBytes(), StandardCharsets.UTF_8)));
            }
        }
        return entries;
    }

    @Test
    void packsAnEpubWithTheMimetypeFirstAndStored() throws IOException {
        byte[] epub = new EpubReportRenderer(Clock.fixed(WRITTEN, ZoneOffset.UTC)).render(SampleReport.full());
        Map<String, Entry> entries = unzip(epub);

        assertThat(new ArrayList<>(entries.keySet())).startsWith("mimetype", "META-INF/container.xml",
            "EPUB/package.opf", "EPUB/nav.xhtml", "EPUB/report.xhtml", "EPUB/fonts/nunito-sans-latin.woff2",
            "EPUB/fonts/gabarito-latin.woff2", "EPUB/fonts/android-insomnia.ttf", "EPUB/haembina-mark.png",
            "EPUB/plugin-icon.png", "EPUB/icons/shield-check-section.png")
            .hasSize(EpubReportRenderer.images().size() + 8);
        assertThat(entries.get("mimetype").method()).isEqualTo(ZipEntry.STORED);
        assertThat(entries.get("mimetype").text()).isEqualTo("application/epub+zip");
        assertThat(entries.get("META-INF/container.xml").text()).contains("full-path=\"EPUB/package.opf\"");
        assertThat(entries.get("EPUB/report.xhtml").text()).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .contains("src: url(\"fonts/gabarito-latin.woff2\")", "src=\"haembina-mark.png\"");
    }

    @Test
    void describesThePackage() {
        ReportView view = ReportView.of(SampleReport.full());
        String opf = EpubReportRenderer.opf(view, WRITTEN);

        assertThat(opf).contains("<dc:identifier id=\"id\">urn:uuid:")
            .contains("<dc:title>acme – Branch feature/x</dc:title>", "<dc:language>en</dc:language>")
            .contains("<meta property=\"dcterms:modified\">2026-10-28T12:30:45Z</meta>")
            .contains("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>")
            .contains("<item id=\"font2\" href=\"fonts/android-insomnia.ttf\" media-type=\"font/ttf\"/>")
            .contains("<item id=\"image1\" href=\"plugin-icon.png\" media-type=\"image/png\"/>")
            .contains("<itemref idref=\"report\"/>")
            .isEqualTo(EpubReportRenderer.opf(view, WRITTEN));
        assertThat(EpubReportRenderer.nav(view)).contains("<nav epub:type=\"toc\"><ol><li><a href=\"report.xhtml\">acme</a>");
    }

    @Test
    void namesEachPackagedFileRelatively() {
        assertThat(EpubReportRenderer.PACKAGED.imageSource("haembina-mark.png")).isEqualTo("haembina-mark.png");
        assertThat(EpubReportRenderer.PACKAGED.fontFaces())
            .contains("font-family: \"Nunito Sans\"; src: url(\"fonts/nunito-sans-latin.woff2\"); font-weight: 200 1000;")
            .doesNotContain("data:");
    }

    @Test
    void passesOnAWriteThatFails() {
        OutputStream full = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("disk full");
            }
        };

        ReportView empty = ReportView.of(SampleReport.empty());

        assertThatThrownBy(() -> EpubReportRenderer.write(empty, WRITTEN, full))
            .isInstanceOf(UncheckedIOException.class)
            .hasRootCauseMessage("disk full");
    }
}
