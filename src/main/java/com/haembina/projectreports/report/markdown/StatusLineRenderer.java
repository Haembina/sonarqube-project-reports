package com.haembina.projectreports.report.markdown;

import static com.haembina.projectreports.report.markdown.MarkdownText.cell;
import static com.haembina.projectreports.report.markdown.MarkdownText.status;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportRenderer;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.view.MetricText;
import com.haembina.projectreports.report.view.ReportView;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class StatusLineRenderer implements ReportRenderer {

    private static final String NEW_COVERAGE = "new_coverage";

    @Override
    public byte[] render(Report report) {
        ReportView view = ReportView.of(report);
        List<String> parts = new ArrayList<>();
        parts.add("**" + cell(view.project()) + "**");
        if (view.has(ReportSection.GATE)) {
            parts.add(cell(Message.REPORT_GATE.text()) + ": " + status(view.gate().label(), view.gate().tone()));
        }
        report.measures().stream().filter(measure -> NEW_COVERAGE.equals(measure.metric()) && measure.value() != null)
            .findFirst().ifPresent(measure -> parts.add(cell(MetricText.label(NEW_COVERAGE)) + ": "
                + cell(MetricText.value(NEW_COVERAGE, measure.value()))));
        if (view.has(ReportSection.ISSUES)) {
            parts.add(cell(Message.METRIC_VIOLATIONS.text()) + ": " + view.issues().size());
        }
        Report.Links links = report.links();
        parts.add(MarkdownText.link(links.sonarUrl() == null ? links.scopeUrl() : links.sonarUrl(), view.scope()));
        return (String.join(" · ", parts) + "\n").getBytes(StandardCharsets.UTF_8);
    }
}
