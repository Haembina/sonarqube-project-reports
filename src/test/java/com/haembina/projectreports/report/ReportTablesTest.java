package com.haembina.projectreports.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ReportTablesTest {

    private static Map<String, ReportTables.Table> tables(Report report) {
        return ReportTables.of(report).stream().collect(Collectors.toMap(ReportTables.Table::name, Function.identity()));
    }

    @Test
    void writesEachMeasureAndEachFileWithItsMetricsInColumnOrder() {
        Map<String, ReportTables.Table> tables = tables(SampleReport.full());

        assertThat(tables.get("measures").rows()).containsExactly(List.of("coverage", "72.4"), List.of("ncloc", ""));
        assertThat(tables.get("files").rows()).containsExactly(List.of("src/app.js", "12", "50.0", "", "", "", "2"));
    }

    @Test
    void titlesTheReportByItsScope() {
        assertThat(ReportTables.title(SampleReport.empty())).isEqualTo("acme, pull request 7");
        assertThat(ReportTables.title(SampleReport.full())).isEqualTo("acme, branch feature/x");
    }
}
