package com.haembina.projectreports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;

class ReportsPageDefinitionTest {

    @Test
    void addsTheReportsPageToEveryProject() {
        Context context = new Context();

        new ReportsPageDefinition().define(context);

        Page page = context.getPages().iterator().next();
        assertThat(page.getKey()).isEqualTo("haembinaprojectreports/report_page");
        assertThat(page.getName()).isEqualTo("Reports");
        assertThat(page.getScope()).isEqualTo(Page.Scope.COMPONENT);
        assertThat(page.getComponentQualifiers()).containsExactly(Page.Qualifier.PROJECT);
        assertThat(page.isAdmin()).isFalse();
    }
}
