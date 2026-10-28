package com.haembina.projectreports.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;

final class JsonReportRenderer implements ReportRenderer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    @Override
    public byte[] render(Report report) {
        return GSON.toJson(report).getBytes(StandardCharsets.UTF_8);
    }
}
