package model;

/**
 * Status values stored in the database. Keeping them in one place avoids
 * typing mistakes such as "Active" vs "ACTIVE".
 */
public final class Status {

    /** Record can be used in new transactions. */
    public static final String ACTIVE = "ACTIVE";
    /** Record is kept for history but hidden from new transactions. */
    public static final String INACTIVE = "INACTIVE";

    /** Transaction was saved and changed the stock. */
    public static final String POSTED = "POSTED";
    /** Transaction was cancelled and its stock effect was reversed. */
    public static final String VOIDED = "VOIDED";

    private Status() {
    }
}
