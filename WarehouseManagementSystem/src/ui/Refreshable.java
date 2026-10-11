package ui;

/**
 * Implemented by every module screen. The main window calls refreshData()
 * each time a screen is opened, so it always shows the latest data.
 */
public interface Refreshable {

    void refreshData();
}
