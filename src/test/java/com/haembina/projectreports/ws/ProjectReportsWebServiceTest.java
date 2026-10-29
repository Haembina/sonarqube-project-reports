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
    void describesTheControllerAndEachParameter() {
        WebService.Context context = new WebService.Context();
        new ProjectReportsWebService(mock(Server.class)).define(context);
        WebService.Controller controller = context.controller("api/project_reports");
        WebService.Action action = controller.action("download");

        assertThat(controller.description()).isEqualTo("Reports of a project's quality gate, measures and issues, "
            + "for its main branch, a branch or a pull request.");
        assertThat(action.description()).isEqualTo("Download a report of a project's main branch, a branch or a "
            + "pull request. Requires Browse permission on the project.");
        assertThat(action.param("project").description()).isEqualTo("Project key");
        assertThat(action.param("branch").description())
            .isEqualTo("Branch name; the main branch when neither this nor pullRequest is set");
        assertThat(action.param("pullRequest").description()).isEqualTo("Pull request id");
        assertThat(action.param("format").description()).isEqualTo("Report format");
        assertThat(action.param("sections").description()).isEqualTo("Comma-separated sections the report holds; "
            + "every section when unset. coverage lists each uncovered and partly covered line, and duplications each "
            + "duplicated block, and both need See Source Code permission");
    }

    @Test
    void usesTheHandlerItIsGiven() {
        DownloadAction handler = new DownloadAction(mock(Server.class));

        assertThat(download(new ProjectReportsWebService(handler)).handler()).isSameAs(handler);
    }
}
