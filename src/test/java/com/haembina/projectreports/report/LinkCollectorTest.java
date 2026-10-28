package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.haembina.projectreports.api.FakeConnector;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LinkCollectorTest {

    private static final String NO_LINKS = "{\"links\": []}";

    private static Report.Links collect(ReportTarget target, String binding, String links, String settings,
        String pullRequests) {
        return new LinkCollector(FakeConnector.api((path, params) -> switch (path) {
            case "api/alm_settings/get_binding" -> binding;
            case "api/project_links/search" -> links;
            case "api/settings/values" -> settings;
            default -> pullRequests;
        }), "https://sonar.test/").collect(target);
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void linksABoundGitHubRepositoryItsPullRequestAndSonarQube() {
        Report.Links links = collect(new ReportTarget("acme", null, "7"),
            "{\"alm\": \"github\", \"repository\": \"Acme/app\", \"url\": \"https://api.github.com/\"}", NO_LINKS,
            "{\"settings\": []}", "{\"pullRequests\": [{\"key\": \"6\"}, {\"key\": \"7\", \"title\": \"Add reports\"}]}");

        assertThat(links).isEqualTo(new Report.Links("Acme/app", "https://github.com/Acme/app",
            "https://github.com/Acme/app/pull/7", "https://sonar.test/dashboard?id=acme&pullRequest=7", "Add reports"));
    }

    @Test
    void prefersTheProjectsScmLinkAndThePullRequestsRecordedUrl() {
        Report.Links links = collect(new ReportTarget("acme", null, "7"), FakeConnector.FORBIDDEN, """
            {"links": [{"type": "homepage", "url": "https://acme.io"},
                       {"type": "scm", "url": "git@gitea.acme.io:acme/app.git"}]}""", FakeConnector.FORBIDDEN,
            "{\"pullRequests\": [{\"key\": \"7\", \"url\": \"https://gitea.acme.io/acme/app/pulls/7\"}]}");

        assertThat(links).isEqualTo(new Report.Links("acme/app", "https://gitea.acme.io/acme/app",
            "https://gitea.acme.io/acme/app/pulls/7", "https://sonar.test/dashboard?id=acme&pullRequest=7", null));
    }

    @Test
    void shapesABranchLinkFromTheProjectsPatternForAnUnknownProvider() {
        Report.Links links = collect(new ReportTarget("acme", "feature/new ui", null), FakeConnector.MISSING,
            "{\"links\": [{\"type\": \"scm\", \"url\": \"scm:git:https://git.acme.io/acme/app/\"}]}", """
            {"settings": [{"key": "haembina.reports.branchUrl", "value": " {repository}/branches/{branch} "},
                          {"key": "haembina.reports.pullRequestUrl", "value": " "},
                          {"key": "other"}]}""", "{}");

        assertThat(links).isEqualTo(new Report.Links("acme/app", "https://git.acme.io/acme/app",
            "https://git.acme.io/acme/app/branches/feature/new%20ui",
            "https://sonar.test/dashboard?id=acme&branch=feature/new%20ui", null));
    }

    @Test
    void linksNothingButSonarQubeWithoutARepository() {
        Report.Links main = collect(new ReportTarget("acme", null, null), FakeConnector.MISSING, NO_LINKS,
            "{\"settings\": []}", "{}");
        Report.Links branch = collect(new ReportTarget("acme", "dev", null), FakeConnector.MISSING, NO_LINKS,
            "{\"settings\": []}", "{}");
        Report.Links pullRequest = collect(new ReportTarget("acme", null, "9"), FakeConnector.MISSING, NO_LINKS,
            "{\"settings\": [{\"key\": \"haembina.reports.pullRequestUrl\", \"value\": \"https://x/{pullRequest}\"}]}",
            "{\"pullRequests\": []}");

        assertThat(main).isEqualTo(new Report.Links(null, null, null, "https://sonar.test/dashboard?id=acme", null));
        assertThat(branch.scopeUrl()).isNull();
        assertThat(pullRequest.scopeUrl()).isEqualTo("https://x/9");
    }

    @Test
    void readsAnScmLinkInEveryFormItIsWritten() {
        assertThat(LinkCollector.normalized("https://github.com/acme/app.git")).isEqualTo("https://github.com/acme/app");
        assertThat(LinkCollector.normalized("git@gitlab.com:acme/app.git")).isEqualTo("https://gitlab.com/acme/app");
        assertThat(LinkCollector.normalized(" ")).isNull();
        assertThat(LinkCollector.normalized(null)).isNull();
    }

    @Test
    void buildsTheWebAddressOfEachBindingThatHoldsOne() {
        assertThat(LinkCollector.boundUrl(json("""
            {"alm": "github", "repository": "acme/app", "url": "https://git.acme.io/api/v3"}""")))
            .isEqualTo("https://git.acme.io/acme/app");
        assertThat(LinkCollector.boundUrl(json("""
            {"alm": "github", "repository": "acme/app", "url": "https://git.acme.io"}""")))
            .isEqualTo("https://git.acme.io/acme/app");
        assertThat(LinkCollector.boundUrl(json("""
            {"alm": "bitbucket", "repository": "PRJ", "slug": "app", "url": "https://bb.acme.io/"}""")))
            .isEqualTo("https://bb.acme.io/projects/PRJ/repos/app");
        assertThat(LinkCollector.boundUrl(json("""
            {"alm": "azure", "repository": "app", "slug": "web", "url": "https://dev.azure.com/acme"}""")))
            .isEqualTo("https://dev.azure.com/acme/web/_git/app");
        assertThat(LinkCollector.boundUrl(json("{\"alm\": \"bitbucket\", \"repository\": \"PRJ\", \"url\": \"u\"}")))
            .isNull();
        assertThat(LinkCollector.boundUrl(json("{\"alm\": \"azure\", \"repository\": \"app\", \"url\": \"u\"}")))
            .isNull();
        assertThat(LinkCollector.boundUrl(json("{\"alm\": \"gitlab\", \"repository\": \"12\", \"url\": \"u\"}")))
            .isNull();
        assertThat(LinkCollector.boundUrl(json("{\"repository\": \"12\", \"url\": \"u\"}"))).isNull();
        assertThat(LinkCollector.boundUrl(json("{\"alm\": \"github\", \"url\": \"u\"}"))).isNull();
        assertThat(LinkCollector.boundUrl(json("{\"alm\": \"github\", \"repository\": \"a/b\"}"))).isNull();
    }

    @Test
    void namesTheRepositoryByItsPath() {
        assertThat(LinkCollector.repositoryName(json("{}"), "https://dev.azure.com/acme/web/_git/app"))
            .isEqualTo("acme/web/app");
        assertThat(LinkCollector.repositoryName(json("{}"), "https://git.acme.io")).isEqualTo("https://git.acme.io");
        assertThat(LinkCollector.repositoryName(json("{}"), "https://git.acme.io/")).isEqualTo("https://git.acme.io/");
        assertThat(LinkCollector.repositoryName(json("{}"), "mailto:git@acme.io")).isEqualTo("mailto:git@acme.io");
        assertThat(LinkCollector.repositoryName(json("{}"), null)).isNull();
    }

    @Test
    void leavesABranchUnlinkedWhenItsPatternNeedsAMissingRepository() {
        assertThat(LinkCollector.scopeUrl(new ReportTarget("acme", "dev", null), null, GitProvider.GITHUB, Map.of(),
            new JsonObject())).isNull();
    }
}
