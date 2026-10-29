package com.haembina.projectreports.ws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.haembina.projectreports.api.ApiCallException;
import com.haembina.projectreports.api.LocalApi;
import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.Report;
import com.haembina.projectreports.report.ReportCollector;
import com.haembina.projectreports.report.ReportFormat;
import com.haembina.projectreports.report.ReportSection;
import com.haembina.projectreports.report.ReportTarget;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;

final class DownloadAction implements RequestHandler {

    private final Server server;

    DownloadAction(Server server) {
        this.server = server;
    }

    static final String PROJECT = "project";
    static final String BRANCH = "branch";
    static final String PULL_REQUEST = "pullRequest";
    static final String FORMAT = "format";
    static final String SECTIONS = "sections";

    private static final String ERROR_MEDIA_TYPE = "application/json";
    private static final int BAD_REQUEST = 400;
    private static final int NOT_FOUND = 404;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static byte[] errorBody(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("msg", message);
        JsonArray errors = new JsonArray();
        errors.add(error);
        JsonObject body = new JsonObject();
        body.add("errors", errors);
        return GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void handle(Request request, Response response) {
        if (request.hasParam(BRANCH) && request.hasParam(PULL_REQUEST)) {
            throw new IllegalArgumentException(Message.API_BRANCH_AND_PULL_REQUEST.text());
        }
        ReportTarget target = new ReportTarget(
            request.mandatoryParam(PROJECT), request.param(BRANCH), request.param(PULL_REQUEST));
        ReportFormat format = ReportFormat.fromKey(request.mandatoryParam(FORMAT));
        Set<ReportSection> sections = ReportSection.fromKeys(
            Objects.requireNonNullElse(request.paramAsStrings(SECTIONS), List.of()));
        LocalApi api = new LocalApi(request.localConnector());
        Report report;
        try {
            report = new ReportCollector(api, server.getPublicRootUrl()).collect(target, sections);
        } catch (ApiCallException failure) {
            if (failure.status() == NOT_FOUND && !target.isMainBranch() && !branchesSupported(api, target)) {
                write(response, BAD_REQUEST, ERROR_MEDIA_TYPE, errorBody(Message.API_NO_BRANCH_PLUGIN.text()), null);
            } else {
                write(response, failure.status(), ERROR_MEDIA_TYPE, failure.body(), null);
            }
            return;
        }
        byte[] body = format.renderer().render(report);
        write(response, 200, format.mediaType(), body, target.fileStem() + "." + format.extension());
    }

    private static boolean branchesSupported(LocalApi api, ReportTarget target) {
        try {
            api.get("api/project_pull_requests/list", Map.of(PROJECT, target.project()));
            return true;
        } catch (ApiCallException missing) {
            return false;
        }
    }

    private static void write(Response response, int status, String mediaType, byte[] body, String fileName) {
        if (fileName != null) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        Response.Stream stream = response.stream();
        stream.setStatus(status);
        stream.setMediaType(mediaType);
        try (OutputStream output = stream.output()) {
            output.write(body);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
