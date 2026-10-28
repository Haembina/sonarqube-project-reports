package com.haembina.projectreports.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.WebService;

class ProjectReportsWebServiceTest {

    private static WebService.Action download(WebService service) {
        WebService.Context context = new WebService.Context();
        service.define(context);
        return context.controller("api/project_reports").action("download");
    }

    @Test
    void definesTheDownloadActionWithItsParameters() {
        WebService.Action action = download(new ProjectReportsWebService(mock(Server.class)));

        assertThat(action.isPost()).isFalse();
        assertThat(action.handler()).isInstanceOf(DownloadAction.class);
        assertThat(action.param("project").isRequired()).isTrue();
        assertThat(action.param("branch").isRequired()).isFalse();
        assertThat(action.param("pullRequest").isRequired()).isFalse();
        assertThat(action.param("format").defaultValue()).isEqualTo("json");
        assertThat(action.param("format").possibleValues()).containsExactly("json", "markdown", "html", "csv", "sarif", "pdf", "epub");
    }

    @Test
    void usesTheHandlerItIsGiven() {
        DownloadAction handler = new DownloadAction(mock(Server.class));

        assertThat(download(new ProjectReportsWebService(handler)).handler()).isSameAs(handler);
    }
}
