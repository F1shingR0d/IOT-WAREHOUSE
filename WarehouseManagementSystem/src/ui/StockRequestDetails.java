package ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import model.StockRequest;
import model.StockRequestItem;
import util.Formats;

/**
 * Shows one customer stock request with its items. Staff see the exact
 * stock of the warehouse next to every item; customers only see whether
 * the item is in stock (IN STOCK / LOW STOCK / OUT OF STOCK).
 */
final class StockRequestDetails {

    private StockRequestDetails() {
    }

    static void show(Component parent, StockRequest r, boolean staffView) {
        List<Object[]> items = new ArrayList<>();
        int number = 1;
        for (StockRequestItem item : r.getItems()) {
            if (staffView) {
                items.add(new Object[]{number++, item.getSku(), item.getProductName(), item.getUnit(),
                    item.getQuantity(), item.getAvailable(), enoughStock(item)});
            } else {
                items.add(new Object[]{number++, item.getSku(), item.getProductName(), item.getUnit(),
                    item.getQuantity(), r.isOpen() ? item.getAvailability() : "-"});
            }
        }
        String[] columns = staffView
                ? new String[]{"#", "SKU", "Product", "Unit", "Requested", "In Stock Now", "Enough?"}
                : new String[]{"#", "SKU", "Product", "Unit", "Quantity", "Availability"};
        Class<?>[] types = staffView
                ? new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class, Integer.class,
                    String.class}
                : new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class, String.class};

        List<String[]> header = new ArrayList<>();
        header.add(new String[]{"Reference No.", r.getReferenceNo()});
        header.add(new String[]{"Request Date", Formats.date(r.getRequestDate())});
        if (staffView) {
            header.add(new String[]{"Customer", r.getCustomerName()});
        }
        header.add(new String[]{"Warehouse", r.getWarehouseName()});
        header.add(new String[]{"Needed By", r.getNeededBy() == null ? null : Formats.date(r.getNeededBy())});
        header.add(new String[]{"Submitted On", Formats.dateTime(r.getCreatedAt())});
        header.add(new String[]{"Reviewed By", r.getReviewedByName()});
        header.add(new String[]{"Reviewed On", r.getReviewedAt() == null ? null : Formats.dateTime(r.getReviewedAt())});
        header.add(new String[]{"Release No.", r.getReleaseReference()});
        header.add(new String[]{"Warehouse Note", r.getReviewNote()});
        header.add(new String[]{"Remarks", r.getRemarks()});

        new TransactionDetailsDialog(SwingUtilities.getWindowAncestor(parent), "Request " + r.getReferenceNo(),
                r.getStatus(), header.toArray(new String[0][]), columns, types, items,
                r.getItems().size() + " item(s)     Total units: " + Formats.number(r.getItemsQuantity()))
                .setVisible(true);
    }

    /** OK = the whole quantity can be released, LOW = only part of it, OUT = none. */
    private static String enoughStock(StockRequestItem item) {
        if (item.getAvailable() >= item.getQuantity()) {
            return "OK";
        }
        return item.getAvailable() > 0 ? "LOW" : "OUT";
    }
}
