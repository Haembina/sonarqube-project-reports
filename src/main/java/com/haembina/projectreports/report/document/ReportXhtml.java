package com.haembina.projectreports.report.document;

import com.haembina.projectreports.l10n.Language;
import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.view.ReportIcons;
import com.haembina.projectreports.report.view.ReportView;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javax.annotation.Nullable;

public final class ReportXhtml {

    public interface Embedding {

        String fontFaces();

        String imageSource(String asset);
    }

    private static final String CELL = "<td class=\"report-table__cell\">";
    private static final String CELL_END = "</td>";
    private static final String SPAN_END = "</span>";
    private static final String NUMBER = "number";
    private static final String RATINGS = "ABCDE";
    private static final String SEPARATOR = " · ";
    private static final List<String> STATUS_TONES = List.of("pass", "fail", "none");

    private ReportXhtml() {
    }

    public static String language() {
        Language language = Language.DEFAULT;
        return "lang=\"" + language.code() + "\" xml:lang=\"" + language.code() + "\" dir=\"" + language.direction()
            + "\"";
    }

    public static String document(ReportView view, Embedding embedding) {
        StringBuilder html = new StringBuilder(
            "<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\" " + language() + ">\n<head>\n")
            .append("<meta charset=\"utf-8\" />\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n")
            .append("<title>").append(escape(view.project() + " – " + view.scope())).append("</title>\n")
            .append("<style>\n").append(embedding.fontFaces()).append(ReportStyles.resolved())
            .append(iconRules(embedding)).append("</style>\n")
            .append("</head>\n<body>\n<main class=\"report\">\n");
        html.append("<p class=\"report__source\"><a class=\"report__link\" href=\"").append(ReportView.PLUGIN_URL)
            .append("\"><img class=\"report__source-icon\" src=\"").append(embedding.imageSource(ReportAssets.ICON))
            .append("\" alt=\"\" />").append(ReportView.PLUGIN_NAME).append("</a></p>\n")
            .append("<h1 class=\"report__title\">").append(escape(view.project())).append("</h1>\n")
            .append("<div class=\"report__meta\"><p class=\"report__scope\">").append(scopeLine(view))
            .append("</p>");
        if (view.has(ReportSection.GATE)) {
            html.append("<p class=\"report__gate\">").append(escape(Message.REPORT_GATE.text())).append(' ')
                .append(badge(view.gate().label(), view.gate().tone())).append("</p>");
        }
        html.append("</div>\n");
        findings(html, view);
        details(html, view);
        return html.append("<footer class=\"report__brand\"><span class=\"report__brand-lead\">")
            .append(escape(Message.REPORT_DEVELOPED_BY.text())).append(SPAN_END)
            .append("<a class=\"report__brand-link\" href=\"").append(ReportView.BRAND_URL)
            .append("\"><img class=\"report__brand-mark\" src=\"").append(embedding.imageSource(ReportAssets.MARK))
            .append("\" alt=\"\" /><span class=\"report__brand-name\">")
            .append(ReportView.BRAND_NAME).append("</span></a></footer>\n</main>\n</body>\n</html>\n")
            .toString();
    }

    private static String scopeLine(ReportView view) {
        Report.Links links = view.links();
        String repository = links.repository() == null ? ""
            : link(links.repositoryUrl(), links.repository()) + SEPARATOR;
        String sonar = links.sonarUrl() == null ? ""
            : SEPARATOR + "<a class=\"report__link report__sonar\" href=\"" + escape(links.sonarUrl()) + "\">"
                + escape(Message.REPORT_OPEN_IN_SONARQUBE.text()) + "</a>";
        return repository + link(links.scopeUrl(), view.scope()) + sonar;
    }

    private static String link(@Nullable String url, String text) {
        return url == null ? escape(text)
            : "<a class=\"report__link\" href=\"" + escape(url) + "\">" + escape(text) + "</a>";
    }

    private static void findings(StringBuilder html, ReportView view) {
        if (view.has(ReportSection.GATE)) {
            section(html, ReportIcons.GATE, Message.HEADING_CONDITIONS.text(), Message.EMPTY_CONDITIONS.text(),
                List.of(head(Message.COLUMN_CONDITION), head(Message.COLUMN_REQUIRED), head(Message.COLUMN_ACTUAL),
                    head(Message.COLUMN_STATUS)), view.conditions(),
                condition -> List.of(cell(condition.metric()), cell(condition.expected()), cell(condition.actual()),
                    CELL + badge(condition.status(), condition.tone()) + CELL_END));
        }
        if (view.has(ReportSection.MEASURES)) {
            section(html, ReportIcons.MEASURES, Message.HEADING_MEASURES.text(), Message.EMPTY_MEASURES.text(),
                List.of(head(Message.COLUMN_METRIC), number(Message.COLUMN_OVERALL), number(Message.COLUMN_NEW_CODE)),
                view.measures(),
                measure -> List.of(label(measure.label(), measure.icon()), value(measure.overall(), measure.rating()),
                    value(measure.newCode(), measure.rating())));
        }
        if (view.has(ReportSection.ISSUES)) {
            section(html, ReportIcons.ISSUES, Message.HEADING_ISSUES.text(view.issues().size()),
                Message.EMPTY_ISSUES.text(),
                List.of(head(Message.COLUMN_SEVERITY), head(Message.COLUMN_TYPE), head(Message.COLUMN_ISSUE)),
                view.issues(),
                issue -> List.of(label(issue.severity(), issue.severityIcon()), label(issue.type(), issue.typeIcon()),
                    finding(issue.message(), issue.rule(), issue.location())));
        }
        if (view.has(ReportSection.HOTSPOTS)) {
            section(html, ReportIcons.HOTSPOTS, Message.HEADING_HOTSPOTS.text(view.hotspots().size()),
                Message.EMPTY_HOTSPOTS.text(),
                List.of(head(Message.COLUMN_PROBABILITY), head(Message.COLUMN_CATEGORY), head(Message.COLUMN_STATUS),
                    head(Message.COLUMN_HOTSPOT)), view.hotspots(),
                hotspot -> List.of(label(hotspot.probability(), ""), label(hotspot.category(), ""),
                    label(hotspot.status(), ""), finding(hotspot.message(), hotspot.rule(), hotspot.location())));
        }
    }

