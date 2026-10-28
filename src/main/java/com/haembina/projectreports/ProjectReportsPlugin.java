package com.haembina.projectreports;

import com.haembina.projectreports.ws.ProjectReportsWebService;
import org.sonar.api.Plugin;

public final class ProjectReportsPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtensions(ProjectReportsWebService.class, ReportsPageDefinition.class);
        context.addExtensions(ReportSettings.definitions());
    }
}
