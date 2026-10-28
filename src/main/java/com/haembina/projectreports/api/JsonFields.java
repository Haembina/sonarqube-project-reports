package com.haembina.projectreports.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import javax.annotation.Nullable;

public final class JsonFields {

    private JsonFields() {
    }

    public static Iterable<JsonElement> array(JsonObject object, String member) {
        return object.has(member) ? object.getAsJsonArray(member) : List.of();
    }

    @Nullable
    public static String text(JsonObject object, String member) {
        return object.has(member) ? object.get(member).getAsString() : null;
    }

    @Nullable
    public static Integer integer(JsonObject object, String member) {
        return object.has(member) ? object.get(member).getAsInt() : null;
    }

    @Nullable
    public static String measureValue(JsonObject measure) {
        JsonObject source = measure.has("period") ? measure.getAsJsonObject("period") : measure;
        return text(source, "value");
    }
}
