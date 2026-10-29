package com.haembina.projectreports.ws;

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
            .setDescription("Reports of a project's quality gate, measures and issues, for its main branch, a branch or a pull request.")
            .setSince("0.1");
        NewAction action = controller.createAction(DOWNLOAD)
            .setDescription("Download a report of a project's main branch, a branch or a pull request. Requires Browse permission on the project.")
            .setSince("0.1")
            .setHandler(downloadAction);
        action.createParam(DownloadAction.PROJECT)
            .setDescription("Project key")
            .setRequired(true)
            .setExampleValue("my_project");
        action.createParam(DownloadAction.BRANCH)
            .setDescription("Branch name; the main branch when neither this nor pullRequest is set")
            .setExampleValue("feature/my-branch");
        action.createParam(DownloadAction.PULL_REQUEST)
            .setDescription("Pull request id")
            .setExampleValue("42");
        action.createParam(DownloadAction.FORMAT)
            .setDescription("Comma-separated report formats; more than one answers a zip holding a report in each")
            .setPossibleValues(Arrays.stream(ReportFormat.values()).map(ReportFormat::key).toList())
            .setDefaultValue(ReportFormat.JSON.key());
        action.createParam(DownloadAction.SECTIONS)
            .setDescription("Comma-separated sections the report holds; every section when unset. coverage lists each uncovered "
                + "and partly covered line, and duplications each duplicated block, and both need See Source Code permission")
            .setPossibleValues(Arrays.stream(ReportSection.values()).map(ReportSection::key).toList())
            .setExampleValue("gate,measures,coverage");
        controller.done();
    }
}
