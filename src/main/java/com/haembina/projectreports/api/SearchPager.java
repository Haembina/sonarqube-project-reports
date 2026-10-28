package com.haembina.projectreports.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SearchPager {

    public static final int RESULT_CAP = 10_000;
    static final int PAGE_SIZE = 500;

    private final LocalApi api;

    public SearchPager(LocalApi api) {
        this.api = api;
    }

    public JsonObject first(String path, Map<String, String> params, int pageSize) {
        return api.get(path, page(params, 1, pageSize));
    }

    public static int total(JsonObject answer) {
        return answer.getAsJsonObject("paging").get("total").getAsInt();
    }

    public List<JsonObject> all(String path, Map<String, String> params, String member) {
        List<JsonObject> found = new ArrayList<>();
        int index = 1;
        int total;
        do {
            JsonObject answer = api.get(path, page(params, index, PAGE_SIZE));
            total = Math.min(total(answer), RESULT_CAP);
            int before = found.size();
            for (JsonElement element : JsonFields.array(answer, member)) {
                found.add(element.getAsJsonObject());
            }
            if (found.size() == before) {
                break;
            }
            index++;
        } while (found.size() < total);
        return found;
    }

    private static Map<String, String> page(Map<String, String> params, int index, int size) {
        Map<String, String> paged = new LinkedHashMap<>(params);
        paged.put("p", Integer.toString(index));
        paged.put("ps", Integer.toString(size));
        return paged;
    }
}
