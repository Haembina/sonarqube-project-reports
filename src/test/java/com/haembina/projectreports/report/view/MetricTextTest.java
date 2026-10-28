package com.haembina.projectreports.report.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MetricTextTest {

    @Test
    void namesAConditionMetricOrKeepsAnUnknownKey() {
        assertThat(MetricText.label("new_coverage")).isEqualTo("Coverage on new code");
        assertThat(MetricText.label("custom_metric")).isEqualTo("custom_metric");
    }

    @Test
    void writesARatingAsALetterWithinAToE() {
        assertThat(MetricText.value("sqale_rating", "1.0")).isEqualTo("A");
        assertThat(MetricText.value("new_security_rating", "3")).isEqualTo("C");
        assertThat(MetricText.value("reliability_rating", "0")).isEqualTo("A");
        assertThat(MetricText.value("reliability_rating", "9")).isEqualTo("E");
    }

    @Test
    void writesAPercentWithoutTrailingZeros() {
        assertThat(MetricText.value("coverage", "50.0")).isEqualTo("50 %");
        assertThat(MetricText.value("duplicated_lines_density", "66.7")).isEqualTo("66.7 %");
        assertThat(MetricText.value("security_hotspots_reviewed", "100")).isEqualTo("100 %");
    }

    @Test
    void groupsACountsThousands() {
        assertThat(MetricText.value("ncloc", "1234567")).isEqualTo("1,234,567");
    }

    @Test
    void writesADashForNoValueAndKeepsAValueThatIsNoNumber() {
        assertThat(MetricText.value("ncloc", null)).isEqualTo("—");
        assertThat(MetricText.value("ncloc", " ")).isEqualTo("—");
        assertThat(MetricText.value("alert_status", "ERROR")).isEqualTo("ERROR");
    }

    @Test
    void readsAComparatorAsTheBoundItSets() {
        assertThat(MetricText.comparator("LT")).isEqualTo("at least");
        assertThat(MetricText.comparator("GT")).isEqualTo("at most");
        assertThat(MetricText.comparator("EQ")).isEqualTo("EQ");
    }

    @Test
    void turnsAConstantIntoWords() {
        assertThat(MetricText.words("CODE_SMELL")).isEqualTo("Code smell");
        assertThat(MetricText.words(null)).isEqualTo("—");
        assertThat(MetricText.words("")).isEqualTo("—");
    }
}
