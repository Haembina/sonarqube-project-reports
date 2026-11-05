package com.haembina.projectreports.report;

import com.haembina.projectreports.l10n.Message;
import java.util.Arrays;
import java.util.List;

public enum ReportLayout {
    DOCUMENT("document"),
    COMMENT("comment"),
    LINE("line");

    private final String key;

    ReportLayout(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static List<String> keys() {
        return Arrays.stream(values()).map(ReportLayout::key).toList();
    }

    public static ReportLayout fromKey(String key) {
        return Arrays.stream(values()).filter(layout -> layout.key.equals(key)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(Message.API_UNKNOWN_LAYOUT.text(key)));
    }
}
