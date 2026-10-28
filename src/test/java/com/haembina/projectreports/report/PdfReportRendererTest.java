package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfReportRendererTest {

    @Test
    void writesAPdfThatEmbedsEveryFaceAndLinks() throws IOException {
        try (PDDocument pdf = Loader.loadPDF(new PdfReportRenderer().render(SampleReport.full()))) {
            Set<String> fonts = new TreeSet<>();
            List<String> links = new ArrayList<>();
            for (PDPage page : pdf.getPages()) {
                for (COSName name : page.getResources().getFontNames()) {
                    PDFont font = page.getResources().getFont(name);
                    fonts.add(font.getName().substring(font.getName().indexOf('+') + 1));
                }
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link && link.getAction() instanceof PDActionURI uri) {
                        links.add(uri.getURI());
                    }
                }
            }
            String text = new PDFTextStripper().getText(pdf);

            assertThat(fonts).contains("NunitoSans-Regular", "NunitoSans-Bold", "Gabarito-Bold", "android_insomnia");
            assertThat(links).containsExactly("https://github.com/Haembina/sonarqube-project-reports",
                "https://haembina.com");
            assertThat(text).contains("Haembina Project Reports", "Quality gate conditions", "Refactor | this <function>",
                "Haembina");
        }
    }

    @Test
    void drawsTheMarkFromTheJarRatherThanTheSite() {
        assertThat(PdfReportRenderer.PRINT.fontFaces()).isEmpty();
        assertThat(PdfReportRenderer.PRINT.imageSource("plugin-icon.png")).startsWith("data:image/png;base64,");
    }

    @Test
    void passesOnAWriteThatFails() {
        OutputStream full = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("disk full");
            }
        };

        assertThatThrownBy(() -> PdfReportRenderer.write("<html><body><p>x</p></body></html>", full))
            .isInstanceOf(UncheckedIOException.class)
            .hasRootCauseMessage("disk full");
    }
}
