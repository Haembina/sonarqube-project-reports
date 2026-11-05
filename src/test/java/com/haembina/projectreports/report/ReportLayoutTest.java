package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportLayoutTest {

    @Test
    void findsEachLayoutByItsKey() {
        assertThat(ReportLayout.keys()).containsExactly("document", "comment", "line");
        assertThat(ReportLayout.fromKey("comment")).isEqualTo(ReportLayout.COMMENT);
        assertThatThrownBy(() -> ReportLayout.fromKey("slide")).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown report layout: slide");
    }

    @Test
    void shapesOnlyMarkdownByItsLayout() {
        assertThat(ReportFormat.MARKDOWN.renderer(ReportLayout.COMMENT, Optional.empty()))
            .isInstanceOf(CommentReportRenderer.class);
        assertThat(ReportFormat.MARKDOWN.renderer(ReportLayout.LINE, Optional.empty()))
            .isInstanceOf(StatusLineRenderer.class);
        assertThat(ReportFormat.MARKDOWN.renderer(ReportLayout.DOCUMENT, Optional.of(GitProvider.GITHUB)))
            .isInstanceOf(MarkdownReportRenderer.class);
        assertThat(ReportFormat.HTML.renderer(ReportLayout.COMMENT, Optional.empty()))
            .isInstanceOf(HtmlReportRenderer.class);
    }
}
