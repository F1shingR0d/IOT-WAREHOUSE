package model;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of a report: column headings, column types, the rows and a
 * one-line summary. The Reports screen shows it in a JTable.
 */
public class ReportData {

    private final String title;
    private final String[] columns;
    private final Class<?>[] columnTypes;
    private final List<Object[]> rows = new ArrayList<>();
    private String summary = "";

    public ReportData(String title, String[] columns, Class<?>[] columnTypes) {
        this.title = title;
        this.columns = columns;
        this.columnTypes = columnTypes;
    }

    public void addRow(Object... values) {
        rows.add(values);
    }

    public String getTitle() {
        return title;
    }

    public String[] getColumns() {
        return columns;
    }

    public Class<?>[] getColumnTypes() {
        return columnTypes;
    }

    public List<Object[]> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rows.size();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
