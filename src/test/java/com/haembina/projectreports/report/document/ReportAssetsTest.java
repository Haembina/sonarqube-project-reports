package com.haembina.projectreports.report.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportAssetsTest {

    @Test
    void shipsTheMarkAndEveryFace() {
        assertThat(ReportAssets.bytes(ReportAssets.MARK)).startsWith(0x89, 'P', 'N', 'G');
        for (ReportAssets.Face face : List.of(ReportAssets.BODY, ReportAssets.BODY_BOLD, ReportAssets.HEADING,
            ReportAssets.WORDMARK)) {
            assertThat(ReportAssets.bytes(face.webFile())).isNotEmpty();
            assertThat(ReportAssets.bytes(face.printFile())).startsWith(0, 1, 0, 0);
        }
    }

    @Test
    void readsTheStylesAsText() {
        assertThat(ReportAssets.text(ReportAssets.STYLES)).contains(".report__brand-name");
    }

    @Test
    void refusesAnAssetTheJarDoesNotHold() {
        assertThatThrownBy(() -> ReportAssets.bytes("missing.png"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The plugin jar holds no /report/missing.png");
    }

    @Test
    void passesOnAReadThatFails() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("jar closed");
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                throw new IOException("jar closed");
            }
        };

        assertThatThrownBy(() -> ReportAssets.read(broken, "x"))
            .isInstanceOf(UncheckedIOException.class)
            .hasRootCauseMessage("jar closed");
    }
}
