package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates document reference numbers such as RCV-20260928-001:
 * a prefix, the document date and a running number for that day.
 */
final class ReferenceNumber {

    private ReferenceNumber() {
    }

    /**
     * Returns the next free reference number. Runs inside the caller's
     * transaction; the UNIQUE constraint on reference_no guards against duplicates.
     *
     * @param prefix "RCV", "REL" or "TRF"
     * @param table  RECEIVINGS, RELEASES or STOCK_TRANSFERS (a constant of this
     *               program, never user input)
     */
    static String next(Connection conn, String prefix, String table, LocalDate date) throws SQLException {
        String stem = prefix + "-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String sql = "SELECT MAX(CAST(SUBSTR(reference_no, " + (stem.length() + 1) + ") AS INTEGER)) "
                + "FROM " + table + " WHERE reference_no LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stem + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int last = rs.next() ? rs.getInt(1) : 0;   // NULL (first document of the day) reads as 0
                return stem + String.format("%03d", last + 1);
            }
        }
    }
}
