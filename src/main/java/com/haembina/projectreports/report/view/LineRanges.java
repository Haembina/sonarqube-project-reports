package com.haembina.projectreports.report.view;

import com.haembina.projectreports.report.Report;
import java.util.ArrayList;
import java.util.List;

public final class LineRanges {

    private static final String NONE = "—";

    private LineRanges() {
    }

    public static String text(List<Integer> lines) {
        if (lines.isEmpty()) {
            return NONE;
        }
        List<String> ranges = new ArrayList<>();
        int start = lines.get(0);
        int end = start;
        for (int line : lines.subList(1, lines.size())) {
            if (line == end + 1) {
                end = line;
            } else {
                ranges.add(span(start, end));
                start = line;
                end = line;
            }
        }
        ranges.add(span(start, end));
        return String.join(", ", ranges);
    }

    public static String span(int from, int to) {
        return from == to ? String.valueOf(from) : from + "–" + to;
    }

    static String partial(Report.PartLine line) {
        return line.line() + " (" + line.covered() + " of " + line.conditions() + " conditions)";
    }
}
