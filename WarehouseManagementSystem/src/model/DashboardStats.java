package model;

import java.math.BigDecimal;

/**
 * The summary numbers shown on the dashboard cards.
 */
public class DashboardStats {

    private int activeWarehouses;
    private int activeProducts;
    private long unitsOnHand;
    private BigDecimal stockValue = BigDecimal.ZERO;
    private int lowStockProducts;
    private int todayReceivings;
    private int todayReceivedUnits;
    private int todayReleases;
    private int todayReleasedUnits;
    private int todayTransfers;
    private int todayTransferredUnits;

    public int getActiveWarehouses() {
        return activeWarehouses;
    }

    public void setActiveWarehouses(int activeWarehouses) {
        this.activeWarehouses = activeWarehouses;
    }

    public int getActiveProducts() {
        return activeProducts;
    }

    public void setActiveProducts(int activeProducts) {
        this.activeProducts = activeProducts;
    }

    public long getUnitsOnHand() {
        return unitsOnHand;
    }

    public void setUnitsOnHand(long unitsOnHand) {
        this.unitsOnHand = unitsOnHand;
    }

    public BigDecimal getStockValue() {
        return stockValue;
    }

    public void setStockValue(BigDecimal stockValue) {
        this.stockValue = stockValue;
    }

    public int getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(int lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public int getTodayReceivings() {
        return todayReceivings;
    }

    public void setTodayReceivings(int todayReceivings) {
        this.todayReceivings = todayReceivings;
    }

    public int getTodayReceivedUnits() {
        return todayReceivedUnits;
    }

    public void setTodayReceivedUnits(int todayReceivedUnits) {
        this.todayReceivedUnits = todayReceivedUnits;
    }

    public int getTodayReleases() {
        return todayReleases;
    }

    public void setTodayReleases(int todayReleases) {
        this.todayReleases = todayReleases;
    }

    public int getTodayReleasedUnits() {
        return todayReleasedUnits;
    }

    public void setTodayReleasedUnits(int todayReleasedUnits) {
        this.todayReleasedUnits = todayReleasedUnits;
    }

    public int getTodayTransfers() {
        return todayTransfers;
    }

    public void setTodayTransfers(int todayTransfers) {
        this.todayTransfers = todayTransfers;
    }

    public int getTodayTransferredUnits() {
        return todayTransferredUnits;
    }

    public void setTodayTransferredUnits(int todayTransferredUnits) {
        this.todayTransferredUnits = todayTransferredUnits;
    }
}
