package com.haembina.projectreports.report;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;

public enum GitProvider {
    GITHUB("github", "github", "{repository}/tree/{branch}", "{repository}/pull/{pullRequest}"),
    GITLAB("gitlab", "gitlab", "{repository}/-/tree/{branch}", "{repository}/-/merge_requests/{pullRequest}"),
    BITBUCKET_CLOUD("bitbucketcloud", "bitbucket.org", "{repository}/src/{branch}",
        "{repository}/pull-requests/{pullRequest}"),
    BITBUCKET_SERVER("bitbucket", "bitbucket", "{repository}/browse?at=refs%2Fheads%2F{branch}",
        "{repository}/pull-requests/{pullRequest}"),
    AZURE_DEVOPS("azure", "dev.azure.com", "{repository}?version=GB{branch}", "{repository}/pullrequest/{pullRequest}"),
    GITEA("gitea", "gitea", "{repository}/src/branch/{branch}", "{repository}/pulls/{pullRequest}");

    private static final String CODEBERG = "codeberg.org";
    private static final String FORGEJO = "forgejo";
    private static final String VISUAL_STUDIO = "visualstudio.com";

    private final String alm;
    private final String host;
    private final String branchPattern;
    private final String pullRequestPattern;

    GitProvider(String alm, String host, String branchPattern, String pullRequestPattern) {
        this.alm = alm;
        this.host = host;
        this.branchPattern = branchPattern;
        this.pullRequestPattern = pullRequestPattern;
    }

    public String branchPattern() {
        return branchPattern;
    }

    public String pullRequestPattern() {
        return pullRequestPattern;
    }

    static Optional<GitProvider> ofAlm(@Nullable String alm) {
        return Arrays.stream(values()).filter(provider -> provider.alm.equals(alm)).findFirst();
    }

    static Optional<GitProvider> ofUrl(String repositoryUrl) {
        String found = URI.create(repositoryUrl).getHost();
        if (found == null) {
            return Optional.empty();
        }
        String name = found.toLowerCase(Locale.ROOT);
        if (name.equals(CODEBERG) || name.contains(FORGEJO)) {
            return Optional.of(GITEA);
        }
        if (name.endsWith(VISUAL_STUDIO)) {
            return Optional.of(AZURE_DEVOPS);
        }
        return Arrays.stream(values()).filter(provider -> name.contains(provider.host)).findFirst();
    }
}
