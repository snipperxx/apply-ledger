package io.github.snipperxx.applyledger.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void quotesValuesWithSeparatorsQuotesOrLineBreaks() {
        assertThat(Csv.cell("Karlsruhe, DE")).isEqualTo("\"Karlsruhe, DE\"");
        assertThat(Csv.cell("the \"big\" one")).isEqualTo("\"the \"\"big\"\" one\"");
        assertThat(Csv.cell("line one\nline two")).isEqualTo("\"line one\nline two\"");
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        assertThat(Csv.cell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(Csv.cell("+49 721")).isEqualTo("'+49 721");
        assertThat(Csv.cell("-")).isEqualTo("'-");
    }

    @Test
    void nullBecomesEmptyAndRowsEndWithCrLf() {
        assertThat(Csv.row(Arrays.asList(1, null, "x"))).isEqualTo("1,,x\r\n");
        assertThat(Csv.row(List.of())).isEqualTo("\r\n");
    }
}
