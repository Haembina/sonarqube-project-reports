package com.haembina.projectreports.report;

import java.time.Clock;
import java.util.Arrays;

public enum ReportFormat {
    JSON("json", "json", "application/json"),
    MARKDOWN("markdown", "md", "text/markdown; charset=utf-8"),
    HTML("html", "html", "text/html; charset=utf-8"),
    CSV("csv", "zip", "application/zip"),
    SARIF("sarif", "sarif", "application/sarif+json"),
    PDF("pdf", "pdf", "application/pdf"),
    EPUB("epub", "epub", EpubReportRenderer.MIMETYPE);

    private final String key;
    private final String extension;
    private final String mediaType;

    ReportFormat(String key, String extension, String mediaType) {
        this.key = key;
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public static ReportFormat fromKey(String key) {
        return Arrays.stream(values())
            .filter(format -> format.key.equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown report format: " + key));
    }

    public String key() {
        return key;
    }

    public String extension() {
        return extension;
    }

    public String mediaType() {
        return mediaType;
    }

    public ReportRenderer renderer() {
        return switch (this) {
            case JSON -> new JsonReportRenderer();
            case MARKDOWN -> new MarkdownReportRenderer();
            case HTML -> new HtmlReportRenderer();
            case CSV -> new CsvReportRenderer();
            case SARIF -> new SarifReportRenderer();
            case PDF -> new PdfReportRenderer();
            case EPUB -> new EpubReportRenderer(Clock.systemUTC());
        };
    }
}
