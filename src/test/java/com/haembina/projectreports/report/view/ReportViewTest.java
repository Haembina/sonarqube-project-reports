package com.haembina.projectreports.report.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.ReportTarget;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportViewTest {

    private static Report report(ReportTarget target, String gate) {
        return new Report(target,
            new Report.QualityGate(gate, List.of(
                new Report.Condition("new_coverage", "LT", "80", "61.5", "ERROR"),
                new Report.Condition("new_violations", "GT", "0", "0", "OK"))),
            List.of(new Report.Measure("new_lines", "3"), new Report.Measure("coverage", "72.4"),
                new Report.Measure("new_coverage", "61.5"),
                new Report.Measure("sqale_rating", "2.0")),
            List.of(new Report.Issue("i1", "javascript:S3516", "BLOCKER", "CODE_SMELL", "src/app.js", 2,
                "Refactor this", "OPEN", null)),
            List.of(new Report.Hotspot("h1", "javascript:S2245", "src/app.js", 5, "Make sure random is safe",
                "weak-cryptography", "MEDIUM", "TO_REVIEW", null)),
            List.of(new Report.FileMeasures("acme:src/app.js", "src/app.js",
                Map.of("coverage", "50.0", "ncloc", "1200", "violations", "2"))));
    }

    @Test
    void readsAReportInWords() {
        ReportView view = ReportView.of(report(new ReportTarget("acme", "feature/x", null), "ERROR"));

        assertThat(view.project()).isEqualTo("acme");
        assertThat(view.scope()).isEqualTo("Branch feature/x");
        assertThat(view.gate()).isEqualTo(new ReportView.Gate("Failed", "fail"));
        assertThat(view.conditions()).containsExactly(
            new ReportView.Condition("Coverage on new code", "at least 80 %", "61.5 %", "Failed", "fail"),
            new ReportView.Condition("Issues on new code", "at most 0", "0", "Passed", "pass"));
        assertThat(view.measures()).containsExactly(
            new ReportView.Measure("Lines of code", "code-row", "—", "3", false),
            new ReportView.Measure("Coverage", "umbrella-row", "72.4 %", "61.5 %", false),
            new ReportView.Measure("Maintainability rating", "radioactive-row", "B", "—", true));
        assertThat(view.issues()).containsExactly(
            new ReportView.Issue("Blocker", "circle-x-row", "Code smell", "radioactive-row", "javascript:S3516",
                "src/app.js:2", "Refactor this"));
        assertThat(view.hotspots()).containsExactly(new ReportView.Hotspot("Medium", "Weak-cryptography",
            "javascript:S2245", "src/app.js:5", "Make sure random is safe", "To review"));
        assertThat(view.files()).containsExactly(new ReportView.File("src/app.js", "1,200", "50 %", "—", "—", "2"));
    }

    @Test
    void writesEachUncoveredLineAndDuplicatedBlockInWords() {
        Report report = new Report(new ReportTarget("acme", null, null), EnumSet.of(ReportSection.COVERAGE),
            new Report.QualityGate("OK", List.of()), List.of(), List.of(), List.of(), List.of(),
            List.of(new Report.LineCoverage("a.js", List.of(2, 3, 9), List.of(new Report.PartLine(6, 1, 2),
                new Report.PartLine(8, 0, 4))), new Report.LineCoverage("b.js", List.of(1), List.of())),
            List.of(new Report.Duplication("a.js", 10, 24, List.of(new Report.Place("b.js", 2, 16),
                new Report.Place("c.js", 5, 5))), new Report.Duplication("d.js", 1, 2, List.of())),
            true);

        ReportView view = ReportView.of(report);

        assertThat(view.coverage()).containsExactly(
            new ReportView.CoverageRow("a.js", "2–3, 9", "6 (1 of 2 conditions), 8 (0 of 4 conditions)"),
            new ReportView.CoverageRow("b.js", "1", "—"));
        assertThat(view.duplications()).containsExactly(
            new ReportView.DuplicationRow("a.js", "10–24", "b.js 2–16; c.js 5"),
            new ReportView.DuplicationRow("d.js", "1–2", "—"));
        assertThat(view.sourceHidden()).isTrue();
        assertThat(view.has(ReportSection.COVERAGE)).isTrue();
        assertThat(view.has(ReportSection.GATE)).isFalse();
    }

    @Test
    void namesAPullRequestByItsTitleWhenItHasOne() {
        ReportTarget target = new ReportTarget("acme", null, "7");

        assertThat(ReportView.scope(target, "Add reports")).isEqualTo("Pull request 7 – Add reports");
        assertThat(ReportView.scope(target, " ")).isEqualTo("Pull request 7");
        assertThat(ReportView.of(new Report(target, EnumSet.of(ReportSection.GATE),
            new Report.QualityGate("OK", List.of()), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            false, new Report.Links(null, null, null, null, "Add reports"))).scope())
            .isEqualTo("Pull request 7 – Add reports");
    }

    @Test
    void namesEachScope() {
        assertThat(ReportView.scope(new ReportTarget("acme", null, "7"))).isEqualTo("Pull request 7");
        assertThat(ReportView.scope(new ReportTarget("acme", "main", null))).isEqualTo("Branch main");
        assertThat(ReportView.scope(new ReportTarget("acme", null, null))).isEqualTo("Main branch");
    }

    @Test
    void namesEachGateStatus() {
        assertThat(ReportView.gate("OK")).isEqualTo(new ReportView.Gate("Passed", "pass"));
        assertThat(ReportView.gate("ERROR")).isEqualTo(new ReportView.Gate("Failed", "fail"));
        assertThat(ReportView.gate("NONE")).isEqualTo(new ReportView.Gate("Not computed", "none"));
        assertThat(ReportView.gate(null)).isEqualTo(new ReportView.Gate("Not computed", "none"));
    }

    @Test
    void placesAFindingByFileAndLine() {
        assertThat(ReportView.location("src/a.js", 4)).isEqualTo("src/a.js:4");
        assertThat(ReportView.location("src/a.js", 0)).isEqualTo("src/a.js");
        assertThat(ReportView.location("src/a.js", null)).isEqualTo("src/a.js");
        assertThat(ReportView.location(null, 4)).isEqualTo("—");
    }

    @Test
    void keepsItsOwnCopyOfEachList() {
        List<ReportView.Issue> issues = new ArrayList<>();
        ReportView view = new ReportView("acme", "Main branch", ReportView.gate(null), new ArrayList<>(),
            new ArrayList<>(), issues, new ArrayList<>(), new ArrayList<>());

        issues.add(new ReportView.Issue("Info", "", "Bug", "", "r", "l", "m"));

        assertThat(view.issues()).isEmpty();
    }
}
