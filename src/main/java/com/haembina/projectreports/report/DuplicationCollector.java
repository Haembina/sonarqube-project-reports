package com.haembina.projectreports.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haembina.projectreports.api.JsonFields;
import com.haembina.projectreports.api.LocalApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

final class DuplicationCollector {

    static final String DUPLICATED_LINES = "duplicated_lines";

    private final LocalApi api;

    DuplicationCollector(LocalApi api) {
        this.api = api;
    }

    List<Report.Duplication> collect(ReportTarget target, List<Report.FileMeasures> files) {
        List<Report.Duplication> duplications = new ArrayList<>();
        for (Report.FileMeasures file : files) {
            if (LineCollector.positive(file.measure(DUPLICATED_LINES))) {
                duplications.addAll(blocks(target, file));
            }
        }
        return duplications;
    }

    private List<Report.Duplication> blocks(ReportTarget target, Report.FileMeasures file) {
        Map<String, String> params = target.params("key");
        params.put("key", file.key());
        JsonObject answer = api.get("api/duplications/show", params);
        JsonObject refs = answer.has("files") ? answer.getAsJsonObject("files") : new JsonObject();
        List<Report.Duplication> duplications = new ArrayList<>();
        for (JsonElement element : JsonFields.array(answer, "duplications")) {
            List<Report.Place> places = new ArrayList<>(
                StreamSupport.stream(JsonFields.array(element.getAsJsonObject(), "blocks").spliterator(), false)
                    .map(block -> place(target, refs, block.getAsJsonObject()))
                    .toList());
            int here = places.stream().map(Report.Place::path).toList().indexOf(file.path());
            if (here >= 0) {
                Report.Place own = places.remove(here);
                duplications.add(new Report.Duplication(own.path(), own.from(), own.to(), places));
            }
        }
        return duplications;
    }

    private static Report.Place place(ReportTarget target, JsonObject refs, JsonObject block) {
        int from = block.get("from").getAsInt();
        int size = block.get("size").getAsInt();
        String ref = JsonFields.text(block, "_ref");
        JsonObject referenced = ref != null && refs.has(ref) ? refs.getAsJsonObject(ref) : new JsonObject();
        String path = target.path(JsonFields.text(referenced, "key"));
        return new Report.Place(path == null ? "—" : path, from, from + size - 1);
    }
}
