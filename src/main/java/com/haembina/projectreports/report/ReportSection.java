package com.haembina.projectreports.report;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public enum ReportSection {
    GATE("gate"),
    MEASURES("measures"),
    ISSUES("issues"),
    HOTSPOTS("hotspots"),
    FILES("files"),
    COVERAGE("coverage"),
    DUPLICATIONS("duplications");

    private final String key;

    ReportSection(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Set<ReportSection> all() {
        return EnumSet.allOf(ReportSection.class);
    }

    public static Set<ReportSection> fromKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return all();
        }
        Set<ReportSection> sections = EnumSet.noneOf(ReportSection.class);
        for (String key : keys) {
            sections.add(Arrays.stream(values())
                .filter(section -> section.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown report section: " + key)));
        }
        return sections;
    }
}
