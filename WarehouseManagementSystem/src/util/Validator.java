package util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * Input validation rules used by all forms before anything is sent to the
 * database (required fields, numbers, e-mail, phone numbers, dates, codes).
 */
public final class Validator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._]{4,30}$");
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9-]{1,29}$");
    private static final Pattern PHONE_CHARACTERS = Pattern.compile("^[0-9+()\\- ]+$");

    /** Strict yyyy-MM-dd (rejects dates such as 2026-02-30). */
    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    /** Largest amount that fits DECIMAL(10,2). */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 64;

    private Validator() {
    }

    /** true when the text is null, empty or only spaces. */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean exceedsLength(String text, int maxLength) {
        return text != null && text.trim().length() > maxLength;
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    /** 7 to 15 digits; spaces, dashes, parentheses and a leading + are allowed. */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone) || !PHONE_CHARACTERS.matcher(phone.trim()).matches()) {
            return false;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() >= 7 && digits.length() <= 15;
    }

    /** 4-30 letters, digits, dots or underscores. */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username.trim()).matches();
    }

    /** Codes such as SKU "ELE-1001" or warehouse code "WH-001". */
    public static boolean isValidCode(String code) {
        return code != null && CODE.matcher(code.trim()).matches();
    }

    public static boolean isValidPassword(char[] password) {
        return password != null
                && password.length >= MIN_PASSWORD_LENGTH
                && password.length <= MAX_PASSWORD_LENGTH;
    }

    /** Parses a whole number; returns null when the text is not a valid integer. */
    public static Integer parseInteger(String text) {
        if (isEmpty(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isPositiveInteger(String text) {
        Integer value = parseInteger(text);
        return value != null && value > 0;
    }

    public static boolean isNonNegativeInteger(String text) {
        Integer value = parseInteger(text);
        return value != null && value >= 0;
    }

    /** Parses a peso amount (0 - 99,999,999.99, max. 2 decimals); null when invalid. */
    public static BigDecimal parseAmount(String text) {
        if (isEmpty(text)) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(",", ""));
            if (value.signum() < 0 || value.scale() > 2 || value.compareTo(MAX_AMOUNT) > 0) {
                return null;
            }
            return value.setScale(2);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isValidAmount(String text) {
        return parseAmount(text) != null;
    }

    /** Parses a yyyy-MM-dd date; returns null when the text is not a real date. */
    public static LocalDate parseDate(String text) {
        if (isEmpty(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), STRICT_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static boolean isValidDate(String text) {
        return parseDate(text) != null;
    }

    public static boolean isFutureDate(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }

    /** Trims the text and turns an empty string into null (for optional columns). */
    public static String clean(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
