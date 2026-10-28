package com.haembina.projectreports.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.sonar.api.server.ws.LocalConnector;

public final class LocalApi {

    private static final int OK = 200;

    private final LocalConnector connector;

    public LocalApi(LocalConnector connector) {
        this.connector = connector;
    }

    public JsonObject get(String path, Map<String, String> params) {
        LocalConnector.LocalResponse response = connector.call(new ApiRequest(path, params));
        if (response.getStatus() != OK) {
            throw new ApiCallException(response.getStatus(), response.getBytes());
        }
        return JsonParser.parseString(new String(response.getBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
