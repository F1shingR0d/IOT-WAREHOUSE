package util;

import java.awt.Toolkit;
import java.util.regex.Pattern;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Limits a text field to the size of its database column (so the database
 * never receives a value that is too long) and optionally to certain
 * characters, e.g. codes are automatically typed in capital letters.
 */
public class LengthFilter extends DocumentFilter {

    private final int maxLength;
    private final Pattern allowedCharacters;   // null = any character
    private final boolean upperCase;

    private LengthFilter(int maxLength, Pattern allowedCharacters, boolean upperCase) {
        this.maxLength = maxLength;
        this.allowedCharacters = allowedCharacters;
        this.upperCase = upperCase;
    }

    /** Any characters, at most maxLength of them. */
    public static void apply(JTextComponent field, int maxLength) {
        install(field, new LengthFilter(maxLength, null, false));
    }

    /** Codes such as SKU "ELE-1001": letters, digits and dashes, typed in capital letters. */
    public static void code(JTextComponent field, int maxLength) {
        install(field, new LengthFilter(maxLength, Pattern.compile("[A-Za-z0-9-]*"), true));
    }

    /** Phone numbers: digits, spaces, dashes, parentheses and +. */
    public static void phone(JTextComponent field) {
        install(field, new LengthFilter(20, Pattern.compile("[0-9+()\\- ]*"), false));
    }

    /** Usernames: letters, digits, dots and underscores. */
    public static void username(JTextComponent field) {
        install(field, new LengthFilter(30, Pattern.compile("[A-Za-z0-9._]*"), false));
    }

    private static void install(JTextComponent field, DocumentFilter filter) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(filter);
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, text, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text == null) {
            text = "";
        }
        if (upperCase) {
            text = text.toUpperCase();
        }
        if (allowedCharacters != null && !allowedCharacters.matcher(text).matches()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        int room = maxLength - (fb.getDocument().getLength() - length);
        if (text.length() > room) {
            Toolkit.getDefaultToolkit().beep();
            text = text.substring(0, Math.max(0, room));
        }
        super.replace(fb, offset, length, text, attrs);
    }
}
