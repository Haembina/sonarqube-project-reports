package com.haembina.projectreports.report.markdown;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.view.ReportView;
import java.util.List;
import javax.annotation.Nullable;

final class MarkdownText {

    static final String BREAK = Character.toString(0x200B);
    private static final String SLASH = "/";

    private MarkdownText() {
    }

    static String cell(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|").replace("\r", "").replace("\n", " ")
            .replace("<", "&lt;").replace(">", "&gt;");
    }

    static String code(String value) {
        return "`" + value.replace("`", "'").replace("|", "\\|").replace("\n", " ") + "`";
    }

    static String path(String value) {
        return code(breakable(value));
    }

    static String linkedPath(String value, @Nullable String url) {
        return url == null ? path(value) : "[" + path(value) + "](" + address(url) + ")";
    }

    static String breakable(String value) {
        return value.replace(SLASH, SLASH + BREAK);
    }

    static String link(@Nullable String url, String text) {
        return url == null ? cell(text)
            : "[" + cell(text).replace("[", "\\[").replace("]", "\\]") + "](" + address(url) + ")";
    }

    static String status(String label, String tone) {
        return switch (tone) {
            case "pass" -> "✓ " + label;
            case "fail" -> "✗ " + label;
            default -> label;
        };
    }

    static String row(List<String> cells) {
        StringBuilder text = new StringBuilder("|");
        cells.forEach(cell -> text.append(' ').append(cell).append(" |"));
        return text.append('\n').toString();
    }

    static String scopeLine(ReportView view) {
        Report.Links links = view.links();
        String repository = links.repository() == null ? "" : link(links.repositoryUrl(), links.repository()) + " · ";
        String sonar = links.sonarUrl() == null ? ""
            : " · " + link(links.sonarUrl(), Message.REPORT_OPEN_IN_SONARQUBE.text());
        return repository + link(links.scopeUrl(), view.scope()) + sonar;
    }

    private static String address(String url) {
        return url.replace(" ", "%20").replace(")", "%29");
    }
}
