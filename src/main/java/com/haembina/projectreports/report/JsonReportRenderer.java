package com.haembina.projectreports.report;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

final class JsonReportRenderer implements ReportRenderer {

    private static final Set<String> RENDERING_HINTS = Set.of("provider", "linePattern");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls()
        .addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getDeclaringClass() == Report.Links.class && RENDERING_HINTS.contains(field.getName());
            }

            @Override
            public boolean shouldSkipClass(Class<?> type) {
                return false;
            }
        }).create();

    @Override
    public byte[] render(Report report) {
        return GSON.toJson(report).getBytes(StandardCharsets.UTF_8);
    }
}
