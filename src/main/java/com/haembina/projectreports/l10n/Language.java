package com.haembina.projectreports.l10n;

import java.util.Locale;

public enum Language {
    ENGLISH("en", "ltr", Locale.ENGLISH, "haembinaprojectreports.properties");

    public static final Language DEFAULT = ENGLISH;

    private final String code;
    private final String direction;
    private final Locale locale;
    private final String table;

    Language(String code, String direction, Locale locale, String table) {
        this.code = code;
        this.direction = direction;
        this.locale = locale;
        this.table = table;
    }

    public String code() {
        return code;
    }

    public String direction() {
        return direction;
    }

    public Locale locale() {
        return locale;
    }

    public String table() {
        return table;
    }
}
