package com.haembina.projectreports.report;

import com.haembina.projectreports.report.document.ReportAssets;
import com.haembina.projectreports.report.document.ReportXhtml;
import com.haembina.projectreports.report.view.ReportView;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

final class HtmlReportRenderer implements ReportRenderer {

    static final ReportXhtml.Embedding INLINE = new ReportXhtml.Embedding() {
        @Override
        public String fontFaces() {
            StringBuilder faces = new StringBuilder();
            for (ReportAssets.Face face : List.of(ReportAssets.BODY, ReportAssets.HEADING, ReportAssets.WORDMARK)) {
                faces.append("@font-face { font-family: \"").append(face.family()).append("\"; src: url(\"")
                    .append(dataUri(face.webType(), face.webFile())).append("\"); font-weight: ")
                    .append(face.webWeights())
                    .append("; }\n");
            }
            return faces.toString();
        }

        @Override
        public String imageSource(String asset) {
            return dataUri("image/png", asset);
        }
    };

    @Override
    public byte[] render(Report report) {
        return ReportXhtml.document(ReportView.of(report), INLINE).getBytes(StandardCharsets.UTF_8);
    }

    static String dataUri(String type, String asset) {
        return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(ReportAssets.bytes(asset));
    }
}
