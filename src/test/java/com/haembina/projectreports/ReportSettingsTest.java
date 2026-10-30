package com.haembina.projectreports;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.PropertyDefinition;

class ReportSettingsTest {

    @Test
    void namesAndDescribesEachLinkSetting() {
        List<PropertyDefinition> definitions = ReportSettings.definitions();

        assertThat(definitions).extracting(PropertyDefinition::name).containsExactly("Branch link", "Pull request link");
        assertThat(definitions).extracting(PropertyDefinition::category)
            .containsOnly("Haembina Project Reports");
        assertThat(definitions.get(0).description()).isEqualTo("Where a report's branch name links, for a git "
            + "provider the plugin does not recognize. {repository} is the repository's web address and {branch} the "
            + "branch name, as in {repository}/src/branch/{branch}.");
        assertThat(definitions.get(1).description()).isEqualTo("Where a report's pull request links, for a git "
            + "provider the plugin does not recognize. {repository} is the repository's web address and {pullRequest} "
            + "the pull request id, as in {repository}/pulls/{pullRequest}.");
    }
}
