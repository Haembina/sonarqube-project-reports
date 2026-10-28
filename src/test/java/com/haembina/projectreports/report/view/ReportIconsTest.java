package com.haembina.projectreports.report.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.report.document.ReportAssets;
import org.junit.jupiter.api.Test;

class ReportIconsTest {

    @Test
    void marksEachStatusWithACheckOrACross() {
        assertThat(ReportIcons.forTone("pass")).isEqualTo("check-pass");
        assertThat(ReportIcons.forTone("fail")).isEqualTo("x-fail");
        assertThat(ReportIcons.forTone("none")).isEqualTo("minus-none");
    }

    @Test
    void namesAnIconForEachIssueTypeAndSeverity() {
        assertThat(ReportIcons.forType("BUG")).isEqualTo("bug-row");
        assertThat(ReportIcons.forType("VULNERABILITY")).isEqualTo("lock-open-row");
        assertThat(ReportIcons.forType("CODE_SMELL")).isEqualTo("radioactive-row");
        assertThat(ReportIcons.forType("SECURITY_HOTSPOT")).isEqualTo("flame-row");
        assertThat(ReportIcons.forType(null)).isEmpty();
        assertThat(ReportIcons.forSeverity("BLOCKER")).isEqualTo("circle-x-row");
        assertThat(ReportIcons.forSeverity("INFO")).isEqualTo("info-circle-row");
        assertThat(ReportIcons.forSeverity("UNKNOWN")).isEmpty();
        assertThat(ReportIcons.forSeverity(null)).isEmpty();
    }

    @Test
    void shipsEveryIconItNames() {
        for (String icon : ReportIcons.ALL) {
            assertThat(ReportAssets.bytes(ReportAssets.iconFile(icon))).as(icon).startsWith(0x89, 'P', 'N', 'G');
        }
    }
}
