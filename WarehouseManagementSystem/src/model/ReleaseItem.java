package model;

/**
 * A product line of a release document (table RELEASE_ITEMS).
 */
public class ReleaseItem extends DocumentItem {

    private int releaseId;

    public ReleaseItem() {
    }

    public ReleaseItem(int productId, String sku, String productName, String unit, int quantity) {
        super(productId, sku, productName, unit, quantity);
    }

    public int getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(int releaseId) {
        this.releaseId = releaseId;
    }
}
