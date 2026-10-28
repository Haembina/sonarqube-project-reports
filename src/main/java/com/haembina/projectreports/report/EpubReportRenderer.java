package com.haembina.projectreports.report;

import com.haembina.projectreports.report.document.ReportAssets;
import com.haembina.projectreports.report.document.ReportXhtml;
import com.haembina.projectreports.report.view.ReportIcons;
import com.haembina.projectreports.report.view.ReportView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class EpubReportRenderer implements ReportRenderer {

    static final String MIMETYPE = "application/epub+zip";
    private static final String FOLDER = "EPUB/";
    private static final List<ReportAssets.Face> FACES = List.of(ReportAssets.BODY, ReportAssets.HEADING,
        ReportAssets.WORDMARK);

    static final ReportXhtml.Embedding PACKAGED = new ReportXhtml.Embedding() {
        @Override
        public String fontFaces() {
            StringBuilder faces = new StringBuilder();
            for (ReportAssets.Face face : FACES) {
                faces.append("@font-face { font-family: \"").append(face.family()).append("\"; src: url(\"")
                    .append(face.webFile()).append("\"); font-weight: ").append(face.webWeights()).append("; }\n");
            }
            return faces.toString();
        }

        @Override
        public String imageSource(String asset) {
            return asset;
        }
    };

    private final Clock clock;

    EpubReportRenderer(Clock clock) {
        this.clock = clock;
    }

    @Override
    public byte[] render(Report report) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        write(ReportView.of(report), Instant.now(clock), bytes);
        return bytes.toByteArray();
    }

    static void write(ReportView view, Instant modified, OutputStream output) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("META-INF/container.xml", text("""
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles><rootfile full-path="EPUB/package.opf" media-type="application/oebps-package+xml"/></rootfiles>
            </container>
            """));
        files.put(FOLDER + "package.opf", text(opf(view, modified)));
        files.put(FOLDER + "nav.xhtml", text(nav(view)));
        files.put(FOLDER + "report.xhtml", text("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + ReportXhtml.document(view, PACKAGED)));
        for (ReportAssets.Face face : FACES) {
            files.put(FOLDER + face.webFile(), ReportAssets.bytes(face.webFile()));
        }
        for (String image : images()) {
            files.put(FOLDER + image, ReportAssets.bytes(image));
        }
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(stored(text(MIMETYPE)));
            zip.write(text(MIMETYPE));
            zip.closeEntry();
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static ZipEntry stored(byte[] content) {
        ZipEntry entry = new ZipEntry("mimetype");
        CRC32 crc = new CRC32();
        crc.update(content);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(content.length);
        entry.setCompressedSize(content.length);
        entry.setCrc(crc.getValue());
        return entry;
    }

    static String opf(ReportView view, Instant modified) {
        String title = ReportXhtml.escape(view.project() + " – " + view.scope());
        UUID id = UUID.nameUUIDFromBytes((view.project() + "\n" + view.scope()).getBytes(StandardCharsets.UTF_8));
        StringBuilder manifest = new StringBuilder();
        int index = 0;
        for (ReportAssets.Face face : FACES) {
            manifest.append("    <item id=\"font").append(index++).append("\" href=\"").append(face.webFile())
                .append("\" media-type=\"").append(face.webType()).append("\"/>\n");
        }
        index = 0;
        for (String image : images()) {
            manifest.append("    <item id=\"image").append(index++).append("\" href=\"").append(image)
                .append("\" media-type=\"image/png\"/>\n");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"id\" xml:lang=\"en\">\n"
            + "  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
            + "    <dc:identifier id=\"id\">urn:uuid:" + id + "</dc:identifier>\n"
            + "    <dc:title>" + title + "</dc:title>\n"
            + "    <dc:language>en</dc:language>\n"
            + "    <dc:publisher>" + ReportView.PLUGIN_NAME + "</dc:publisher>\n"
            + "    <meta property=\"dcterms:modified\">" + modified.truncatedTo(ChronoUnit.SECONDS) + "</meta>\n"
            + "  </metadata>\n  <manifest>\n"
            + "    <item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n"
            + "    <item id=\"report\" href=\"report.xhtml\" media-type=\"application/xhtml+xml\"/>\n"
            + manifest
            + "  </manifest>\n  <spine>\n    <itemref idref=\"report\"/>\n  </spine>\n</package>\n";
    }

    static List<String> images() {
        List<String> images = new ArrayList<>(List.of(ReportAssets.MARK, ReportAssets.ICON));
        ReportIcons.ALL.forEach(icon -> images.add(ReportAssets.iconFile(icon)));
        return images;
    }

    static String nav(ReportView view) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html>\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" lang=\"en\""
            + " xml:lang=\"en\">\n<head><title>" + ReportXhtml.escape(view.project()) + "</title></head>\n<body>\n"
            + "<nav epub:type=\"toc\"><ol><li><a href=\"report.xhtml\">" + ReportXhtml.escape(view.project())
            + "</a></li></ol></nav>\n</body>\n</html>\n";
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
