package com.haembina.projectreports.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;

class DownloadActionTest {

    private final Request request = mock(Request.class);
    private final Response response = mock(Response.class);
    private final Response.Stream stream = mock(Response.Stream.class);
    private final LocalConnector connector = mock(LocalConnector.class);
    private final ByteArrayOutputStream written = new ByteArrayOutputStream();
    private final Server server = mock(Server.class);
    private final DownloadAction action = new DownloadAction(server);

    @BeforeEach
    void setUp() {
        when(request.mandatoryParam("project")).thenReturn("acme");
        when(request.mandatoryParam("format")).thenReturn("json");
        when(request.localConnector()).thenReturn(connector);
        when(server.getPublicRootUrl()).thenReturn("https://sonar.test");
        when(response.stream()).thenReturn(stream);
        when(stream.output()).thenReturn(written);
    }

    private static LocalConnector.LocalResponse answer(int status, String body) {
        LocalConnector.LocalResponse answer = mock(LocalConnector.LocalResponse.class);
        when(answer.getStatus()).thenReturn(status);
        when(answer.getBytes()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        return answer;
    }

    @Test
    void writesTheReportAsAnAttachmentNamedForItsTarget() {
        when(request.hasParam("branch")).thenReturn(true);
        when(request.param("branch")).thenReturn("feature/x");
        LocalConnector.LocalResponse gate = answer(200, "{\"projectStatus\":{\"status\":\"OK\"}}");
        LocalConnector.LocalResponse measures = answer(200, "{\"component\":{}}");
        LocalConnector.LocalResponse empty = answer(200, "{\"paging\":{\"total\":0}}");
        when(connector.call(any())).thenReturn(gate, measures, empty);

        action.handle(request, response);

        verify(response).setHeader("Content-Disposition", "attachment; filename=\"acme-feature_x-report.json\"");
        verify(stream).setStatus(200);
        verify(stream).setMediaType("application/json");
        assertThat(written.toString(StandardCharsets.UTF_8)).contains("\"branch\": \"feature/x\"", "\"status\": \"OK\"");
    }

    @Test
    void holdsOnlyTheSectionsAsked() {
        when(request.paramAsStrings("sections")).thenReturn(List.of("gate"));
        LocalConnector.LocalResponse gate = answer(200, "{\"projectStatus\":{\"status\":\"OK\"}}");
        when(connector.call(any())).thenReturn(gate);

        action.handle(request, response);

        verify(connector, times(4)).call(any());
        assertThat(written.toString(StandardCharsets.UTF_8)).contains("\"sections\": [\n    \"GATE\"\n  ]");
    }

    @Test
    void passesOnTheStatusAndBodyOfAFailedCall() {
        LocalConnector.LocalResponse forbidden = answer(403, "{\"errors\":[]}");
        when(connector.call(any())).thenReturn(forbidden);

        action.handle(request, response);

        verify(response, never()).setHeader(anyString(), anyString());
        verify(stream).setStatus(403);
        assertThat(written.toString(StandardCharsets.UTF_8)).isEqualTo("{\"errors\":[]}");
    }

    @Test
    void namesTheMissingBranchPluginForABranchItCannotFind() {
        when(request.hasParam("pullRequest")).thenReturn(true);
        when(request.param("pullRequest")).thenReturn("7");
        LocalConnector.LocalResponse missing = answer(404, "{\"errors\":[{\"msg\":\"Pull request '7' not found\"}]}");
        LocalConnector.LocalResponse unknownUrl = answer(404, "{\"errors\":[{\"msg\":\"Unknown url\"}]}");
        when(connector.call(any())).thenReturn(missing, unknownUrl);

        action.handle(request, response);

        verify(stream).setStatus(400);
        assertThat(written.toString(StandardCharsets.UTF_8)).isEqualTo("{\"errors\":[{\"msg\":\"Reports of a branch or "
            + "pull request need the haembina-branch-analysis plugin, which this SonarQube does not have\"}]}");
    }

    @Test
    void passesOnAMissingBranchWhereThePluginIsInstalled() {
        when(request.hasParam("branch")).thenReturn(true);
        when(request.param("branch")).thenReturn("gone");
        LocalConnector.LocalResponse missing = answer(404, "{\"errors\":[{\"msg\":\"Branch 'gone' not found\"}]}");
        LocalConnector.LocalResponse pullRequests = answer(200, "{\"pullRequests\":[]}");
        when(connector.call(any())).thenReturn(missing, pullRequests);

        action.handle(request, response);

        verify(stream).setStatus(404);
        assertThat(written.toString(StandardCharsets.UTF_8)).contains("Branch 'gone' not found");
    }

    @Test
    void passesOnAMissingProjectWithoutAskingAboutBranches() {
        LocalConnector.LocalResponse missing = answer(404, "{\"errors\":[{\"msg\":\"Project not found\"}]}");
        when(connector.call(any())).thenReturn(missing);

        action.handle(request, response);

        verify(stream).setStatus(404);
        verify(connector).call(any());
    }

    @Test
    void refusesABranchAndAPullRequestTogether() {
        when(request.hasParam("branch")).thenReturn(true);
        when(request.hasParam("pullRequest")).thenReturn(true);

        assertThatThrownBy(() -> action.handle(request, response))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Set either branch or pullRequest, not both");
    }

    @Test
    void reportsAWriteThatFails() throws IOException {
        LocalConnector.LocalResponse missing = answer(404, "{}");
        when(connector.call(any())).thenReturn(missing);
        OutputStream broken = mock(OutputStream.class);
        doThrow(new IOException("closed")).when(broken).write(any(byte[].class));
        when(stream.output()).thenReturn(broken);

        assertThatThrownBy(() -> action.handle(request, response))
            .isInstanceOf(UncheckedIOException.class)
            .hasRootCauseMessage("closed");
    }
}
