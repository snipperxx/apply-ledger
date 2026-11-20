package io.github.snipperxx.applyledger.report;

import java.util.List;
import java.util.stream.Collectors;

/** Minimal RFC 4180 writer. Values that a spreadsheet would run as a formula get a leading apostrophe. */
final class Csv {

    private Csv() {
    }

    static String row(List<?> values) {
        return values.stream().map(Csv::cell).collect(Collectors.joining(",")) + "\r\n";
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
