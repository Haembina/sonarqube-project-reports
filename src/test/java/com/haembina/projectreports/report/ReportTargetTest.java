package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

class ReportTargetTest {

    @Test
    void namesTheMainBranchByTheProjectAlone() {
        ReportTarget target = new ReportTarget("acme", null, null);

        assertThat(target.params("component")).containsExactly(entry("component", "acme"));
        assertThat(target.fileStem()).isEqualTo("acme-main-report");
        assertThat(target.isMainBranch()).isTrue();
        assertThat(new ReportTarget("acme", "dev", null).isMainBranch()).isFalse();
        assertThat(new ReportTarget("acme", null, "7").isMainBranch()).isFalse();
    }

    @Test
    void namesABranch() {
        ReportTarget target = new ReportTarget("acme", "feature/x y", null);

        assertThat(target.params("projectKey"))
            .containsExactly(entry("projectKey", "acme"), entry("branch", "feature/x y"));
        assertThat(target.fileStem()).isEqualTo("acme-feature_x_y-report");
    }

    @Test
    void readsAComponentKeyAsAPathInTheProject() {
        ReportTarget target = new ReportTarget("acme", null, null);

        assertThat(target.path("acme:src/a.js")).isEqualTo("src/a.js");
        assertThat(target.path("other:src/a.js")).isEqualTo("other:src/a.js");
        assertThat(target.path(null)).isNull();
    }

    @Test
    void namesAPullRequest() {
        ReportTarget target = new ReportTarget("acme", null, "42");

        assertThat(target.params("component"))
            .containsExactly(entry("component", "acme"), entry("pullRequest", "42"));
        assertThat(target.fileStem()).isEqualTo("acme-pr-42-report");
    }
}
