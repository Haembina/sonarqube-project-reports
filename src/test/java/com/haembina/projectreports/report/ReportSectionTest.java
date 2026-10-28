package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReportSectionTest {

    @Test
    void readsTheSectionsByTheirKeys() {
        assertThat(ReportSection.fromKeys(List.of("coverage", "gate")))
            .containsExactly(ReportSection.GATE, ReportSection.COVERAGE);
        assertThat(ReportSection.DUPLICATIONS.key()).isEqualTo("duplications");
    }

    @Test
    void holdsEverySectionWhenNoneIsNamed() {
        assertThat(ReportSection.fromKeys(List.of())).isEqualTo(ReportSection.all()).hasSize(7);
    }

    @Test
    void refusesAnUnknownSection() {
        List<String> keys = List.of("source");

        assertThatThrownBy(() -> ReportSection.fromKeys(keys))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown report section: source");
    }
}
