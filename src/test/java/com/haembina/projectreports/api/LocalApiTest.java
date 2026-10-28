package com.haembina.projectreports.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.LocalConnector;

class LocalApiTest {

    private final LocalConnector connector = mock(LocalConnector.class);
    private final LocalConnector.LocalResponse response = mock(LocalConnector.LocalResponse.class);

    private void answer(int status, String body) {
        when(response.getStatus()).thenReturn(status);
        when(response.getBytes()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(connector.call(any())).thenReturn(response);
    }

    @Test
    void callsTheWebServiceInProcessAndParsesItsAnswer() {
        answer(200, "{\"projectStatus\":{\"status\":\"ERROR\"}}");

        String status = new LocalApi(connector).get("api/qualitygates/project_status", Map.of("projectKey", "acme"))
            .getAsJsonObject("projectStatus").get("status").getAsString();

        ArgumentCaptor<LocalConnector.LocalRequest> sent = ArgumentCaptor.forClass(LocalConnector.LocalRequest.class);
        verify(connector).call(sent.capture());
        assertThat(status).isEqualTo("ERROR");
        assertThat(sent.getValue().getPath()).isEqualTo("api/qualitygates/project_status");
        assertThat(sent.getValue().getParam("projectKey")).isEqualTo("acme");
    }

    @Test
    void throwsTheStatusAndBodyOfAnAnswerOtherThanOk() {
        answer(404, "{\"errors\":[{\"msg\":\"missing\"}]}");

        ApiCallException failure = catchThrowableOfType(ApiCallException.class,
            () -> new LocalApi(connector).get("api/measures/component", Map.of()));

        assertThat(failure.status()).isEqualTo(404);
        assertThat(new String(failure.body(), StandardCharsets.UTF_8)).isEqualTo("{\"errors\":[{\"msg\":\"missing\"}]}");
        assertThat(failure).hasMessage("A SonarQube web service answered 404");
    }
}
