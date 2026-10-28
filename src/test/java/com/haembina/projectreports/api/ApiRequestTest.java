package com.haembina.projectreports.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiRequestTest {

    private final ApiRequest request = new ApiRequest("api/issues/search", Map.of("componentKeys", "acme"));

    @Test
    void isAJsonGetToItsPath() {
        assertThat(request.getPath()).isEqualTo("api/issues/search");
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getMediaType()).isEqualTo("application/json");
        assertThat(request.getHeader("Accept")).isEmpty();
    }

    @Test
    void carriesItsParameters() {
        assertThat(request.hasParam("componentKeys")).isTrue();
        assertThat(request.hasParam("branch")).isFalse();
        assertThat(request.getParam("componentKeys")).isEqualTo("acme");
        assertThat(request.getMultiParam("componentKeys")).containsExactly("acme");
        assertThat(request.getMultiParam("branch")).isEmpty();
        assertThat(request.getParameterMap()).containsOnlyKeys("componentKeys");
        assertThat(request.getParameterMap().get("componentKeys")).containsExactly("acme");
    }
}
