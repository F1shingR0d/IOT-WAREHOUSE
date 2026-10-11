package util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Consistent formatting of numbers, money and dates everywhere in the program.
 * All dates use the yyyy-MM-dd format.
 */
public final class Formats {

    /** Philippine peso sign. */
    public static final String PESO = "₱";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private Formats() {
    }

    /** 1234.5 becomes "1,234.50". */
    public static String amount(BigDecimal value) {
        return new DecimalFormat("#,##0.00").format(value == null ? BigDecimal.ZERO : value);
    }

    /** 1234.5 becomes "₱1,234.50". */
    public static String money(BigDecimal value) {
        return PESO + amount(value);
    }

    /** 12345 becomes "12,345". */
    public static String number(long value) {
        return new DecimalFormat("#,##0").format(value);
    }

    public static String percent(double value) {
        return new DecimalFormat("0.0").format(value) + "%";
    }

    public static String date(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    public static String dateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DATE_TIME);
    }
}