    private static void details(StringBuilder html, ReportView view) {
        if (view.has(ReportSection.FILES)) {
            section(html, ReportIcons.FILES, Message.HEADING_FILES.text(view.files().size()),
                Message.EMPTY_FILES.text(),
                List.of(head(Message.COLUMN_FILE), number(Message.METRIC_NCLOC), number(Message.METRIC_COVERAGE),
                    number(Message.COLUMN_UNCOVERED_LINES), number(Message.METRIC_DUPLICATED_LINES_DENSITY),
                    number(Message.COLUMN_OPEN_ISSUES)), view.files(),
                file -> List.of(code(file.path()), numeric(file.lines()), numeric(file.coverage()),
                    numeric(file.uncovered()), numeric(file.duplicated()), numeric(file.issues())));
        }
        if (view.has(ReportSection.COVERAGE)) {
            section(html, ReportIcons.COVERAGE_LINES, Message.HEADING_COVERAGE.text(view.coverage().size()),
                view.sourceHidden() ? Message.REPORT_SOURCE_HIDDEN.text() : Message.EMPTY_COVERAGE.text(),
                List.of(head(Message.COLUMN_FILE), head(Message.COLUMN_UNCOVERED_LINES),
                    head(Message.COLUMN_PARTLY_COVERED)), view.coverage(),
                lines -> List.of(code(lines.path()), cell(lines.uncovered()), cell(lines.partial())));
        }
        if (view.has(ReportSection.DUPLICATIONS)) {
            section(html, ReportIcons.DUPLICATED_BLOCKS, Message.HEADING_DUPLICATIONS.text(view.duplications().size()),
                view.sourceHidden() ? Message.REPORT_SOURCE_HIDDEN.text() : Message.EMPTY_DUPLICATIONS.text(),
                List.of(head(Message.COLUMN_FILE), head(Message.COLUMN_LINES), head(Message.COLUMN_ALSO_IN)),
                view.duplications(),
                block -> List.of(code(block.path()), cell(block.lines()), cell(block.copies())));
        }
    }

    static String iconRules(Embedding embedding) {
        StringBuilder rules = new StringBuilder();
        for (String icon : ReportIcons.ALL) {
            rules.append(".report-icon--").append(icon).append(" { background-image: url(\"")
                .append(embedding.imageSource(ReportAssets.iconFile(icon))).append("\"); }\n");
        }
        return rules.toString();
    }

    private static String icon(String name) {
        return "<span class=\"report-icon report-icon--" + name + "\"></span>";
    }

    private static <T> void section(StringBuilder html, String icon, String title, String empty, List<String> heads,
        List<T> rows, Function<T, List<String>> cells) {
        html.append("<h2 class=\"report__section\">").append(icon(icon)).append(escape(title)).append("</h2>\n");
        if (rows.isEmpty()) {
            html.append("<p class=\"report__empty\">").append(escape(empty)).append("</p>\n");
            return;
        }
        html.append("<table class=\"report-table\">\n<thead><tr>").append(String.join("", heads))
            .append("</tr></thead>\n<tbody>\n");
        for (T row : rows) {
            html.append("<tr>").append(String.join("", cells.apply(row))).append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");
    }

    private static String head(Message text) {
        return "<th class=\"report-table__head\">" + escape(text.text()) + "</th>";
    }

    private static String number(Message text) {
        return "<th class=\"report-table__head report-table__head--" + NUMBER + "\">" + escape(text.text()) + "</th>";
    }

    private static String cell(String text) {
        return CELL + escape(text) + CELL_END;
    }

    private static String numeric(String text) {
        return "<td class=\"report-table__cell report-table__cell--" + NUMBER + "\">" + escape(text) + CELL_END;
    }

    private static String label(String text, String icon) {
        String iconed = icon.isEmpty() ? "" : " report-table__cell--iconed report-icon--" + icon;
        return "<td class=\"report-table__cell report-table__cell--label" + iconed + "\">" + escape(text) + CELL_END;
    }

    private static String code(String text) {
        return CELL + codeText(text) + CELL_END;
    }

    private static String codeText(String text) {
        return "<span class=\"report-table__code\">" + escape(text) + SPAN_END;
    }

    private static String finding(String message, String rule, String location) {
        return "<td class=\"report-table__cell report-table__cell--finding\">" + escape(message)
            + "<span class=\"report-table__where\">" + codeText(rule) + " · " + codeText(location) + SPAN_END
            + CELL_END;
    }

    private static String value(String text, boolean rating) {
        if (rating && text.length() == 1 && RATINGS.contains(text)) {
            return "<td class=\"report-table__cell report-table__cell--" + NUMBER + "\">"
                + badge(text, text.toLowerCase(Locale.ROOT)) + CELL_END;
        }
        return numeric(text);
    }

    private static String badge(String label, String tone) {
        String mark = STATUS_TONES.contains(tone) ? icon(ReportIcons.forTone(tone)) : "";
        return "<span class=\"report-badge report-badge--" + tone + "\">" + mark + escape(label) + SPAN_END;
    }

    public static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
