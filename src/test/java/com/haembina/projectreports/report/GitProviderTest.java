package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shapesALineLinkAtARevisionTheProvidersWay() {
        assertThat(GitProvider.GITHUB.linePattern()).isEqualTo("{repository}/blob/{revision}/{path}#L{line}");
        assertThat(GitProvider.GITLAB.linePattern()).isEqualTo("{repository}/-/blob/{revision}/{path}#L{line}");
        assertThat(GitProvider.BITBUCKET_CLOUD.linePattern())
            .isEqualTo("{repository}/src/{revision}/{path}#lines-{line}");
        assertThat(GitProvider.BITBUCKET_SERVER.linePattern())
            .isEqualTo("{repository}/browse/{path}?at={revision}#{line}");
        assertThat(GitProvider.AZURE_DEVOPS.linePattern())
            .isEqualTo("{repository}?path=/{path}&version=GC{revision}&line={line}");
        assertThat(GitProvider.GITEA.linePattern()).isEqualTo("{repository}/src/commit/{revision}/{path}#L{line}");
    }

    @Test
    void foldsACommentOnlyWhereThePlatformRendersDetails() {
        assertThat(GitProvider.values()).filteredOn(GitProvider::foldsComments)
            .containsExactly(GitProvider.GITHUB, GitProvider.GITLAB, GitProvider.GITEA);
    }

    @Test
    void namesOneFlavorPerPlatformAfterAuto() {
        assertThat(GitProvider.flavors()).containsExactly("auto", "github", "gitlab", "bitbucket", "azure", "gitea");
        assertThat(GitProvider.ofFlavor("auto")).isEmpty();
        assertThat(GitProvider.ofFlavor("bitbucket")).contains(GitProvider.BITBUCKET_CLOUD);
        assertThat(GitProvider.ofFlavor("gitea")).contains(GitProvider.GITEA);
        assertThatThrownBy(() -> GitProvider.ofFlavor("sourcehut")).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown git platform: sourcehut");
    }
}
