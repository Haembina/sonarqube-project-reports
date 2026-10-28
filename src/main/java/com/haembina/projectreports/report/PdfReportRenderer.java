package com.haembina.projectreports.report;

import com.haembina.projectreports.report.document.ReportAssets;
import com.haembina.projectreports.report.document.ReportXhtml;
import com.haembina.projectreports.report.view.ReportView;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

final class PdfReportRenderer implements ReportRenderer {

    static final ReportXhtml.Embedding PRINT = new ReportXhtml.Embedding() {
        @Override
        public String fontFaces() {
            return "";
        }

        @Override
        public String imageSource(String asset) {
            return HtmlReportRenderer.dataUri("image/png", asset);
        }
    };

    @Override
    public byte[] render(Report report) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        write(ReportXhtml.document(ReportView.of(report), PRINT), bytes);
        return bytes.toByteArray();
    }

    static void write(String xhtml, OutputStream output) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(xhtml, null);
        for (ReportAssets.Face face : List.of(ReportAssets.BODY, ReportAssets.BODY_BOLD, ReportAssets.HEADING,
            ReportAssets.WORDMARK)) {
            byte[] font = ReportAssets.bytes(face.printFile());
            builder.useFont(() -> new ByteArrayInputStream(font), face.family(), face.weight(),
                BaseRendererBuilder.FontStyle.NORMAL, true);
        }
        builder.toStream(output);
        try {
            builder.run();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
