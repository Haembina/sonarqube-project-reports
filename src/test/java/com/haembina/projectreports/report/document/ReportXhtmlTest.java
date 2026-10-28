package com.haembina.projectreports.report.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.view.ReportView;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportXhtmlTest {

    private static final ReportXhtml.Embedding EMBEDDING = new ReportXhtml.Embedding() {
        @Override
        public String fontFaces() {
            return "@font-face { font-family: \"Test\"; }\n";
        }

        @Override
        public String imageSource(String asset) {
            return "/" + asset;
        }
    };

    private static ReportView full() {
        return new ReportView("acme <app>", "Pull request 7", new ReportView.Gate("Failed", "fail"),
            List.of(new ReportView.Condition("Coverage on new code", "at least 80 %", "61.5 %", "Failed", "fail"),
                new ReportView.Condition("Issues on new code", "at most 0", "0", "Passed", "pass")),
            List.of(new ReportView.Measure("Coverage", "umbrella-row", "72.4 %", "—", false),
                new ReportView.Measure("Maintainability rating", "radioactive-row", "B", "—", true)),
            List.of(new ReportView.Issue("Blocker", "circle-x-row", "Code smell", "radioactive-row", "js:S1",
                "src/a.js:2", "Refactor \"this\"")),
            List.of(new ReportView.Hotspot("Medium", "Weak-cryptography", "js:S2", "src/a.js:5", "Check it",
                "To review")),
            List.of(new ReportView.File("src/a.js", "12", "50 %", "6", "—", "2")));
    }

    @Test
    void opensWithThePluginIconAndLinkAboveTheScopeAndGate() {
        String html = ReportXhtml.document(full(), EMBEDDING);

        assertThat(html).startsWith("<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\"")
            .contains("<title>acme &lt;app&gt; – Pull request 7</title>")
            .contains("<style>\n@font-face { font-family: \"Test\"; }\n")
            .contains("<p class=\"report__source\"><a class=\"report__link\" "
                + "href=\"https://github.com/Haembina/sonarqube-project-reports\"><img class=\"report__source-icon\" "
                + "src=\"/plugin-icon.png\" alt=\"\" />Haembina Project Reports</a></p>")
            .contains("<h1 class=\"report__title\">acme &lt;app&gt;</h1>")
            .contains("<div class=\"report__meta\"><p class=\"report__scope\">Pull request 7</p>"
                + "<p class=\"report__gate\">Quality gate <span class=\"report-badge report-badge--fail\">"
                + "<span class=\"report-icon report-icon--x-fail\"></span>Failed</span></p></div>");
    }

    @Test
    void declaresEveryIconOnce() {
        assertThat(ReportXhtml.iconRules(EMBEDDING))
            .contains(".report-icon--check-pass { background-image: url(\"/icons/check-pass.png\"); }\n")
            .contains(".report-icon--files-section { background-image: url(\"/icons/files-section.png\"); }\n");
    }

    @Test
    void writesEverySectionAsATableUnderItsIcon() {
        String html = ReportXhtml.document(full(), EMBEDDING);

        assertThat(html)
            .contains("<h2 class=\"report__section\"><span class=\"report-icon report-icon--shield-check-section\">"
                + "</span>Quality gate conditions</h2>")
            .contains("<td class=\"report-table__cell\"><span class=\"report-badge report-badge--pass\">"
                + "<span class=\"report-icon report-icon--check-pass\"></span>Passed</span>")
            .contains("<td class=\"report-table__cell report-table__cell--label report-table__cell--iconed "
                + "report-icon--umbrella-row\">Coverage</td>")
            .contains("<td class=\"report-table__cell report-table__cell--number\">72.4 %</td>")
            .contains("<span class=\"report-badge report-badge--b\">B</span>")
            .contains("<td class=\"report-table__cell report-table__cell--number\">—</td>")
            .contains("<span class=\"report-icon report-icon--alert-circle-section\"></span>Issues (1)</h2>")
            .contains("report-table__cell--iconed report-icon--circle-x-row\">Blocker</td>")
            .contains("report-table__cell--iconed report-icon--radioactive-row\">Code smell</td>")
            .contains("<td class=\"report-table__cell report-table__cell--finding\">Refactor &quot;this&quot;"
                + "<span class=\"report-table__where\"><span class=\"report-table__code\">js:S1</span> · "
                + "<span class=\"report-table__code\">src/a.js:2</span></span></td>")
            .contains("<td class=\"report-table__cell report-table__cell--label\">To review</td>")
            .contains("<td class=\"report-table__cell\"><span class=\"report-table__code\">src/a.js</span></td>");
    }

    @Test
    void closesWithDevelopedByAndTheBrandLinkingToTheSite() {
        assertThat(ReportXhtml.document(full(), EMBEDDING)).endsWith("""
            <footer class="report__brand"><span class="report__brand-lead">Developed by</span>\
            <a class="report__brand-link" href="https://haembina.com">\
            <img class="report__brand-mark" src="/haembina-mark.png" alt="" />\
            <span class="report__brand-name">Haembina</span></a></footer>
            </main>
            </body>
            </html>
            """);
    }

    @Test
    void saysSoForAnEmptySection() {
        ReportView empty = new ReportView("acme", "Main branch", new ReportView.Gate("Not computed", "none"),
            List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(ReportXhtml.document(empty, EMBEDDING))
            .contains("<span class=\"report-icon report-icon--minus-none\"></span>Not computed")
            .contains("<p class=\"report__empty\">No conditions apply to this scope.</p>")
            .contains("<p class=\"report__empty\">The server holds no measures for this scope.</p>")
            .contains("<p class=\"report__empty\">No open issues.</p>")
            .contains("<p class=\"report__empty\">No security hotspots.</p>")
            .contains("<p class=\"report__empty\">No files.</p>")
            .doesNotContain("<table");
    }

    @Test
    void drawsARatingBadgeOnlyForALetter() {
        ReportView view = new ReportView("acme", "Main branch", new ReportView.Gate("Passed", "pass"), List.of(),
            List.of(new ReportView.Measure("Security rating", "", "—", "AB", true)), List.of(), List.of(), List.of());

        assertThat(ReportXhtml.document(view, EMBEDDING))
            .doesNotContain("<span class=\"report-badge report-badge--a", "report-badge--—")
            .contains("<td class=\"report-table__cell report-table__cell--label\">Security rating</td>")
            .contains("<td class=\"report-table__cell report-table__cell--number\">AB</td>");
    }

    @Test
    void holdsOnlyTheChosenSectionsWithEachLineAndBlock() {
        ReportView view = new ReportView("acme", "Main branch", new ReportView.Gate("Failed", "fail"), List.of(),
            List.of(), List.of(), List.of(), List.of(), EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS),
            List.of(new ReportView.CoverageRow("src/a.js", "2–4, 9", "6 (1 of 2 conditions)")),
            List.of(new ReportView.DuplicationRow("src/a.js", "10–24", "src/b.js 2–16")), false);

        String html = ReportXhtml.document(view, EMBEDDING);

        assertThat(html).doesNotContain("Quality gate", "Measures</h2>", "Issues (", "Files (")
            .contains("<div class=\"report__meta\"><p class=\"report__scope\">Main branch</p></div>")
            .contains("<span class=\"report-icon report-icon--umbrella-section\"></span>Uncovered lines (1)")
            .contains("<td class=\"report-table__cell\">2–4, 9</td><td class=\"report-table__cell\">"
                + "6 (1 of 2 conditions)</td>")
            .contains("<span class=\"report-icon report-icon--copy-section\"></span>Duplicated blocks (1)")
            .contains("<td class=\"report-table__cell\">10–24</td><td class=\"report-table__cell\">src/b.js 2–16</td>");
    }

    @Test
    void leavesOutTheLineSectionsWhenNotChosen() {
        ReportView view = new ReportView("acme", "Main branch", new ReportView.Gate("Passed", "pass"), List.of(),
            List.of(), List.of(), List.of(), List.of(), EnumSet.of(ReportSection.GATE), List.of(), List.of(), false);

        assertThat(ReportXhtml.document(view, EMBEDDING)).contains("Quality gate conditions")
            .doesNotContain("Uncovered lines (", "Duplicated blocks (");
    }

    @Test
    void saysWhyTheLinesAreMissingOrThatNoneAre() {
        ReportView hidden = new ReportView("acme", "Main branch", new ReportView.Gate("Passed", "pass"), List.of(),
            List.of(), List.of(), List.of(), List.of(), EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS),
            List.of(), List.of(), true);
        ReportView clean = new ReportView("acme", "Main branch", new ReportView.Gate("Passed", "pass"), List.of(),
            List.of(), List.of(), List.of(), List.of(), EnumSet.of(ReportSection.COVERAGE, ReportSection.DUPLICATIONS),
            List.of(), List.of(), false);

        assertThat(ReportXhtml.document(hidden, EMBEDDING).split(ReportXhtml.SOURCE_HIDDEN, -1)).hasSize(3);
        assertThat(ReportXhtml.document(clean, EMBEDDING))
            .contains("<p class=\"report__empty\">Every line is covered.</p>")
            .contains("<p class=\"report__empty\">No duplicated blocks.</p>");
    }

    @Test
    void linksTheRepositoryTheScopeAndSonarQube() {
        ReportView view = new ReportView("acme", "Pull request 7 – Add reports", new ReportView.Gate("Passed", "pass"),
            List.of(), List.of(), List.of(), List.of(), List.of(), EnumSet.of(ReportSection.GATE), List.of(), List.of(),
            false, new Report.Links("Acme/app", "https://github.com/Acme/app", "https://github.com/Acme/app/pull/7",
                "https://sonar.test/dashboard?id=acme&pullRequest=7", "Add reports"));

        assertThat(ReportXhtml.document(view, EMBEDDING)).contains("<p class=\"report__scope\">"
            + "<a class=\"report__link\" href=\"https://github.com/Acme/app\">Acme/app</a> · "
            + "<a class=\"report__link\" href=\"https://github.com/Acme/app/pull/7\">Pull request 7 – Add reports</a> · "
            + "<a class=\"report__link report__sonar\" href=\"https://sonar.test/dashboard?id=acme&amp;pullRequest=7\">"
            + "Open in SonarQube</a></p>");
    }

    @Test
    void namesTheScopeUnlinkedWithoutARepository() {
        ReportView view = new ReportView("acme", "Main branch", new ReportView.Gate("Passed", "pass"), List.of(),
            List.of(), List.of(), List.of(), List.of());

        assertThat(ReportXhtml.document(view, EMBEDDING)).contains("<p class=\"report__scope\">Main branch</p>");
    }

    @Test
    void escapesEveryMarkupCharacter() {
        assertThat(ReportXhtml.escape("<a href=\"x\" title='y'>&</a>"))
            .isEqualTo("&lt;a href=&quot;x&quot; title=&#39;y&#39;&gt;&amp;&lt;/a&gt;");
    }
}
