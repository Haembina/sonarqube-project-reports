package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatusLineRendererTest {

    private static String render(Report report) {
        return new String(new StatusLineRenderer().render(report), StandardCharsets.UTF_8);
    }

    private static Report withMeasures(List<Report.Measure> measures, Report.Links links) {
        return new Report(new ReportTarget("acme", "feature/x", null), ReportSection.all(),
            new Report.QualityGate("ERROR", List.of()), measures, List.of(), List.of(), List.of(), List.of(), List.of(),
            false, links);
    }

    @Test
    void saysTheVerdictCoverageOnNewCodeAndIssueCountWithALinkToSonarQube() {
        Report report = withMeasures(List.of(new Report.Measure("coverage", "72.4"),
            new Report.Measure("new_coverage", "61.5")), SampleReport.githubLinks());

        assertThat(render(report)).isEqualTo("**acme** · Quality gate: ✗ Failed · Coverage on new code: 61.5 % · "
            + "Issues: 0 · [Branch feature/x](https://sonar.test/dashboard?id=acme&branch=feature/x)\n");
    }

    @Test
    void leavesOutWhatTheReportDoesNotHold() {
        Report bare = new Report(new ReportTarget("acme", null, null), EnumSet.of(ReportSection.MEASURES),
            new Report.QualityGate("OK", List.of()), List.of(new Report.Measure("new_coverage", null)), List.of(),
            List.of(), List.of(), List.of(), List.of(), false,
            new Report.Links(null, null, "https://git.acme.io/acme", null, null));

        assertThat(render(bare)).isEqualTo("**acme** · [Main branch](https://git.acme.io/acme)\n");
        assertThat(render(SampleReport.empty())).isEqualTo("**acme** · Quality gate: Not computed · Issues: 0 · "
            + "Pull request 7\n");
    }
}
