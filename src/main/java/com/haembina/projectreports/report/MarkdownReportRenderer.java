package com.haembina.projectreports.report;

import com.haembina.projectreports.report.document.ReportAssets;
import com.haembina.projectreports.report.document.ReportXhtml;
import com.haembina.projectreports.report.view.ReportIcons;
import com.haembina.projectreports.report.view.ReportView;
import java.nio.charset.StandardCharsets;
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
            text.append(" · Quality gate: **").append(status(view.gate().label(), view.gate().tone())).append("**");
        }
        text.append('\n');
        findings(text, view);
        details(text, view);
        text.append("\n---\n\n<p align=\"center\">Developed by <a href=\"").append(ReportView.BRAND_URL).append("\">")
            .append(image(ReportView.MARK_URL, MARK_SIZE)).append(" <b>").append(ReportView.BRAND_NAME)
            .append("</b></a></p>\n");
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String scopeLine(ReportView view) {
        Report.Links links = view.links();
        String repository = links.repository() == null ? "" : link(links.repositoryUrl(), links.repository()) + " · ";
        String sonar = links.sonarUrl() == null ? ""
            : " · " + link(links.sonarUrl(), ReportXhtml.OPEN_IN_SONARQUBE);
        return repository + link(links.scopeUrl(), view.scope()) + sonar;
    }

    private static String link(@Nullable String url, String text) {
        return url == null ? cell(text) : "[" + cell(text).replace("[", "\\[").replace("]", "\\]") + "](" + url
            .replace(" ", "%20").replace(")", "%29") + ")";
    }

    private static void findings(StringBuilder text, ReportView view) {
        if (view.has(ReportSection.GATE)) {
            section(text, heading(ReportIcons.GATE, "Quality gate conditions"), "No conditions apply to this scope.",
                List.of("Condition", "Required", "Actual", "Status"), "| --- | --- | ---: | --- |", view.conditions(),
                condition -> List.of(condition.metric(), condition.expected(), condition.actual(),
                    status(condition.status(), condition.tone())));
        }
        if (view.has(ReportSection.MEASURES)) {
            section(text, heading(ReportIcons.MEASURES, "Measures"), "The server holds no measures for this scope.",
                List.of("Metric", "Overall", "New code"), "| --- | ---: | ---: |", view.measures(),
                measure -> List.of(measure.label(), measure.overall(), measure.newCode()));
        }
        if (view.has(ReportSection.ISSUES)) {
            section(text, heading(ReportIcons.ISSUES, "Issues (" + view.issues().size() + ")"), "No open issues.",
                List.of("Severity", "Type", "Rule", "Location", "Message"), "| --- | --- | --- | --- | --- |",
                view.issues(), issue -> List.of(issue.severity(), issue.type(), code(issue.rule()),
                    code(issue.location()), issue.message()));
        }
        if (view.has(ReportSection.HOTSPOTS)) {
            section(text, heading(ReportIcons.HOTSPOTS, "Security hotspots (" + view.hotspots().size() + ")"),
                "No security hotspots.", List.of("Probability", "Category", "Rule", "Location", "Message", "Status"),
                "| --- | --- | --- | --- | --- | --- |", view.hotspots(),
                hotspot -> List.of(hotspot.probability(), hotspot.category(), code(hotspot.rule()),
                    code(hotspot.location()), hotspot.message(), hotspot.status()));
        }
    }

    private static void details(StringBuilder text, ReportView view) {
        if (view.has(ReportSection.FILES)) {
            section(text, heading(ReportIcons.FILES, "Files (" + view.files().size() + ")"), "No files.",
                List.of("File", "Lines of code", "Coverage", "Uncovered lines", "Duplicated lines (%)", "Open issues"),
                "| --- | ---: | ---: | ---: | ---: | ---: |", view.files(),
                file -> List.of(code(file.path()), file.lines(), file.coverage(), file.uncovered(), file.duplicated(),
                    file.issues()));
        }
        if (view.has(ReportSection.COVERAGE)) {
            section(text, heading(ReportIcons.COVERAGE_LINES, "Uncovered lines (" + view.coverage().size() + ")"),
                view.sourceHidden() ? ReportXhtml.SOURCE_HIDDEN : "Every line is covered.",
                List.of("File", "Uncovered lines", "Partly covered"), "| --- | --- | --- |", view.coverage(),
                lines -> List.of(code(lines.path()), lines.uncovered(), lines.partial()));
        }
        if (view.has(ReportSection.DUPLICATIONS)) {
            String duplicated = "Duplicated blocks (" + view.duplications().size() + ")";
            section(text, heading(ReportIcons.DUPLICATED_BLOCKS, duplicated),
                view.sourceHidden() ? ReportXhtml.SOURCE_HIDDEN : "No duplicated blocks.",
                List.of("File", "Lines", "Also in"), "| --- | --- | --- |", view.duplications(),
                block -> List.of(code(block.path()), block.lines(), block.copies()));
        }
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
