package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitProviderTest {

    @Test
    void knowsEachProviderByItsBindingAlm() {
        assertThat(GitProvider.ofAlm("github")).contains(GitProvider.GITHUB);
        assertThat(GitProvider.ofAlm("gitlab")).contains(GitProvider.GITLAB);
        assertThat(GitProvider.ofAlm("bitbucketcloud")).contains(GitProvider.BITBUCKET_CLOUD);
        assertThat(GitProvider.ofAlm("bitbucket")).contains(GitProvider.BITBUCKET_SERVER);
        assertThat(GitProvider.ofAlm("azure")).contains(GitProvider.AZURE_DEVOPS);
        assertThat(GitProvider.ofAlm(null)).isEmpty();
    }

    @Test
    void knowsEachProviderByItsHost() {
        assertThat(GitProvider.ofUrl("https://github.com/acme/app")).contains(GitProvider.GITHUB);
        assertThat(GitProvider.ofUrl("https://gitlab.example.com/acme/app")).contains(GitProvider.GITLAB);
        assertThat(GitProvider.ofUrl("https://bitbucket.org/acme/app")).contains(GitProvider.BITBUCKET_CLOUD);
        assertThat(GitProvider.ofUrl("https://bitbucket.acme.io/projects/A/repos/app"))
            .contains(GitProvider.BITBUCKET_SERVER);
        assertThat(GitProvider.ofUrl("https://dev.azure.com/acme/web/_git/app")).contains(GitProvider.AZURE_DEVOPS);
        assertThat(GitProvider.ofUrl("https://acme.visualstudio.com/web/_git/app")).contains(GitProvider.AZURE_DEVOPS);
        assertThat(GitProvider.ofUrl("https://gitea.acme.io/acme/app")).contains(GitProvider.GITEA);
        assertThat(GitProvider.ofUrl("https://codeberg.org/acme/app")).contains(GitProvider.GITEA);
        assertThat(GitProvider.ofUrl("https://git.forgejo.acme.io/acme/app")).contains(GitProvider.GITEA);
        assertThat(GitProvider.ofUrl("https://git.acme.io/acme/app")).isEmpty();
        assertThat(GitProvider.ofUrl("app")).isEmpty();
    }

    @Test
    void shapesBranchAndPullRequestLinksTheProvidersWay() {
        assertThat(GitProvider.GITHUB.branchPattern()).isEqualTo("{repository}/tree/{branch}");
        assertThat(GitProvider.GITLAB.pullRequestPattern()).isEqualTo("{repository}/-/merge_requests/{pullRequest}");
        assertThat(GitProvider.GITEA.pullRequestPattern()).isEqualTo("{repository}/pulls/{pullRequest}");
    }
}
