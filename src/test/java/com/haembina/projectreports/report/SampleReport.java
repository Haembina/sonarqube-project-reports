package com.haembina.projectreports.report;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class SampleReport {

    private SampleReport() {
    }

    static Report full() {
        return new Report(new ReportTarget("acme", "feature/x", null),
            new Report.QualityGate("ERROR", List.of(
                new Report.Condition("new_coverage", "LT", "80", "61.5", "ERROR"))),
            List.of(new Report.Measure("coverage", "72.4"), new Report.Measure("ncloc", null)),
            List.of(
                new Report.Issue("i1", "javascript:S3516", "BLOCKER", "CODE_SMELL", "src/app.js", 2,
                    "Refactor | this <function>", "OPEN", "2026-10-28T07:06:28+0000"),
                new Report.Issue("i2", "javascript:S1135", "INFO", "CODE_SMELL", null, null, "=HYPERLINK(\"x\")",
                    null, null),
                new Report.Issue("i3", "javascript:S1481", "MAJOR", "BUG", "src/b.js", 0, "line\nbreak", "OPEN",
                    null)),
            List.of(new Report.Hotspot("h1", "javascript:S2245", "src/app.js", 5, "Make sure \"random\" is safe",
                "weak-cryptography", "MEDIUM", "TO_REVIEW", null)),
            List.of(new Report.FileMeasures("acme:src/app.js", "src/app.js",
                Map.of("coverage", "50.0", "ncloc", "12", "violations", "2"))));
    }

    static Report detailed(Set<ReportSection> sections, boolean sourceHidden) {
        return new Report(new ReportTarget("acme", null, null), sections, new Report.QualityGate("OK", List.of()),
            List.of(), List.of(), List.of(), List.of(),
            sourceHidden ? List.of() : List.of(new Report.LineCoverage("src/a.js", List.of(2, 3, 4, 9),
                List.of(new Report.PartLine(6, 1, 2)))),
            sourceHidden ? List.of() : List.of(new Report.Duplication("src/a.js", 10, 24,
                List.of(new Report.Place("src/b.js", 2, 16)))),
            sourceHidden);
    }

    static Report empty() {
        return new Report(new ReportTarget("acme", null, "7"), new Report.QualityGate(null, List.of()), List.of(),
            List.of(), List.of(), List.of());
    }
}
