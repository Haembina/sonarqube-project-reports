package com.haembina.projectreports.report.document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReportStyles {

    private static final Pattern TOKEN = Pattern.compile("(--[a-z0-9-]+)\\s*+:\\s*+([^;]++);");
    private static final Pattern USE = Pattern.compile("var\\((--[a-z0-9-]+)\\)");
    private static final Pattern COMMENT = Pattern.compile("[ \\t]*/\\*.*?\\*/\\r?\\n?", Pattern.DOTALL);

    private ReportStyles() {
    }

    public static String resolved() {
        return resolve(ReportAssets.text(ReportAssets.TOKENS), ReportAssets.text(ReportAssets.STYLES));
    }

    static String resolve(String tokens, String styles) {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher declared = TOKEN.matcher(tokens);
        while (declared.find()) {
            values.put(declared.group(1), declared.group(2).trim());
        }
        Matcher used = USE.matcher(styles);
        StringBuilder out = new StringBuilder();
        while (used.find()) {
            String value = values.get(used.group(1));
            if (value == null) {
                throw new IllegalStateException("The report styles use " + used.group(1) + ", which no token declares");
            }
            used.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        used.appendTail(out);
        return COMMENT.matcher(out).replaceAll("");
    }
}
