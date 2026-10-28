package com.haembina.projectreports.report.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReportStylesTest {

    @Test
    void writesEachTokensValueInPlaceAndDropsComments() {
        String styles = ReportStyles.resolve(":root {\n  --ink: #123456;\n  --mark:  #ff0055 ;\n}\n",
            "a {\n  /* note */\n  color: var(--ink);\n  border-color: var(--mark);\n}\n");

        assertThat(styles).isEqualTo("a {\n  color: #123456;\n  border-color: #ff0055;\n}\n");
    }

    @Test
    void refusesATokenNoTokensFileDeclares() {
        assertThatThrownBy(() -> ReportStyles.resolve(":root { --ink: #000; }", "a { color: var(--paper); }"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The report styles use --paper, which no token declares");
    }

    @Test
    void resolvesTheShippedStylesWithNoVariableLeft() {
        assertThat(ReportStyles.resolved()).doesNotContain("var(", "/*").contains("#ff0055");
    }

    @Test
    void keepsATableRowAndAHeadingWithWhatFollowsOnOnePage() {
        assertThat(ReportStyles.resolved())
            .contains(".report-table tr {\n  page-break-inside: avoid;\n}")
            .contains(".report-table thead {\n  page-break-after: avoid;")
            .containsPattern("\\.report__section \\{[^}]*page-break-after: avoid;");
    }
}
