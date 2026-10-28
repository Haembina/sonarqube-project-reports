package com.haembina.projectreports;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.haembina.projectreports.ws.ProjectReportsWebService;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;

class ProjectReportsPluginTest {

    @Test
    void registersTheReportWebService() {
        Plugin.Context context = mock(Plugin.Context.class);

        new ProjectReportsPlugin().define(context);

        verify(context).addExtensions(ProjectReportsWebService.class, ReportsPageDefinition.class);
    }
}
