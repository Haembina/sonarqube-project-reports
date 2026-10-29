package com.haembina.projectreports.ws;

import com.haembina.projectreports.l10n.Message;
import com.haembina.projectreports.report.ReportFormat;
import com.haembina.projectreports.report.ReportSection;
import java.util.Arrays;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.WebService;

public final class ProjectReportsWebService implements WebService {

    static final String CONTROLLER = "api/project_reports";
    static final String DOWNLOAD = "download";

    private final DownloadAction downloadAction;

    public ProjectReportsWebService(Server server) {
        this(new DownloadAction(server));
    }

    ProjectReportsWebService(DownloadAction download) {
        this.downloadAction = download;
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController(CONTROLLER)
            .setDescription(Message.API_CONTROLLER.text())
            .setSince("0.1");
        NewAction action = controller.createAction(DOWNLOAD)
            .setDescription(Message.API_DOWNLOAD.text())
            .setSince("0.1")
            .setHandler(downloadAction);
        action.createParam(DownloadAction.PROJECT)
            .setDescription(Message.API_PROJECT.text())
            .setRequired(true)
            .setExampleValue("my_project");
        action.createParam(DownloadAction.BRANCH)
            .setDescription(Message.API_BRANCH.text())
            .setExampleValue("feature/my-branch");
        action.createParam(DownloadAction.PULL_REQUEST)
            .setDescription(Message.API_PULL_REQUEST.text())
            .setExampleValue("42");
        action.createParam(DownloadAction.FORMAT)
            .setDescription(Message.API_FORMAT.text())
            .setPossibleValues(Arrays.stream(ReportFormat.values()).map(ReportFormat::key).toList())
            .setDefaultValue(ReportFormat.JSON.key());
        action.createParam(DownloadAction.SECTIONS)
            .setDescription(Message.API_SECTIONS.text())
            .setPossibleValues(Arrays.stream(ReportSection.values()).map(ReportSection::key).toList())
            .setExampleValue("gate,measures,coverage");
        controller.done();
    }
}
