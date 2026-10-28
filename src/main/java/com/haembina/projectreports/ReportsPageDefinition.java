package com.haembina.projectreports;

import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;

public final class ReportsPageDefinition implements PageDefinition {

    static final String PAGE_KEY = "haembinaprojectreports/report_page";

    @Override
    public void define(Context context) {
        context.addPage(Page.builder(PAGE_KEY)
            .setName("Reports")
            .setScope(Page.Scope.COMPONENT)
            .setComponentQualifiers(Page.Qualifier.PROJECT)
            .build());
    }
}
