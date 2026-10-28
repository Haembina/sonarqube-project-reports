package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HtmlReportRendererTest {

    @Test
    void writesOneFileCarryingItsFontsAndMark() {
        String html = new String(new HtmlReportRenderer().render(SampleReport.full()), StandardCharsets.UTF_8);

        assertThat(html).startsWith("<!DOCTYPE html>")
            .contains("@font-face { font-family: \"Nunito Sans\"; src: url(\"data:font/woff2;base64,")
            .contains("@font-face { font-family: \"Gabarito\"; src: url(\"data:font/woff2;base64,")
            .contains("@font-face { font-family: \"Android Insomnia\"; src: url(\"data:font/ttf;base64,")
            .contains("font-weight: 200 1000; }", "<img class=\"report__brand-mark\" src=\"data:image/png;base64,")
            .contains("<h1 class=\"report__title\">acme</h1>", "Branch feature/x")
            .doesNotContain("url(\"fonts/");
    }

    @Test
    void encodesAnAssetAsADataUri() {
        assertThat(HtmlReportRenderer.dataUri("image/png", "haembina-mark.png")).startsWith("data:image/png;base64,iVBOR");
    }
}
