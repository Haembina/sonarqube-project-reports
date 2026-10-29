package com.haembina.projectreports.ws;

import com.haembina.projectreports.api.ApiCallException;
import com.haembina.projectreports.api.LocalApi;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
    private static final String ARCHIVE_MEDIA_TYPE = "application/zip";
    private static final int BAD_REQUEST = 400;
    private static final int NOT_FOUND = 404;
    private static final byte[] NO_BRANCH_PLUGIN = ("{\"errors\":[{\"msg\":\"Reports of a branch or pull request need "
        + "the haembina-branch-analysis plugin, which this SonarQube does not have\"}]}")
        .getBytes(StandardCharsets.UTF_8);

    @Override
    public void handle(Request request, Response response) {
        if (request.hasParam(BRANCH) && request.hasParam(PULL_REQUEST)) {
            throw new IllegalArgumentException("Set either branch or pullRequest, not both");
        }
        ReportTarget target = new ReportTarget(
            request.mandatoryParam(PROJECT), request.param(BRANCH), request.param(PULL_REQUEST));
        Set<ReportFormat> formats = ReportFormat.fromKeys(request.mandatoryParamAsStrings(FORMAT));
        if (formats.isEmpty()) {
            throw new IllegalArgumentException("Set at least one format");
        }
        Set<ReportSection> sections = ReportSection.fromKeys(
            Objects.requireNonNullElse(request.paramAsStrings(SECTIONS), List.of()));
        LocalApi api = new LocalApi(request.localConnector());
        Report report;
        try {
            report = new ReportCollector(api, server.getPublicRootUrl()).collect(target, sections);
        } catch (ApiCallException failure) {
            if (failure.status() == NOT_FOUND && !target.isMainBranch() && !branchesSupported(api, target)) {
                write(response, BAD_REQUEST, ERROR_MEDIA_TYPE, null, bytes(NO_BRANCH_PLUGIN));
            } else {
                write(response, failure.status(), ERROR_MEDIA_TYPE, null, bytes(failure.body()));
            }
            return;
        }
        String stem = target.fileStem();
        if (formats.size() == 1) {
            ReportFormat format = formats.iterator().next();
            write(response, 200, format.mediaType(), stem + "." + format.extension(),
                bytes(format.renderer().render(report)));
        } else {
            write(response, 200, ARCHIVE_MEDIA_TYPE, stem + "s.zip", output -> archive(report, formats, stem, output));
        }
    }

    private static Body bytes(byte[] body) {
        return output -> output.write(body);
    }

    private static void archive(Report report, Set<ReportFormat> formats, String stem, OutputStream output)
        throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (ReportFormat format : formats) {
                zip.putNextEntry(new ZipEntry(stem + "." + format.extension()));
                zip.write(format.renderer().render(report));
                zip.closeEntry();
            }
        }
    }

    private static boolean branchesSupported(LocalApi api, ReportTarget target) {
        try {
            api.get("api/project_pull_requests/list", Map.of(PROJECT, target.project()));
            return true;
        } catch (ApiCallException missing) {
            return false;
        }
    }

    private static void write(Response response, int status, String mediaType, String fileName, Body body) {
        if (fileName != null) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        Response.Stream stream = response.stream();
        stream.setStatus(status);
        stream.setMediaType(mediaType);
        try (OutputStream output = stream.output()) {
            body.writeTo(output);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    @FunctionalInterface
    private interface Body {
        void writeTo(OutputStream output) throws IOException;
    }
}
