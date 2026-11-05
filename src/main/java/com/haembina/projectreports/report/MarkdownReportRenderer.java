package com.haembina.projectreports.report;

import static com.haembina.projectreports.report.MarkdownText.cell;
import static com.haembina.projectreports.report.MarkdownText.status;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.document.ReportAssets;
import com.haembina.projectreports.report.view.ReportView;
import java.nio.charset.StandardCharsets;

final class MarkdownReportRenderer implements ReportRenderer {

    private static final int MARK_SIZE = 20;
    private static final int ICON_SIZE = 24;
    private static final int SECTION_ICON_SIZE = 20;

    @Override
    public byte[] render(Report report) {
        ReportView view = ReportView.of(report);
        StringBuilder text = new StringBuilder("<p align=\"center\"><a href=\"").append(ReportView.PLUGIN_URL)
            .append("\">").append(image(ReportView.ASSET_URL + ReportAssets.ICON, ICON_SIZE)).append(" <b>")
            .append(ReportView.PLUGIN_NAME).append("</b></a></p>\n\n")
            .append("# ").append(cell(view.project())).append("\n\n")
            .append(MarkdownText.scopeLine(view));
        if (view.has(ReportSection.GATE)) {
            text.append(" · ").append(cell(Message.REPORT_GATE.text())).append(": **")
                .append(status(view.gate().label(), view.gate().tone())).append("**");
        }
        text.append('\n');
        for (MarkdownTables.Table table : MarkdownTables.of(view)) {
            text.append("\n## ").append(image(ReportView.ASSET_URL + ReportAssets.iconFile(table.icon()),
                SECTION_ICON_SIZE)).append(' ').append(cell(table.title())).append("\n\n");
            if (table.rows().isEmpty()) {
                text.append(table.empty()).append('\n');
                continue;
            }
            text.append(table.header());
            table.rows().forEach(row -> text.append(MarkdownText.row(row)));
        }
        text.append("\n---\n\n<p align=\"center\">").append(cell(Message.REPORT_DEVELOPED_BY.text()))
            .append(" <a href=\"").append(ReportView.BRAND_URL).append("\">")
            .append(image(ReportView.MARK_URL, MARK_SIZE)).append(" <b>").append(ReportView.BRAND_NAME)
            .append("</b></a></p>\n");
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String image(String source, int size) {
        return "<img src=\"" + source + "\" alt=\"\" width=\"" + size + "\" height=\"" + size + "\" />";
    }
}
