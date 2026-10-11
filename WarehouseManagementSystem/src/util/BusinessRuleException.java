package util;

/**
 * Thrown when an operation would break a warehouse business rule, for example
 * releasing more stock than is available or exceeding a warehouse's capacity.
 * The message is written for the user and is shown as-is in a dialog.
 */
public class BusinessRuleException extends Exception {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
