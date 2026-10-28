package com.haembina.projectreports.report.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

public final class ReportAssets {

    public static final String MARK = "haembina-mark.png";
    public static final String ICON = "plugin-icon.png";
    public static final String TOKENS = "report.tokens.css";
    public static final String STYLES = "report.styles.css";

    private static final String WOFF2 = "font/woff2";

    public record Face(String family, int weight, String webWeights, String webFile, String webType, String printFile) {
    }

    public static final Face BODY = new Face("Nunito Sans", 400, "200 1000", "fonts/nunito-sans-latin.woff2", WOFF2,
        "fonts/nunito-sans-400.ttf");
    public static final Face BODY_BOLD = new Face("Nunito Sans", 700, "200 1000", "fonts/nunito-sans-latin.woff2", WOFF2,
        "fonts/nunito-sans-700.ttf");
    public static final Face HEADING = new Face("Gabarito", 700, "400 900", "fonts/gabarito-latin.woff2", WOFF2,
        "fonts/gabarito-700.ttf");
    public static final Face WORDMARK = new Face("Android Insomnia", 400, "400", "fonts/android-insomnia.ttf", "font/ttf",
        "fonts/android-insomnia.ttf");

    private static final String ROOT = "/report/";

    private ReportAssets() {
    }

    public static String iconFile(String icon) {
        return "icons/" + icon + ".png";
    }

    public static byte[] bytes(String name) {
        return read(ReportAssets.class.getResourceAsStream(ROOT + name), name);
    }

    static byte[] read(@Nullable InputStream resource, String name) {
        if (resource == null) {
            throw new IllegalStateException("The plugin jar holds no " + ROOT + name);
        }
        try (InputStream input = resource) {
            return input.readAllBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static String text(String name) {
        return new String(bytes(name), StandardCharsets.UTF_8);
    }
}
