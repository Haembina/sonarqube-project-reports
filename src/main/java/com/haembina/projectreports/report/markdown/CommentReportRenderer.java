package com.haembina.projectreports.report.markdown;

import static com.haembina.projectreports.report.markdown.MarkdownText.cell;
import static com.haembina.projectreports.report.markdown.MarkdownText.status;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.GitProvider;
import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportRenderer;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.view.ReportView;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public final class CommentReportRenderer implements ReportRenderer {

    static final int LIMIT = 65_536;
    private static final int RESERVE = 4_096;

    private final Optional<GitProvider> flavor;

    public CommentReportRenderer(Optional<GitProvider> flavor) {
        this.flavor = flavor;
    }

    @Override
    public byte[] render(Report report) {
        ReportView view = ReportView.of(report);
        boolean folds = flavor.or(() -> Optional.ofNullable(report.links().provider()))
            .map(GitProvider::foldsComments).orElse(false);
        StringBuilder text = new StringBuilder("### ").append(cell(view.project()));
        if (view.has(ReportSection.GATE)) {
            text.append(" · ").append(cell(Message.REPORT_GATE.text())).append(": ")
                .append(status(view.gate().label(), view.gate().tone()));
        }
        text.append("\n\n").append(MarkdownText.scopeLine(view)).append('\n');
        boolean full = false;
        for (MarkdownTables.Table table : MarkdownTables.of(view)) {
            boolean folded = folds && table.section() != ReportSection.GATE;
            text.append('\n').append(folded ? "<details><summary>" + cell(table.title()) + "</summary>\n\n"
                : "#### " + cell(table.title()) + "\n\n");
            full = rows(text, table, view.links().sonarUrl(), full);
            if (folded) {
                text.append("\n</details>\n");
            }
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static boolean rows(StringBuilder text, MarkdownTables.Table table, @Nullable String sonarUrl,
        boolean full) {
        List<List<String>> rows = table.rows();
        if (rows.isEmpty()) {
            text.append(table.empty()).append('\n');
            return full;
        }
        int shown = 0;
        if (!full) {
            text.append(table.header());
            for (List<String> row : rows) {
                String line = MarkdownText.row(row);
                if (text.length() + line.length() > LIMIT - RESERVE) {
                    break;
                }
                text.append(line);
                shown++;
            }
        }
        if (shown == rows.size()) {
            return false;
        }
        text.append('\n').append(MarkdownText.link(sonarUrl, Message.REPORT_MORE_ROWS.text(rows.size() - shown)))
            .append('\n');
        return true;
    }
}
