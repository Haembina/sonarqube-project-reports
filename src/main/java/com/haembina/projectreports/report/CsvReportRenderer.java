package com.haembina.projectreports.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class CsvReportRenderer implements ReportRenderer {

    private static final String FORMULA_STARTS = "=+-@\t\r";

    @Override
    public byte[] render(Report report) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        write(report, bytes);
        return bytes.toByteArray();
    }

    static void write(Report report, OutputStream output) {
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (ReportTables.Table table : ReportTables.of(report)) {
                zip.putNextEntry(new ZipEntry(table.name() + ".csv"));
                StringBuilder csv = new StringBuilder();
                line(csv, table.headers());
                for (List<String> cells : table.rows()) {
                    line(csv, cells);
                }
                zip.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static void line(StringBuilder csv, List<String> cells) {
        for (int index = 0; index < cells.size(); index++) {
            csv.append(index == 0 ? "" : ",").append(field(cells.get(index)));
        }
        csv.append("\r\n");
    }

    static String field(String value) {
        String safe = !value.isEmpty() && FORMULA_STARTS.indexOf(value.charAt(0)) >= 0 && !isNumber(value)
            ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }
}
