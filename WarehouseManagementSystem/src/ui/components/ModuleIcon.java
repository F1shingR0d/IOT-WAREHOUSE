package ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Small line icons for the sidebar and the buttons, drawn with Java 2D shapes
 * (no image files).
 * Each icon is designed on a 16 x 16 grid and scaled to the requested size.
 */
public class ModuleIcon implements Icon {

    /** The available pictures. */
    public enum Type {
        DASHBOARD, INVENTORY, RECEIVING, RELEASING, TRANSFER, PRODUCTS, CATEGORIES,
        WAREHOUSES, SUPPLIERS, CUSTOMERS, REPORTS, USERS, LOGOUT,
        // button icons
        SAVE, EDIT, DELETE, CLEAR, SEARCH, USER, CALENDAR, PRINT, EXPORT, ADD, REMOVE, VOID, VIEW, CLOSE, KEY, LOGIN
    }

    private final Type type;
    private final int size;
    private Color color;

    public ModuleIcon(Type type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.scale(size / 16.0, size / 16.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case DASHBOARD:
                g.draw(new RoundRectangle2D.Float(1.5f, 1.5f, 5.5f, 5.5f, 1.5f, 1.5f));
                g.draw(new RoundRectangle2D.Float(9f, 1.5f, 5.5f, 5.5f, 1.5f, 1.5f));
                g.draw(new RoundRectangle2D.Float(1.5f, 9f, 5.5f, 5.5f, 1.5f, 1.5f));
                g.draw(new RoundRectangle2D.Float(9f, 9f, 5.5f, 5.5f, 1.5f, 1.5f));
                break;
            case INVENTORY:
                g.draw(new Rectangle2D.Float(1.5f, 8.5f, 6f, 6f));
                g.draw(new Rectangle2D.Float(8.5f, 8.5f, 6f, 6f));
                g.draw(new Rectangle2D.Float(5f, 1.5f, 6f, 6f));
                break;
            case RECEIVING:
                drawTray(g);
                g.draw(new Line2D.Float(8f, 1.5f, 8f, 9.5f));
                g.draw(path(5f, 6.5f, 8f, 9.5f, 11f, 6.5f));
                break;
            case RELEASING:
                drawTray(g);
                g.draw(new Line2D.Float(8f, 9.5f, 8f, 1.5f));
                g.draw(path(5f, 4.5f, 8f, 1.5f, 11f, 4.5f));
                break;
            case TRANSFER:
                g.draw(new Line2D.Float(1.5f, 5f, 14f, 5f));
                g.draw(path(11f, 2f, 14f, 5f, 11f, 8f));
                g.draw(new Line2D.Float(14.5f, 11f, 2f, 11f));
                g.draw(path(5f, 8f, 2f, 11f, 5f, 14f));
                break;
            case PRODUCTS:
                Path2D cube = path(8f, 1.5f, 14.5f, 4.75f, 14.5f, 11.25f, 8f, 14.5f, 1.5f, 11.25f, 1.5f, 4.75f);
                cube.closePath();
                g.draw(cube);
                g.draw(path(1.5f, 4.75f, 8f, 8f, 14.5f, 4.75f));
                g.draw(new Line2D.Float(8f, 8f, 8f, 14.5f));
                break;
            case CATEGORIES:
                Path2D tag = path(1.5f, 1.5f, 8.5f, 1.5f, 14.5f, 7.5f, 8f, 14f, 1.5f, 7.5f);
                tag.closePath();
                g.draw(tag);
                g.fill(new Ellipse2D.Float(4f, 4f, 2.2f, 2.2f));
                break;
            case WAREHOUSES:
                g.draw(path(1f, 6.5f, 8f, 1.5f, 15f, 6.5f));
                g.draw(path(2.5f, 5.5f, 2.5f, 14.5f, 13.5f, 14.5f, 13.5f, 5.5f));
                g.draw(new Rectangle2D.Float(5.5f, 9f, 5f, 5.5f));
                break;
            case SUPPLIERS:
                g.draw(new Rectangle2D.Float(1f, 3.5f, 9f, 7.5f));
                g.draw(path(10f, 6f, 13f, 6f, 15f, 8.5f, 15f, 11f, 10f, 11f));
                g.draw(new Ellipse2D.Float(2.5f, 11f, 3f, 3f));
                g.draw(new Ellipse2D.Float(10.5f, 11f, 3f, 3f));
                break;
            case CUSTOMERS:
                g.draw(new Ellipse2D.Float(5f, 1.5f, 6f, 6f));
                Path2D body = new Path2D.Float();
                body.moveTo(2f, 14.5f);
                body.quadTo(2f, 9.5f, 8f, 9.5f);
                body.quadTo(14f, 9.5f, 14f, 14.5f);
                g.draw(body);
                break;
            case REPORTS:
                g.draw(new Line2D.Float(1.5f, 14.5f, 14.5f, 14.5f));
                g.draw(new Rectangle2D.Float(2.5f, 8.5f, 3f, 6f));
                g.draw(new Rectangle2D.Float(6.5f, 4.5f, 3f, 10f));
                g.draw(new Rectangle2D.Float(10.5f, 1.5f, 3f, 13f));
                break;
            case USERS:
                g.draw(new Ellipse2D.Float(2f, 2f, 5f, 5f));
                Path2D first = new Path2D.Float();
                first.moveTo(0.8f, 14f);
                first.quadTo(0.8f, 9.5f, 4.5f, 9.5f);
                first.quadTo(8.2f, 9.5f, 8.2f, 14f);
                g.draw(first);
                g.draw(new Ellipse2D.Float(9.5f, 3.5f, 4.5f, 4.5f));
                Path2D second = new Path2D.Float();
                second.moveTo(9.8f, 10.2f);
                second.quadTo(15.2f, 9.6f, 15.2f, 14f);
                g.draw(second);
                break;
            case LOGOUT:
                g.draw(path(9.5f, 4f, 9.5f, 1.5f, 1.5f, 1.5f, 1.5f, 14.5f, 9.5f, 14.5f, 9.5f, 12f));
                g.draw(new Line2D.Float(5.5f, 8f, 14.5f, 8f));
                g.draw(path(12f, 5.5f, 14.5f, 8f, 12f, 10.5f));
                break;
            case SAVE:
                Path2D disk = path(1.5f, 1.5f, 11.5f, 1.5f, 14.5f, 4.5f, 14.5f, 14.5f, 1.5f, 14.5f);
                disk.closePath();
                g.draw(disk);
                g.draw(new Rectangle2D.Float(4.5f, 1.5f, 6f, 4f));
                g.draw(new Rectangle2D.Float(4f, 9f, 8f, 5.5f));
                break;
            case EDIT:
                Path2D pencil = path(10.5f, 2.5f, 13.5f, 5.5f, 5.5f, 13.5f, 2f, 14f, 2.5f, 10.5f);
                pencil.closePath();
                g.draw(pencil);
                g.draw(new Line2D.Float(9f, 4f, 12f, 7f));
                break;
            case DELETE:
                g.draw(new Line2D.Float(1.5f, 4f, 14.5f, 4f));
                g.draw(path(5.5f, 4f, 5.5f, 1.5f, 10.5f, 1.5f, 10.5f, 4f));
                g.draw(path(3f, 4f, 4f, 14.5f, 12f, 14.5f, 13f, 4f));
                g.draw(new Line2D.Float(6.5f, 7f, 6.5f, 11.5f));
                g.draw(new Line2D.Float(9.5f, 7f, 9.5f, 11.5f));
                break;
            case CLEAR:
                g.draw(new java.awt.geom.Arc2D.Float(2f, 2f, 12f, 12f, 60, 260, java.awt.geom.Arc2D.OPEN));
                g.draw(path(11f, 0.8f, 11.2f, 3.4f, 8.6f, 4.4f));
                break;
            case SEARCH:
                g.draw(new Ellipse2D.Float(1.5f, 1.5f, 9f, 9f));
                g.draw(new Line2D.Float(9.2f, 9.2f, 14.5f, 14.5f));
                break;
            case USER:
                g.fill(new Ellipse2D.Float(4.5f, 1f, 7f, 7f));
                Path2D person = new Path2D.Float();
                person.moveTo(1.5f, 15f);
                person.quadTo(1.5f, 9.5f, 8f, 9.5f);
                person.quadTo(14.5f, 9.5f, 14.5f, 15f);
                person.closePath();
                g.fill(person);
                break;
            case CALENDAR:
                g.draw(new RoundRectangle2D.Float(1.5f, 2.5f, 13f, 12f, 2f, 2f));
                g.draw(new Line2D.Float(1.5f, 6f, 14.5f, 6f));
                g.draw(new Line2D.Float(5f, 1f, 5f, 4f));
                g.draw(new Line2D.Float(11f, 1f, 11f, 4f));
                for (int row = 0; row < 2; row++) {
                    for (int col = 0; col < 3; col++) {
                        g.fill(new Rectangle2D.Float(4f + col * 3f, 8f + row * 3f, 1.6f, 1.6f));
                    }
                }
                break;
            case PRINT:
                g.draw(path(4f, 5f, 4f, 1.5f, 12f, 1.5f, 12f, 5f));
                g.draw(new RoundRectangle2D.Float(1.5f, 5f, 13f, 6.5f, 1.5f, 1.5f));
                g.draw(new Rectangle2D.Float(4f, 9f, 8f, 5.5f));
                break;
            case EXPORT:
                g.draw(path(5f, 5f, 2f, 5f, 2f, 14.5f, 14f, 14.5f, 14f, 5f, 11f, 5f));
                g.draw(new Line2D.Float(8f, 10f, 8f, 1.5f));
                g.draw(path(5f, 4.5f, 8f, 1.5f, 11f, 4.5f));
                break;
            case ADD:
                g.draw(new Line2D.Float(8f, 2f, 8f, 14f));
                g.draw(new Line2D.Float(2f, 8f, 14f, 8f));
                break;
            case REMOVE:
                g.draw(new Line2D.Float(2f, 8f, 14f, 8f));
                break;
            case VOID:
                g.draw(new Ellipse2D.Float(1.5f, 1.5f, 13f, 13f));
                g.draw(new Line2D.Float(3.5f, 12.5f, 12.5f, 3.5f));
                break;
            case VIEW:
                Path2D eye = new Path2D.Float();
                eye.moveTo(1f, 8f);
                eye.quadTo(8f, 0.5f, 15f, 8f);
                eye.quadTo(8f, 15.5f, 1f, 8f);
                g.draw(eye);
                g.draw(new Ellipse2D.Float(5.8f, 5.8f, 4.4f, 4.4f));
                break;
            case CLOSE:
                g.draw(new Line2D.Float(3f, 3f, 13f, 13f));
                g.draw(new Line2D.Float(13f, 3f, 3f, 13f));
                break;
            case KEY:
                g.draw(new Ellipse2D.Float(1.5f, 5f, 6f, 6f));
                g.draw(path(7.5f, 8f, 14.5f, 8f, 14.5f, 11f));
                g.draw(new Line2D.Float(11.5f, 8f, 11.5f, 10.5f));
                break;
            case LOGIN:
                g.draw(path(7f, 4f, 7f, 1.5f, 14.5f, 1.5f, 14.5f, 14.5f, 7f, 14.5f, 7f, 12f));
                g.draw(new Line2D.Float(1.5f, 8f, 10.5f, 8f));
                g.draw(path(8f, 5.5f, 10.5f, 8f, 8f, 10.5f));
                break;
            default:
                break;
        }
        g.dispose();
    }

    private static void drawTray(Graphics2D g) {
        g.draw(path(1.5f, 9.5f, 1.5f, 14.5f, 14.5f, 14.5f, 14.5f, 9.5f));
    }

    /** Open path through the given x,y points. */
    private static Path2D path(float... points) {
        Path2D p = new Path2D.Float();
        p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            p.lineTo(points[i], points[i + 1]);
        }
        return p;
    }
}
