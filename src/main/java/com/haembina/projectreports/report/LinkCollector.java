package com.haembina.projectreports.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haembina.projectreports.api.ApiCallException;
import com.haembina.projectreports.api.JsonFields;
import com.haembina.projectreports.api.LocalApi;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

final class LinkCollector {


    private static final String GIT_SUFFIX = ".git";
    private static final String SCM_PREFIX = "scm:git:";

    private final LocalApi api;
    private final String sonarRoot;

    LinkCollector(LocalApi api, String sonarRoot) {
        this.api = api;
        this.sonarRoot = sonarRoot.endsWith("/") ? sonarRoot.substring(0, sonarRoot.length() - 1) : sonarRoot;
    }

    Report.Links collect(ReportTarget target) {
        JsonObject binding = answer("api/alm_settings/get_binding", Map.of("project", target.project()));
        String repositoryUrl = Optional.ofNullable(scmLink(target)).orElseGet(() -> boundUrl(binding));
        GitProvider provider = GitProvider.ofAlm(JsonFields.text(binding, "alm"))
            .or(() -> repositoryUrl == null ? Optional.empty() : GitProvider.ofUrl(repositoryUrl)).orElse(null);
        Map<String, String> settings = settings(target);
        JsonObject pullRequest = pullRequest(target);
        String scopeUrl = scopeUrl(target, repositoryUrl, provider, settings, pullRequest);
        return new Report.Links(repositoryName(binding, repositoryUrl), repositoryUrl, scopeUrl, sonarUrl(target),
            JsonFields.text(pullRequest, "title"));
    }

    private JsonObject answer(String path, Map<String, String> params) {
        try {
            return api.get(path, params);
        } catch (ApiCallException unavailable) {
            return new JsonObject();
        }
    }

    @Nullable
    private String scmLink(ReportTarget target) {
        for (JsonElement element : JsonFields.array(
            answer("api/project_links/search", Map.of("projectKey", target.project())), "links")) {
            JsonObject link = element.getAsJsonObject();
            if ("scm".equals(JsonFields.text(link, "type"))) {
                return normalized(JsonFields.text(link, "url"));
            }
        }
        return null;
    }

    @Nullable
    static String normalized(@Nullable String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        String url = link.trim();
        if (url.startsWith(SCM_PREFIX)) {
            url = url.substring(SCM_PREFIX.length());
        }
        if (url.startsWith("git@")) {
            url = "https://" + url.substring("git@".length()).replaceFirst(":", "/");
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url.endsWith(GIT_SUFFIX) ? url.substring(0, url.length() - GIT_SUFFIX.length()) : url;
    }

    @Nullable
    static String boundUrl(JsonObject binding) {
        String alm = JsonFields.text(binding, "alm");
        String api = JsonFields.text(binding, "url");
        String repository = JsonFields.text(binding, "repository");
        if (api == null || repository == null) {
            return null;
        }
        String root = api.endsWith("/") ? api.substring(0, api.length() - 1) : api;
        String slug = JsonFields.text(binding, "slug");
        return switch (alm == null ? "" : alm) {
            case "github" -> webRoot(root) + "/" + repository;
            case "bitbucket" -> slug == null ? null : root + "/projects/" + repository + "/repos/" + slug;
            case "azure" -> slug == null ? null : root + "/" + slug + "/_git/" + repository;
            default -> null;
        };
    }

    private static String webRoot(String api) {
        URI uri = URI.create(api);
        if ("api.github.com".equals(uri.getHost())) {
            return "https://github.com";
        }
        return api.endsWith("/api/v3") ? api.substring(0, api.length() - "/api/v3".length()) : api;
    }

    @Nullable
    static String repositoryName(JsonObject binding, @Nullable String repositoryUrl) {
        if ("github".equals(JsonFields.text(binding, "alm"))) {
            return JsonFields.text(binding, "repository");
        }
        if (repositoryUrl == null) {
            return null;
        }
        String path = URI.create(repositoryUrl).getPath();
        return path == null || path.length() <= 1 ? repositoryUrl : path.substring(1).replace("/_git/", "/");
    }

    private Map<String, String> settings(ReportTarget target) {
        Map<String, String> settings = new HashMap<>();
        JsonObject answer = answer("api/settings/values",
            Map.of("component", target.project(), "keys", LinkSettings.BRANCH_URL + "," + LinkSettings.PULL_REQUEST_URL));
        for (JsonElement element : JsonFields.array(answer, "settings")) {
            JsonObject setting = element.getAsJsonObject();
            String value = JsonFields.text(setting, "value");
            if (value != null && !value.isBlank()) {
                settings.put(JsonFields.text(setting, "key"), value.trim());
            }
        }
        return settings;
    }

    private JsonObject pullRequest(ReportTarget target) {
        if (target.pullRequest() == null) {
            return new JsonObject();
        }
        for (JsonElement element : JsonFields.array(
            answer("api/project_pull_requests/list", Map.of("project", target.project())), "pullRequests")) {
            JsonObject pullRequest = element.getAsJsonObject();
            if (target.pullRequest().equals(JsonFields.text(pullRequest, "key"))) {
                return pullRequest;
            }
        }
        return new JsonObject();
    }

    @Nullable
    static String scopeUrl(ReportTarget target, @Nullable String repositoryUrl, @Nullable GitProvider provider,
        Map<String, String> settings, JsonObject pullRequest) {
        if (target.pullRequest() != null) {
            String recorded = JsonFields.text(pullRequest, "url");
            String pattern = settings.getOrDefault(LinkSettings.PULL_REQUEST_URL,
                provider == null ? null : provider.pullRequestPattern());
            return recorded != null ? recorded : fill(pattern, repositoryUrl, "{pullRequest}", target.pullRequest());
        }
        if (target.branch() != null) {
            String pattern = settings.getOrDefault(LinkSettings.BRANCH_URL, provider == null ? null : provider.branchPattern());
            return fill(pattern, repositoryUrl, "{branch}", target.branch());
        }
        return repositoryUrl;
    }

    @Nullable
    private static String fill(@Nullable String pattern, @Nullable String repositoryUrl, String placeholder,
        String value) {
        if (pattern == null || (repositoryUrl == null && pattern.contains("{repository}"))) {
            return null;
        }
        return pattern.replace("{repository}", repositoryUrl == null ? "" : repositoryUrl)
            .replace(placeholder, encoded(value));
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    String sonarUrl(ReportTarget target) {
        StringBuilder url = new StringBuilder(sonarRoot).append("/dashboard?id=").append(encoded(target.project()));
        if (target.pullRequest() != null) {
            url.append("&pullRequest=").append(encoded(target.pullRequest()));
        } else if (target.branch() != null) {
            url.append("&branch=").append(encoded(target.branch()));
        }
        return url.toString();
    }
}
