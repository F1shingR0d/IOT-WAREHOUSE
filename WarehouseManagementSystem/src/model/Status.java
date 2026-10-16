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

    /** Customer account waiting for approval, or stock request waiting for review. */
    public static final String PENDING = "PENDING";
    /** Stock request accepted by the warehouse; the stock is not released yet. */
    public static final String APPROVED = "APPROVED";
    /** Stock request turned down by the warehouse (a reason is given). */
    public static final String REJECTED = "REJECTED";
    /** Stock request withdrawn by the customer while it was still pending. */
    public static final String CANCELLED = "CANCELLED";
    /** Stock request delivered: a release document was created for it. */
    public static final String FULFILLED = "FULFILLED";

    private Status() {
    }
}
