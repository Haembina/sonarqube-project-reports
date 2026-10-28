package com.haembina.projectreports.report;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

public record ReportTarget(String project, @Nullable String branch, @Nullable String pullRequest) {

    public Map<String, String> params(String projectParam) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(projectParam, project);
        if (branch != null) {
            params.put("branch", branch);
        }
        if (pullRequest != null) {
            params.put("pullRequest", pullRequest);
        }
        return params;
    }

    public boolean isMainBranch() {
        return branch == null && pullRequest == null;
    }

    @Nullable
    public String path(@Nullable String componentKey) {
        String prefix = project + ":";
        if (componentKey != null && componentKey.startsWith(prefix)) {
            return componentKey.substring(prefix.length());
        }
        return componentKey;
    }

    public String fileStem() {
        String scope;
        if (pullRequest != null) {
            scope = "pr-" + pullRequest;
        } else if (branch != null) {
            scope = branch;
        } else {
            scope = "main";
        }
        return (project + "-" + scope + "-report").replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
