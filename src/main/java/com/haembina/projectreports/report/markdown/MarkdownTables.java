package com.haembina.projectreports.report.markdown;

import static com.haembina.projectreports.report.markdown.MarkdownText.cell;
import static com.haembina.projectreports.report.markdown.MarkdownText.code;
import static com.haembina.projectreports.report.markdown.MarkdownText.linkedCode;
import static com.haembina.projectreports.report.markdown.MarkdownText.status;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.view.ReportIcons;
import com.haembina.projectreports.report.view.ReportView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

final class MarkdownTables {

    private MarkdownTables() {
    }

    record Table(ReportSection section, String icon, String title, String empty, List<String> heads, String divider,
        List<List<String>> rows) {

        Table {
            heads = List.copyOf(heads);
            rows = List.copyOf(rows);
        }

        String header() {
            return MarkdownText.row(heads) + divider + "\n";
        }
    }

    static List<Table> of(ReportView view) {
        List<Table> tables = new ArrayList<>();
        add(tables, view, new Table(ReportSection.GATE, ReportIcons.GATE, Message.HEADING_CONDITIONS.text(),
            Message.EMPTY_CONDITIONS.text(), heads(Message.COLUMN_CONDITION, Message.COLUMN_REQUIRED,
                Message.COLUMN_ACTUAL, Message.COLUMN_STATUS), "| --- | --- | ---: | --- |",
            rows(view.conditions(), condition -> List.of(cell(condition.metric()), cell(condition.expected()),
                cell(condition.actual()), cell(status(condition.status(), condition.tone()))))));
        add(tables, view, new Table(ReportSection.MEASURES, ReportIcons.MEASURES, Message.HEADING_MEASURES.text(),
            Message.EMPTY_MEASURES.text(), heads(Message.COLUMN_METRIC, Message.COLUMN_OVERALL,
                Message.COLUMN_NEW_CODE), "| --- | ---: | ---: |",
            rows(view.measures(), measure -> List.of(cell(measure.label()), cell(measure.overall()),
                cell(measure.newCode())))));
        add(tables, view, new Table(ReportSection.ISSUES, ReportIcons.ISSUES,
            Message.HEADING_ISSUES.text(view.issues().size()), Message.EMPTY_ISSUES.text(),
            heads(Message.COLUMN_SEVERITY, Message.COLUMN_TYPE, Message.COLUMN_RULE, Message.COLUMN_LOCATION,
                Message.COLUMN_MESSAGE), "| --- | --- | --- | --- | --- |",
            rows(view.issues(), issue -> List.of(cell(issue.severity()), cell(issue.type()), code(issue.rule()),
                linkedCode(issue.location(), issue.locationUrl()), cell(issue.message())))));
        add(tables, view, new Table(ReportSection.HOTSPOTS, ReportIcons.HOTSPOTS,
            Message.HEADING_HOTSPOTS.text(view.hotspots().size()), Message.EMPTY_HOTSPOTS.text(),
            heads(Message.COLUMN_PROBABILITY, Message.COLUMN_CATEGORY, Message.COLUMN_RULE, Message.COLUMN_LOCATION,
                Message.COLUMN_MESSAGE, Message.COLUMN_STATUS), "| --- | --- | --- | --- | --- | --- |",
            rows(view.hotspots(), hotspot -> List.of(cell(hotspot.probability()), cell(hotspot.category()),
                code(hotspot.rule()), linkedCode(hotspot.location(), hotspot.locationUrl()), cell(hotspot.message()),
                cell(hotspot.status())))));
        add(tables, view, new Table(ReportSection.FILES, ReportIcons.FILES,
            Message.HEADING_FILES.text(view.files().size()), Message.EMPTY_FILES.text(),
            heads(Message.COLUMN_FILE, Message.METRIC_NCLOC, Message.METRIC_COVERAGE, Message.COLUMN_UNCOVERED_LINES,
                Message.METRIC_DUPLICATED_LINES_DENSITY, Message.COLUMN_OPEN_ISSUES),
            "| --- | ---: | ---: | ---: | ---: | ---: |",
            rows(view.files(), file -> List.of(code(file.path()), cell(file.lines()), cell(file.coverage()),
                cell(file.uncovered()), cell(file.duplicated()), cell(file.issues())))));
        add(tables, view, new Table(ReportSection.COVERAGE, ReportIcons.COVERAGE_LINES,
            Message.HEADING_COVERAGE.text(view.coverage().size()), hiddenOr(view, Message.EMPTY_COVERAGE),
            heads(Message.COLUMN_FILE, Message.COLUMN_UNCOVERED_LINES, Message.COLUMN_PARTLY_COVERED),
            "| --- | --- | --- |", rows(view.coverage(), lines -> List.of(code(lines.path()), cell(lines.uncovered()),
                cell(lines.partial())))));
        add(tables, view, new Table(ReportSection.DUPLICATIONS, ReportIcons.DUPLICATED_BLOCKS,
            Message.HEADING_DUPLICATIONS.text(view.duplications().size()), hiddenOr(view, Message.EMPTY_DUPLICATIONS),
            heads(Message.COLUMN_FILE, Message.COLUMN_LINES, Message.COLUMN_ALSO_IN), "| --- | --- | --- |",
            rows(view.duplications(), block -> List.of(code(block.path()), cell(block.lines()),
                cell(block.copies())))));
        return List.copyOf(tables);
    }

    private static void add(List<Table> tables, ReportView view, Table table) {
        if (view.has(table.section())) {
            tables.add(table);
        }
    }

    private static String hiddenOr(ReportView view, Message empty) {
        return view.sourceHidden() ? Message.REPORT_SOURCE_HIDDEN.text() : empty.text();
    }

    private static List<String> heads(Message... columns) {
        return Arrays.stream(columns).map(column -> cell(column.text())).toList();
    }

    private static <T> List<List<String>> rows(List<T> items, Function<T, List<String>> cells) {
        return items.stream().map(cells).toList();
    }
}
