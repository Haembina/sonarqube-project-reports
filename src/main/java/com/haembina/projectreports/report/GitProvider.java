package com.haembina.projectreports.report;

import com.haembina.projectreports.l10n.Message;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;

public enum GitProvider {
    GITHUB("github", "github", new Patterns("{repository}/tree/{branch}", "{repository}/pull/{pullRequest}",
        "{repository}/blob/{revision}/{path}#L{line}"), true),
    GITLAB("gitlab", "gitlab", new Patterns("{repository}/-/tree/{branch}",
        "{repository}/-/merge_requests/{pullRequest}", "{repository}/-/blob/{revision}/{path}#L{line}"), true),
    BITBUCKET_CLOUD("bitbucketcloud", "bitbucket.org", new Patterns("{repository}/src/{branch}",
        "{repository}/pull-requests/{pullRequest}", "{repository}/src/{revision}/{path}#lines-{line}"), false),
    BITBUCKET_SERVER("bitbucket", "bitbucket", new Patterns("{repository}/browse?at=refs%2Fheads%2F{branch}",
        "{repository}/pull-requests/{pullRequest}", "{repository}/browse/{path}?at={revision}#{line}"), false),
    AZURE_DEVOPS("azure", "dev.azure.com", new Patterns("{repository}?version=GB{branch}",
        "{repository}/pullrequest/{pullRequest}", "{repository}?path=/{path}&version=GC{revision}&line={line}"),
        false),
    GITEA("gitea", "gitea", new Patterns("{repository}/src/branch/{branch}", "{repository}/pulls/{pullRequest}",
        "{repository}/src/commit/{revision}/{path}#L{line}"), true);

    public static final String AUTO = "auto";

    private static final String CODEBERG = "codeberg.org";
    private static final String FORGEJO = "forgejo";
    private static final String VISUAL_STUDIO = "visualstudio.com";
    private static final String BITBUCKET = "bitbucket";

    private final String alm;
    private final String host;
    private final String branchPattern;
    private final String pullRequestPattern;
    private final String linePattern;
    private final boolean foldsComments;

    GitProvider(String alm, String host, Patterns patterns, boolean foldsComments) {
        this.alm = alm;
        this.host = host;
        this.branchPattern = patterns.branch();
        this.pullRequestPattern = patterns.pullRequest();
        this.linePattern = patterns.line();
        this.foldsComments = foldsComments;
    }

    public String branchPattern() {
        return branchPattern;
    }

    public String pullRequestPattern() {
        return pullRequestPattern;
    }

    public String linePattern() {
        return linePattern;
    }

    public boolean foldsComments() {
        return foldsComments;
    }

    public String flavor() {
        return alm.startsWith(BITBUCKET) ? BITBUCKET : alm;
    }

    public static List<String> flavors() {
        List<String> flavors = new ArrayList<>(List.of(AUTO));
        Arrays.stream(values()).map(GitProvider::flavor).distinct().forEach(flavors::add);
        return List.copyOf(flavors);
    }

    public static Optional<GitProvider> ofFlavor(String flavor) {
        if (AUTO.equals(flavor)) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(values()).filter(provider -> provider.flavor().equals(flavor)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(Message.API_UNKNOWN_FLAVOR.text(flavor))));
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

    private record Patterns(String branch, String pullRequest, String line) {
    }
}
