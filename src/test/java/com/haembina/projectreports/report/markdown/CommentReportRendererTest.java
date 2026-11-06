package com.haembina.projectreports.report.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.report.GitProvider;
import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.ReportTarget;
import com.haembina.projectreports.report.SampleReport;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CommentReportRendererTest {

    private static String render(Optional<GitProvider> flavor, Report report) {
        return new String(new CommentReportRenderer(flavor).render(report), StandardCharsets.UTF_8);
    }

    private static Report linked(Report.Links links) {
        return SampleReport.linked(SampleReport.full(), links);
    }

    private static Report.Links withProvider(GitProvider provider) {
        Report.Links links = SampleReport.githubLinks();
        return new Report.Links(links.repository(), links.repositoryUrl(), links.scopeUrl(), links.sonarUrl(), null,
            provider, links.linePattern());
    }

    @Test
    void opensWithTheVerdictAndKeepsTheConditionsInSight() {
        assertThat(render(Optional.of(GitProvider.GITHUB), linked(SampleReport.githubLinks()))).startsWith("""
            ### acme · Quality gate: ✗ Failed

            [Acme/app](https://github.com/Acme/app) · [Branch feature/x](https://github.com/Acme/app/tree/feature/x) · \
            [Open in SonarQube](https://sonar.test/dashboard?id=acme&branch=feature/x)

            #### Quality gate conditions

            | Condition | Required | Actual | Status |
            | --- | --- | ---: | --- |
            | Coverage on new code | at least 80 % | 61.5 % | ✗ Failed |

            <details><summary>Measures</summary>

            | Metric | Overall | New code |
            """);
    }

    @Test
    void foldsEachLongSectionWhereThePlatformRendersDetails() {
        String comment = render(Optional.of(GitProvider.GITEA), linked(SampleReport.githubLinks()));

        assertThat(comment).contains("""
            <details><summary>Issues (3)</summary>

            | Severity | Type | Rule | Location | Message |
            | --- | --- | --- | --- | --- |
            | Blocker | Code smell | `javascript:S3516` | \
            [`src/app.js:2`](https://github.com/Acme/app/blob/abc/src/app.js#L2) | Refactor \\| this &lt;function&gt; |
            """).contains("| `src/app.js` | 12 | 50 % | — | — | 2 |\n\n</details>\n")
            .doesNotContain("#### Measures", "<img", "<p ");
    }

    @Test
    void writesPlainHeadingsWhereThePlatformStripsHtml() {
        String comment = render(Optional.of(GitProvider.BITBUCKET_CLOUD), linked(SampleReport.githubLinks()));

        assertThat(comment).contains("#### Measures\n\n| Metric |", "#### Issues (3)\n\n", "#### Files (1)\n\n")
            .doesNotContain("<details>", "</details>", "<summary>");
    }

    @Test
    void takesTheProjectsOwnPlatformWhenAskedForAuto() {
        assertThat(render(Optional.empty(), linked(withProvider(GitProvider.GITLAB)))).contains("<details>");
        assertThat(render(Optional.empty(), linked(withProvider(GitProvider.AZURE_DEVOPS))))
            .doesNotContain("<details>");
        assertThat(render(Optional.empty(), SampleReport.full())).doesNotContain("<details>")
            .contains("\n\nBranch feature/x\n\n", "#### Issues (3)");
        assertThat(render(Optional.of(GitProvider.BITBUCKET_SERVER), linked(withProvider(GitProvider.GITHUB))))
            .doesNotContain("<details>");
    }

    @Test
    void saysSoForAnEmptySectionAndOmitsAnUnchosenGate() {
        assertThat(render(Optional.of(GitProvider.GITHUB), SampleReport.empty()))
            .startsWith("### acme · Quality gate: Not computed\n")
            .contains("#### Quality gate conditions\n\nNo conditions apply to this scope.\n")
            .contains("<details><summary>Issues (0)</summary>\n\nNo open issues.\n\n</details>\n");
        assertThat(render(Optional.empty(), SampleReport.detailed(EnumSet.of(ReportSection.COVERAGE), false)))
            .startsWith("### acme\n\nMain branch\n\n#### Uncovered lines (1)\n");
    }

    @Test
    void cutsRowsToFitThePlatformsLimitAndSaysHowManyAreLeft() {
        String message = "x".repeat(200);
        List<Report.Issue> issues = IntStream.range(0, 1_000).mapToObj(index -> new Report.Issue("i" + index,
            "js:S1", "MAJOR", "BUG", "src/a.js", index + 1, message, "OPEN", null)).toList();
        Report base = new Report(new ReportTarget("acme", null, "7"), EnumSet.of(ReportSection.ISSUES,
            ReportSection.HOTSPOTS), new Report.QualityGate("OK", List.of()), List.of(), issues,
            List.of(new Report.Hotspot("h1", "js:S2", "src/a.js", 1, "m", "dos", "LOW", "TO_REVIEW", null)),
            List.of(), List.of(), List.of(), false);

        String comment = render(Optional.of(GitProvider.GITHUB), SampleReport.linked(base, SampleReport.githubLinks()));
        String shown = comment.substring(comment.indexOf("<summary>Issues"), comment.indexOf("more in SonarQube"));
        long rows = shown.lines().filter(line -> line.startsWith("| Major")).count();

        assertThat(comment.length()).isLessThan(CommentReportRenderer.LIMIT);
        assertThat(rows).isBetween(1L, 999L);
        assertThat(comment).contains("\n[" + (1_000 - rows) + " more in SonarQube]"
                + "(https://sonar.test/dashboard?id=acme&branch=feature/x)\n")
            .contains("<summary>Security hotspots (1)</summary>\n\n\n[1 more in SonarQube](")
            .doesNotContain("| Low |");
    }

    @Test
    void endsWithEveryRowWhenTheyFit() {
        assertThat(render(Optional.of(GitProvider.GITHUB), linked(SampleReport.githubLinks())))
            .doesNotContain("more in SonarQube");
    }
}
