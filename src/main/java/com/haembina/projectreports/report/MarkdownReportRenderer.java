package com.haembina.projectreports.report;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.document.ReportAssets;
import com.haembina.projectreports.report.view.ReportIcons;
import com.haembina.projectreports.report.view.ReportView;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;

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
            .append(scopeLine(view));
        if (view.has(ReportSection.GATE)) {
            text.append(" · ").append(cell(Message.REPORT_GATE.text())).append(": **")
                .append(status(view.gate().label(), view.gate().tone())).append("**");
        }
        text.append('\n');
        findings(text, view);
        details(text, view);
        text.append("\n---\n\n<p align=\"center\">").append(cell(Message.REPORT_DEVELOPED_BY.text()))
            .append(" <a href=\"").append(ReportView.BRAND_URL).append("\">")
            .append(image(ReportView.MARK_URL, MARK_SIZE)).append(" <b>").append(ReportView.BRAND_NAME)
            .append("</b></a></p>\n");
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String scopeLine(ReportView view) {
        Report.Links links = view.links();
        String repository = links.repository() == null ? "" : link(links.repositoryUrl(), links.repository()) + " · ";
        String sonar = links.sonarUrl() == null ? ""
            : " · " + link(links.sonarUrl(), Message.REPORT_OPEN_IN_SONARQUBE.text());
        return repository + link(links.scopeUrl(), view.scope()) + sonar;
    }

    private static String link(@Nullable String url, String text) {
        return url == null ? cell(text) : "[" + cell(text).replace("[", "\\[").replace("]", "\\]") + "](" + url
            .replace(" ", "%20").replace(")", "%29") + ")";
    }

    private static void findings(StringBuilder text, ReportView view) {
        if (view.has(ReportSection.GATE)) {
            section(text, heading(ReportIcons.GATE, Message.HEADING_CONDITIONS.text()),
                Message.EMPTY_CONDITIONS.text(), heads(Message.COLUMN_CONDITION, Message.COLUMN_REQUIRED,
                    Message.COLUMN_ACTUAL, Message.COLUMN_STATUS), "| --- | --- | ---: | --- |", view.conditions(),
                condition -> List.of(condition.metric(), condition.expected(), condition.actual(),
                    status(condition.status(), condition.tone())));
        }
        if (view.has(ReportSection.MEASURES)) {
            section(text, heading(ReportIcons.MEASURES, Message.HEADING_MEASURES.text()),
                Message.EMPTY_MEASURES.text(),
                heads(Message.COLUMN_METRIC, Message.COLUMN_OVERALL, Message.COLUMN_NEW_CODE), "| --- | ---: | ---: |",
                view.measures(), measure -> List.of(measure.label(), measure.overall(), measure.newCode()));
        }
        if (view.has(ReportSection.ISSUES)) {
            section(text, heading(ReportIcons.ISSUES, Message.HEADING_ISSUES.text(view.issues().size())),
                Message.EMPTY_ISSUES.text(), heads(Message.COLUMN_SEVERITY, Message.COLUMN_TYPE, Message.COLUMN_RULE,
                    Message.COLUMN_LOCATION, Message.COLUMN_MESSAGE), "| --- | --- | --- | --- | --- |",
                view.issues(), issue -> List.of(issue.severity(), issue.type(), code(issue.rule()),
                    code(issue.location()), issue.message()));
        }
        if (view.has(ReportSection.HOTSPOTS)) {
            section(text, heading(ReportIcons.HOTSPOTS, Message.HEADING_HOTSPOTS.text(view.hotspots().size())),
                Message.EMPTY_HOTSPOTS.text(), heads(Message.COLUMN_PROBABILITY, Message.COLUMN_CATEGORY,
                    Message.COLUMN_RULE, Message.COLUMN_LOCATION, Message.COLUMN_MESSAGE, Message.COLUMN_STATUS),
                "| --- | --- | --- | --- | --- | --- |", view.hotspots(),
                hotspot -> List.of(hotspot.probability(), hotspot.category(), code(hotspot.rule()),
                    code(hotspot.location()), hotspot.message(), hotspot.status()));
        }
    }

    private static void details(StringBuilder text, ReportView view) {
        if (view.has(ReportSection.FILES)) {
            section(text, heading(ReportIcons.FILES, Message.HEADING_FILES.text(view.files().size())),
                Message.EMPTY_FILES.text(), heads(Message.COLUMN_FILE, Message.METRIC_NCLOC, Message.METRIC_COVERAGE,
                    Message.COLUMN_UNCOVERED_LINES, Message.METRIC_DUPLICATED_LINES_DENSITY,
                    Message.COLUMN_OPEN_ISSUES),
                "| --- | ---: | ---: | ---: | ---: | ---: |", view.files(),
                file -> List.of(code(file.path()), file.lines(), file.coverage(), file.uncovered(), file.duplicated(),
                    file.issues()));
        }
        if (view.has(ReportSection.COVERAGE)) {
            section(text, heading(ReportIcons.COVERAGE_LINES, Message.HEADING_COVERAGE.text(view.coverage().size())),
                view.sourceHidden() ? Message.REPORT_SOURCE_HIDDEN.text() : Message.EMPTY_COVERAGE.text(),
                heads(Message.COLUMN_FILE, Message.COLUMN_UNCOVERED_LINES, Message.COLUMN_PARTLY_COVERED),
                "| --- | --- | --- |", view.coverage(),
                lines -> List.of(code(lines.path()), lines.uncovered(), lines.partial()));
        }
        if (view.has(ReportSection.DUPLICATIONS)) {
            String duplicated = Message.HEADING_DUPLICATIONS.text(view.duplications().size());
            section(text, heading(ReportIcons.DUPLICATED_BLOCKS, duplicated),
                view.sourceHidden() ? Message.REPORT_SOURCE_HIDDEN.text() : Message.EMPTY_DUPLICATIONS.text(),
                heads(Message.COLUMN_FILE, Message.COLUMN_LINES, Message.COLUMN_ALSO_IN), "| --- | --- | --- |",
                view.duplications(), block -> List.of(code(block.path()), block.lines(), block.copies()));
        }
    }

    private static List<String> heads(Message... columns) {
        return Arrays.stream(columns).map(Message::text).toList();
    }

    private static String image(String source, int size) {
        return "<img src=\"" + source + "\" alt=\"\" width=\"" + size + "\" height=\"" + size + "\" />";
    }

    private static String status(String label, String tone) {
        return switch (tone) {
            case "pass" -> "✓ " + label;
            case "fail" -> "✗ " + label;
            default -> label;
        };
    }

    private static String heading(String icon, String title) {
        return image(ReportView.ASSET_URL + ReportAssets.iconFile(icon), SECTION_ICON_SIZE) + " " + cell(title);
    }

    private static <T> void section(StringBuilder text, String heading, String empty,
        List<String> heads, String divider, List<T> rows, Function<T, List<String>> cells) {
        text.append("\n## ").append(heading).append("\n\n");
        if (rows.isEmpty()) {
            text.append(empty).append('\n');
            return;
        }
        row(text, heads);
        text.append(divider).append('\n');
        for (T item : rows) {
            row(text, cells.apply(item));
        }
    }

    private static void row(StringBuilder text, List<String> values) {
        text.append('|');
        for (String value : values) {
            text.append(' ').append(value.startsWith("`") ? value : cell(value)).append(" |");
        }
        text.append('\n');
    }

    private static String code(String value) {
        return "`" + value.replace("`", "'").replace("|", "\\|").replace("\n", " ") + "`";
    }

    private static String cell(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|").replace("\r", "").replace("\n", " ")
            .replace("<", "&lt;").replace(">", "&gt;");
    }
}
