package com.haembina.projectreports.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.sonar.api.server.ws.LocalConnector;

public final class FakeConnector implements LocalConnector {

    public static final String FORBIDDEN = "forbidden";
    public static final String MISSING = "missing";

    private static final int OK = 200;
    private static final Map<String, Integer> FAILURES = Map.of(FORBIDDEN, 403, MISSING, 404);

    private final BiFunction<String, Map<String, String>, String> answers;
    private final List<Map<String, String>> calls = new ArrayList<>();

    public FakeConnector(BiFunction<String, Map<String, String>, String> answers) {
        this.answers = answers;
    }

    public static LocalApi api(BiFunction<String, Map<String, String>, String> answers) {
        return new LocalApi(new FakeConnector(answers));
    }

    public List<Map<String, String>> calls() {
        return calls;
    }

    @Override
    public LocalResponse call(LocalRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));
        Map<String, String> recorded = new LinkedHashMap<>(params);
        recorded.put("path", request.getPath());
        calls.add(recorded);
        String answer = answers.apply(request.getPath(), params);
        Integer failure = FAILURES.get(answer);
        LocalResponse response = mock(LocalResponse.class);
        when(response.getStatus()).thenReturn(failure == null ? OK : failure);
        when(response.getBytes()).thenReturn((failure == null ? answer : "{\"errors\":[{\"msg\":\"" + answer + "\"}]}")
            .getBytes(StandardCharsets.UTF_8));
        return response;
    }
}
