package ui.components;

import java.awt.Color;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import ui.UITheme;

/**
 * Color theme for Swing's built-in Metal look and feel. Every standard
 * component (menus, dialogs, file chooser, tooltips...) takes its colors from
 * here, so the whole program follows the I.O.T Warehouse theme (light work
 * area, amber accents) without any external library.
 */
public class DarkMetalTheme extends DefaultMetalTheme {

    private static ColorUIResource c(Color color) {
        return new ColorUIResource(color);
    }

    @Override
    public String getName() {
        return "I.O.T Warehouse";
    }

    // the "primary" colors are used for selections and focus: amber
    @Override
    protected ColorUIResource getPrimary1() {
        return c(UITheme.PRIMARY_DARK);
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return c(UITheme.BRAND);
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return c(UITheme.BRAND);
    }

    // the "secondary" colors are used for borders and backgrounds: light grays
    @Override
    protected ColorUIResource getSecondary1() {
        return c(UITheme.BUTTON_BORDER);
    }

    @Override
    protected ColorUIResource getSecondary2() {
        return c(UITheme.BORDER);
    }

    @Override
    protected ColorUIResource getSecondary3() {
        return c(UITheme.BACKGROUND);
    }

    @Override
    protected ColorUIResource getWhite() {
        return c(Color.WHITE);
    }

    @Override
    protected ColorUIResource getBlack() {
        return c(Color.BLACK);
    }

    @Override
    public ColorUIResource getControl() {
        return c(UITheme.BACKGROUND);
    }

    @Override
    public ColorUIResource getControlTextColor() {
        return c(UITheme.TEXT);
    }

    @Override
    public ColorUIResource getControlInfo() {
        return c(UITheme.TEXT);
    }

    @Override
    public ColorUIResource getInactiveControlTextColor() {
        return c(UITheme.TEXT_DIM);
    }

    @Override
    public ColorUIResource getSystemTextColor() {
        return c(UITheme.TEXT);
    }

    @Override
    public ColorUIResource getInactiveSystemTextColor() {
        return c(UITheme.TEXT_DIM);
    }

    @Override
    public ColorUIResource getUserTextColor() {
        return c(UITheme.TEXT);
    }

    @Override
    public ColorUIResource getHighlightedTextColor() {
        return c(Color.BLACK);
    }

    @Override
    public ColorUIResource getTextHighlightColor() {
        return c(UITheme.BRAND);
    }

    @Override
    public ColorUIResource getWindowBackground() {
        return c(UITheme.INPUT);
    }

    @Override
    public ColorUIResource getMenuBackground() {
        return c(UITheme.CARD);
    }

    @Override
    public ColorUIResource getMenuForeground() {
        return c(UITheme.TEXT);
    }

    @Override
    public ColorUIResource getMenuSelectedBackground() {
        return c(UITheme.BRAND);
    }

    @Override
    public ColorUIResource getMenuSelectedForeground() {
        return c(Color.BLACK);
    }

    @Override
    public ColorUIResource getMenuDisabledForeground() {
        return c(UITheme.TEXT_DIM);
    }

    @Override
    public ColorUIResource getAcceleratorForeground() {
        return c(UITheme.TEXT_MUTED);
    }

    @Override
    public ColorUIResource getAcceleratorSelectedForeground() {
        return c(Color.BLACK);
    }

    @Override
    public ColorUIResource getFocusColor() {
        return c(UITheme.BRAND);
    }

    @Override
    public ColorUIResource getSeparatorForeground() {
        return c(UITheme.BORDER);
    }

    @Override
    public ColorUIResource getSeparatorBackground() {
        return c(UITheme.CARD);
    }

    @Override
    public FontUIResource getControlTextFont() {
        return new FontUIResource(UITheme.FONT);
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return new FontUIResource(UITheme.FONT);
    }

    @Override
    public FontUIResource getUserTextFont() {
        return new FontUIResource(UITheme.FONT);
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return new FontUIResource(UITheme.FONT);
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return new FontUIResource(UITheme.FONT_BOLD);
    }

    @Override
    public FontUIResource getSubTextFont() {
        return new FontUIResource(UITheme.FONT_SMALL);
    }
}
