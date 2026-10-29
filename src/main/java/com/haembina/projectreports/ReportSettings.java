package com.haembina.projectreports;

import com.haembina.projectreports.l10n.Message;
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
                .name(Message.SETTINGS_BRANCH_URL.text())
                .description(Message.SETTINGS_BRANCH_URL_DESCRIPTION.text())
                .category(CATEGORY)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .build(),
            PropertyDefinition.builder(LinkSettings.PULL_REQUEST_URL)
                .name(Message.SETTINGS_PULL_REQUEST_URL.text())
                .description(Message.SETTINGS_PULL_REQUEST_URL_DESCRIPTION.text())
                .category(CATEGORY)
                .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
                .build());
    }
}
