package com.haembina.projectreports.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.server.ws.LocalConnector;

final class ApiRequest implements LocalConnector.LocalRequest {

    private final String path;
    private final Map<String, String> params;

    ApiRequest(String path, Map<String, String> params) {
        this.path = path;
        this.params = new LinkedHashMap<>(params);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getMediaType() {
        return "application/json";
    }

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public boolean hasParam(String key) {
        return params.containsKey(key);
    }

    @Override
    public String getParam(String key) {
        return params.get(key);
    }

    @Override
    public List<String> getMultiParam(String key) {
        return hasParam(key) ? List.of(params.get(key)) : List.of();
    }

    @Override
    public Optional<String> getHeader(String name) {
        return Optional.empty();
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = new LinkedHashMap<>();
        params.forEach((key, value) -> map.put(key, new String[] {value}));
        return map;
    }
}
