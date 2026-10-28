package com.haembina.projectreports.report.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.haembina.projectreports.report.Report;
import java.util.List;
import org.junit.jupiter.api.Test;

class LineRangesTest {

    @Test
    void joinsRunsOfLinesIntoRanges() {
        assertThat(LineRanges.text(List.of(2, 3, 4, 9, 11, 12))).isEqualTo("2–4, 9, 11–12");
        assertThat(LineRanges.text(List.of(7))).isEqualTo("7");
        assertThat(LineRanges.text(List.of())).isEqualTo("—");
    }

    @Test
    void writesASpanAndAPartlyCoveredLine() {
        assertThat(LineRanges.span(3, 3)).isEqualTo("3");
        assertThat(LineRanges.span(10, 24)).isEqualTo("10–24");
        assertThat(LineRanges.partial(new Report.PartLine(6, 1, 2))).isEqualTo("6 (1 of 2 conditions)");
    }
}
