package com.haembina.projectreports;

import com.haembina.projectreports.report.LinkSettings;
import java.util.List;
import org.sonar.api.config.PropertyDefinition;

final class ReportSettings {

    private static final String CATEGORY = "Haembina Project Reports";

    private ReportSettings() {
    }

    static List<PropertyDefinition> definitions() {
        return List.of(
            PropertyDefinition.builder(LinkSettings.BRANCH_URL)
                .name("Branch link")
                .description("Where a report's branch name links, for a git provider the plugin does not recognize. "
                    + "{repository} is the repository's web address and {branch} the branch name, as in "
                    + "{repository}/src/branch/{branch}.")
                .category(CATEGORY)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .build(),
            PropertyDefinition.builder(LinkSettings.PULL_REQUEST_URL)
                .name("Pull request link")
                .description("Where a report's pull request links, for a git provider the plugin does not recognize. "
                    + "{repository} is the repository's web address and {pullRequest} the pull request id, as in "
                    + "{repository}/pulls/{pullRequest}.")
                .category(CATEGORY)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .build());
    }
}
