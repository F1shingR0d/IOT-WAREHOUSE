package util;

import java.awt.Toolkit;
import java.util.regex.Pattern;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Prevents invalid input in number fields: only digits (and one decimal
 * point for amounts) can be typed or pasted. Invalid keys just beep.
 */
public class NumericFilter extends DocumentFilter {

    private final Pattern pattern;

    private NumericFilter(Pattern pattern) {
        this.pattern = pattern;
    }

    /** Whole numbers only, e.g. quantities. */
    public static void integer(JTextComponent field, int maxDigits) {
        install(field, new NumericFilter(Pattern.compile("\\d{0," + maxDigits + "}")));
    }

    /** Amounts such as 1250.50 (at most the given number of decimals). */
    public static void decimal(JTextComponent field, int maxWholeDigits, int maxDecimals) {
        install(field, new NumericFilter(
                Pattern.compile("\\d{0," + maxWholeDigits + "}(\\.\\d{0," + maxDecimals + "})?")));
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
        Document doc = fb.getDocument();
        String current = doc.getText(0, doc.getLength());
        String result = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
        if (pattern.matcher(result).matches()) {
            super.replace(fb, offset, length, text, attrs);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
