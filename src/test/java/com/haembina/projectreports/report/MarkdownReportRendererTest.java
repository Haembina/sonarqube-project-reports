package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarkdownReportRendererTest {

    private static final String ASSETS =
        "https://raw.githubusercontent.com/Haembina/sonarqube-project-reports/main/src/main/resources/report/";

    private static String render(Report report) {
        return new String(new MarkdownReportRenderer().render(report), StandardCharsets.UTF_8);
    }

    @Test
    void opensWithThePluginIconAndLinkCenteredAboveTheScopeAndGate() {
        assertThat(render(SampleReport.full())).startsWith("""
            <p align="center"><a href="https://github.com/Haembina/sonarqube-project-reports"><img \
            src="{assets}plugin-icon.png" alt="" width="24" height="24" /> <b>Haembina Project Reports</b></a></p>

            # acme

            Branch feature/x · Quality gate: **✗ Failed**
            """.replace("{assets}", ASSETS));
    }

    @Test
    void writesEverySectionAsATable() {
        String markdown = render(SampleReport.full());

        assertThat(markdown)
            .contains("""
                ## <img src="{assets}icons/shield-check-section.png" alt="" width="20" height="20" /> \
                Quality gate conditions

                | Condition | Required | Actual | Status |
                | --- | --- | ---: | --- |
                | Coverage on new code | at least 80 % | 61.5 % | ✗ Failed |
                """.replace("{assets}", ASSETS))
            .contains("| Lines of code | — | — |\n| Coverage | 72.4 % | — |\n")
            .contains(" Issues (3)\n", " Security hotspots (1)\n", " Files (1)\n")
            .contains("| Blocker | Code smell | `javascript:S3516` | `src/app.js:2` | Refactor \\| this &lt;function&gt; |")
            .contains("| Info | Code smell | `javascript:S1135` | `—` | =HYPERLINK(\"x\") |")
            .contains("| `src/b.js` | line break |")
            .contains("| Medium | Weak-cryptography | `javascript:S2245` | `src/app.js:5` | Make sure \"random\" is safe "
                + "| To review |")
            .contains("| `src/app.js` | 12 | 50 % | — | — | 2 |");
    }

    @Test
    void closesWithDevelopedByAndTheBrandCentered() {
        assertThat(render(SampleReport.full())).endsWith("""

            ---

            <p align="center">Developed by <a href="https://haembina.com"><img \
            src="https://haembina.com/assets/branding/logo.png" alt="" width="20" height="20" /> \
            <b>Haembina</b></a></p>
            """);
    }

    @Test
    void saysSoForAnEmptySection() {
        String markdown = render(SampleReport.empty());

        assertThat(markdown).contains("# acme\n\nPull request 7 · Quality gate: **Not computed**\n")
            .contains(" Quality gate conditions\n\nNo conditions apply to this scope.\n")
            .contains(" Issues (0)\n\nNo open issues.\n");
    }

    @Test
    void holdsOnlyTheChosenSectionsWithEachLineAndBlock() {
        String markdown = render(SampleReport.detailed(EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS),
            false));

        assertThat(markdown).contains("# acme\n\nMain branch\n")
            .doesNotContain("Quality gate", " Measures\n", " Issues (", " Files (")
            .contains("""
                 Uncovered lines (1)

                | File | Uncovered lines | Partly covered |
                | --- | --- | --- |
                | `src/a.js` | 2–4, 9 | 6 (1 of 2 conditions) |
                """)
            .contains("""
                 Duplicated blocks (1)

                | File | Lines | Also in |
                | --- | --- | --- |
                | `src/a.js` | 10–24 | src/b.js 2–16 |
                """);
    }

    @Test
    void saysWhyTheLinesAreMissingOrThatNoneAre() {
        Set<ReportSection> details = EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS);
        Report clean = new Report(new ReportTarget("acme", null, null), details, new Report.QualityGate("OK", List.of()),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false);

        assertThat(render(SampleReport.detailed(details, true))
            .split("These lines are not listed: your account lacks See Source Code permission on this project.", -1))
            .hasSize(3);
        assertThat(render(clean)).contains("Every line is covered.", "No duplicated blocks.");
        assertThat(render(SampleReport.detailed(EnumSet.of(ReportSection.GATE), false)))
            .contains("Quality gate conditions").doesNotContain("Uncovered lines (", "Duplicated blocks (");
    }

    @Test
    void linksTheRepositoryTheScopeAndSonarQube() {
        Report report = new Report(new ReportTarget("acme", null, "7"), EnumSet.of(ReportSection.GATE),
            new Report.QualityGate("OK", List.of()), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            false, new Report.Links("Acme/[app]", "https://git.acme.io/Acme/app", "https://git.acme.io/a b/(7)",
                "https://sonar.test/dashboard?id=acme&pullRequest=7", "Add reports"));

        assertThat(render(report)).contains("""
            # acme

            [Acme/\\[app\\]](https://git.acme.io/Acme/app) · \
            [Pull request 7 – Add reports](https://git.acme.io/a%20b/(7%29) · \
            [Open in SonarQube](https://sonar.test/dashboard?id=acme&pullRequest=7) · Quality gate: **✓ Passed**
            """);
    }

    @Test
    void keepsACodeSpanFromBreakingItsCell() {
        Report report = new Report(new ReportTarget("acme", null, null), new Report.QualityGate("OK", List.of()),
            List.of(), List.of(new Report.Issue("i", "r`1|2", "MINOR", "BUG", "a\nb", 1, "m", null, null)), List.of(),
            List.of());

        assertThat(render(report)).contains("Main branch · Quality gate: **✓ Passed**")
            .contains("| Minor | Bug | `r'1\\|2` | `a b:1` | m |");
    }
}
