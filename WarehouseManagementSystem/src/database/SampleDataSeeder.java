package database;

import dao.CustomerAccountDAO;
import dao.CustomerDAO;
import dao.ProductDAO;
import dao.ReceivingDAO;
import dao.ReleaseDAO;
import dao.StockRequestDAO;
import dao.SupplierDAO;
import dao.TransferDAO;
import dao.UserDAO;
import dao.WarehouseDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import model.Customer;
import model.Product;
import model.Receiving;
import model.ReceivingItem;
import model.Release;
import model.ReleaseItem;
import model.Status;
import model.StockRequest;
import model.StockRequestItem;
import model.StockTransfer;
import model.Supplier;
import model.TransferItem;
import model.Warehouse;
import util.BusinessRuleException;
import util.PasswordUtil;

/**
 * Loads demonstration data on the first run: user accounts (with hashed
 * passwords) and about 30 days of receiving, releasing and transfer
 * documents. The documents are saved through the normal DAO methods, so the
 * warehouse stock is always consistent with the transactions.
 *
 * The demo accounts are listed in README.md.
 */
final class SampleDataSeeder {

    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Warehouse> warehouses = new HashMap<>();
    private final Map<String, Supplier> suppliers = new HashMap<>();
    private final Map<String, Customer> customers = new HashMap<>();
    private final Map<String, Integer> users = new HashMap<>();
    private final LocalDate today = LocalDate.now();

    private SampleDataSeeder() {
    }

    /** Demo user accounts (one per role, plus an inactive account). */
    static void insertUsers(Connection conn) throws SQLException {
        String sql = "INSERT INTO USERS (username, password_hash, full_name, role, status) VALUES (?, ?, ?, ?, ?)";
        String[][] accounts = {
            {"admin", "admin123", "System Administrator", "ADMIN", "ACTIVE"},
            {"manager", "manager123", "Maria Santos", "MANAGER", "ACTIVE"},
            {"staff", "staff123", "Juan Dela Cruz", "STAFF", "ACTIVE"},
            {"rreyes", "manager123", "Roberto Reyes", "MANAGER", "ACTIVE"},
            {"agarcia", "staff123", "Ana Garcia", "STAFF", "INACTIVE"}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] account : accounts) {
                ps.setString(1, account[0]);
                ps.setString(2, PasswordUtil.hash(account[1].toCharArray()));
                ps.setString(3, account[2]);
                ps.setString(4, account[3]);
                ps.setString(5, account[4]);
                ps.executeUpdate();
            }
        }
    }

    /** About 30 days of sample documents, in date order. */
    static void insertTransactions() throws SQLException, BusinessRuleException {
        new SampleDataSeeder().run();
    }

    private void run() throws SQLException, BusinessRuleException {
        loadLookups();

        receive(30, "WH-001", "Allied Electronics Distributors", "staff", "Initial stock - DR No. 10231",
                "ELE-1001", 60, "ELE-1002", 300, "ELE-1003", 200, "ELE-1004", 250, "ELE-1005", 120);
        receive(29, "WH-001", "PrimeOffice Supply Co.", "staff", "Initial stock - DR No. 55120",
                "OFF-2001", 800, "OFF-2002", 150, "OFF-2003", 80, "OFF-2004", 200, "OFF-2005", 90);
        receive(28, "WH-001", "BuildRight Hardware Trading", "manager", "Initial stock - DR No. 7781",
                "HRD-3001", 40, "HRD-3002", 120, "HRD-3003", 70, "HRD-3004", 150);
        receive(27, "WH-002", "FreshSource Food Corporation", "staff", "DR No. 30044",
                "FNB-4001", 400, "FNB-4002", 300, "FNB-4003", 60, "FNB-4004", 200);
        receive(26, "WH-002", "CleanPro Industrial Supply", "staff", "DR No. 1198",
                "CLN-5001", 250, "CLN-5002", 150, "CLN-5003", 300);
        receive(25, "WH-003", "PackWell Packaging Solutions", "staff", "DR No. 6620",
                "PKG-6001", 1500, "PKG-6002", 600, "PKG-6003", 80);
        transfer(24, "WH-001", "WH-002", "manager", "Replenishment for North DC",
                "ELE-1002", 80, "ELE-1003", 60, "OFF-2001", 200);
        release(23, "WH-001", "Metro Office Solutions Inc.", "staff", "Customer PO 5521",
                "OFF-2001", 250, "OFF-2002", 60, "OFF-2004", 50);
        transfer(22, "WH-001", "WH-003", "manager", "Stock for South DC hardware section",
                "HRD-3002", 40, "HRD-3004", 50, "OFF-2004", 60);
        release(21, "WH-002", "Golden Harvest Grocery", "staff", "Customer PO GH-2231",
                "FNB-4001", 150, "FNB-4002", 100, "FNB-4004", 80);
        receive(20, "WH-003", "FreshSource Food Corporation", "manager", "DR No. 30102",
                "FNB-4001", 200, "FNB-4004", 100);
        release(19, "WH-001", "Sunrise Construction Corp.", "staff", "Project site delivery",
                "HRD-3001", 25, "HRD-3002", 30, "HRD-3003", 55);
        release(17, "WH-001", "Northpoint Retail Mart", "staff", "Customer PO NP-8841",
                "ELE-1002", 120, "ELE-1004", 150, "ELE-1005", 70);
        transfer(15, "WH-002", "WH-003", "staff", "Rebalancing stock",
                "FNB-4002", 80, "CLN-5002", 40);
        receive(14, "WH-001", "Allied Electronics Distributors", "staff", "DR No. 10388",
                "ELE-1002", 150, "ELE-1004", 100);
        release(12, "WH-002", "Bayview Hotel & Suites", "staff", "Housekeeping supplies",
                "CLN-5001", 180, "CLN-5002", 100, "FNB-4001", 120);
        release(10, "WH-003", "Northpoint Retail Mart", "staff", "Customer PO NP-8902",
                "PKG-6001", 500, "PKG-6003", 68);
        release(8, "WH-001", "Metro Office Solutions Inc.", "staff", "Office fit-out",
                "ELE-1001", 52, "ELE-1003", 90);
        receive(7, "WH-002", "PrimeOffice Supply Co.", "staff", "DR No. 55301",
                "OFF-2001", 300, "OFF-2004", 100);
        transfer(6, "WH-003", "WH-001", "staff", "Packaging for main warehouse",
                "PKG-6001", 400, "PKG-6002", 150);
        int wrongDelivery = receive(5, "WH-001", "PrimeOffice Supply Co.", "staff",
                "Wrong item delivered - returned to supplier", "OFF-2002", 40);
        new ReceivingDAO().voidReceiving(wrongDelivery);
        release(4, "WH-002", "Golden Harvest Grocery", "staff", "Customer PO GH-2290",
                "FNB-4003", 50, "FNB-4002", 90);
        release(3, "WH-003", "Sunrise Construction Corp.", "staff", "Project site delivery",
                "HRD-3002", 35, "HRD-3004", 20);
        receive(2, "WH-001", "BuildRight Hardware Trading", "staff", "DR No. 7840",
                "HRD-3004", 100, "HRD-3002", 50);
        release(1, "WH-001", "Metro Office Solutions Inc.", "staff", "Customer PO 5580",
                "OFF-2003", 72);
        receive(0, "WH-003", "CleanPro Industrial Supply", "staff", "DR No. 1250",
                "CLN-5001", 120, "CLN-5003", 200);
        transfer(0, "WH-001", "WH-002", "staff", "Power banks for North DC",
                "ELE-1005", 30);
        release(0, "WH-002", "Bayview Hotel & Suites", "staff", "Weekly supplies",
                "FNB-4001", 60, "CLN-5003", 90);
    }

    /**
     * Demo data of the Customer Portal: two active customer accounts, one
     * registration waiting for approval and a few stock requests in different
     * states. No request is released here, so the stock is not changed.
     */
    static void insertCustomerPortalData() throws SQLException, BusinessRuleException {
        new SampleDataSeeder().runCustomerPortal();
    }

    private void runCustomerPortal() throws SQLException, BusinessRuleException {
        loadLookups();
        CustomerAccountDAO accountDAO = new CustomerAccountDAO();
        Customer northpoint = customers.get("Northpoint Retail Mart");
        Customer golden = customers.get("Golden Harvest Grocery");
        if (northpoint == null || golden == null) {
            return;   // the sample customers were renamed or removed
        }
        accountDAO.addAccount(northpoint.getCustomerId(), "northpoint", "customer123".toCharArray(), Status.ACTIVE);
        accountDAO.addAccount(golden.getCustomerId(), "goldenharvest", "customer123".toCharArray(), Status.ACTIVE);

        Customer lakeside = new Customer(0, "Lakeside Cafe & Bakery", "Liza Mercado", "0932-555-0206");
        lakeside.setEmail("orders@lakeside.example.com");
        lakeside.setAddress("Lakeshore Drive, Taguig City");
        accountDAO.register(lakeside, "lakeside", "customer123".toCharArray());

        int reviewer = users.get("manager");
        StockRequestDAO requestDAO = new StockRequestDAO();
        request(northpoint, 0, 5, "WH-001", "For the new branch opening", "ELE-1002", 40, "OFF-2001", 120);
        int approved = request(golden, 2, 4, "WH-002", "Weekly grocery restock", "FNB-4001", 60, "FNB-4002", 40);
        requestDAO.approveRequest(approved, reviewer, "Scheduled for delivery this week.");
        int rejected = request(northpoint, 6, 0, "WH-003", null, "PKG-6003", 200);
        requestDAO.rejectRequest(rejected, reviewer,
                "Not enough stock of bubble wrap rolls. Please request again next month.");
        int cancelled = request(golden, 9, 0, "WH-002", "Cleaning supplies", "CLN-5001", 30);
        requestDAO.cancelRequest(cancelled, golden.getCustomerId());
    }

    /** skuAndQuantity: "SKU", quantity, ...; neededInDays 0 = no "needed by" date. */
    private int request(Customer customer, int daysAgo, int neededInDays, String warehouse, String remarks,
                        Object... skuAndQuantity) throws SQLException, BusinessRuleException {
        StockRequest request = new StockRequest(customer.getCustomerId(), warehouses.get(warehouse).getWarehouseId());
        request.setRequestDate(today.minusDays(daysAgo));
        if (neededInDays > 0) {
            request.setNeededBy(today.plusDays(neededInDays));
        }
        request.setRemarks(remarks);
        for (int i = 0; i < skuAndQuantity.length; i += 2) {
            Product p = products.get((String) skuAndQuantity[i]);
            request.addItem(new StockRequestItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(),
                    (Integer) skuAndQuantity[i + 1]));
        }
        return new StockRequestDAO().submitRequest(request);
    }

    private void loadLookups() throws SQLException {
        for (Product p : new ProductDAO().getProducts()) {
            products.put(p.getSku(), p);
        }
        for (Warehouse w : new WarehouseDAO().getWarehouses()) {
            warehouses.put(w.getWarehouseCode(), w);
        }
        for (Supplier s : new SupplierDAO().getSuppliers()) {
            suppliers.put(s.getSupplierName(), s);
        }
        for (Customer c : new CustomerDAO().getCustomers()) {
            customers.put(c.getCustomerName(), c);
        }
        UserDAO userDAO = new UserDAO();
        for (String username : new String[]{"admin", "manager", "staff", "rreyes"}) {
            users.put(username, userDAO.getUserByUsername(username).getUserId());
        }
    }

    /** skuAndQuantity: "SKU", quantity, "SKU", quantity, ... */
    private int receive(int daysAgo, String warehouse, String supplier, String user, String remarks,
                        Object... skuAndQuantity) throws SQLException, BusinessRuleException {
        Receiving receiving = new Receiving(warehouses.get(warehouse).getWarehouseId(),
                suppliers.get(supplier).getSupplierId(), users.get(user));
        receiving.setDocumentDate(today.minusDays(daysAgo));
        receiving.setRemarks(remarks);
        for (int i = 0; i < skuAndQuantity.length; i += 2) {
            Product p = products.get((String) skuAndQuantity[i]);
            receiving.addItem(new ReceivingItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(),
                    (Integer) skuAndQuantity[i + 1], p.getUnitCost()));
        }
        return new ReceivingDAO().saveReceiving(receiving);
    }

    private void release(int daysAgo, String warehouse, String customer, String user, String remarks,
                         Object... skuAndQuantity) throws SQLException, BusinessRuleException {
        Release release = new Release(warehouses.get(warehouse).getWarehouseId(),
                customers.get(customer).getCustomerId(), users.get(user));
        release.setDocumentDate(today.minusDays(daysAgo));
        release.setRemarks(remarks);
        for (int i = 0; i < skuAndQuantity.length; i += 2) {
            Product p = products.get((String) skuAndQuantity[i]);
            release.addItem(new ReleaseItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(),
                    (Integer) skuAndQuantity[i + 1]));
        }
        new ReleaseDAO().saveRelease(release);
    }

    private void transfer(int daysAgo, String fromWarehouse, String toWarehouse, String user, String remarks,
                          Object... skuAndQuantity) throws SQLException, BusinessRuleException {
        StockTransfer transfer = new StockTransfer(warehouses.get(fromWarehouse).getWarehouseId(),
                warehouses.get(toWarehouse).getWarehouseId(), users.get(user));
        transfer.setDocumentDate(today.minusDays(daysAgo));
        transfer.setRemarks(remarks);
        for (int i = 0; i < skuAndQuantity.length; i += 2) {
            Product p = products.get((String) skuAndQuantity[i]);
            transfer.addItem(new TransferItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(),
                    (Integer) skuAndQuantity[i + 1]));
        }
        new TransferDAO().saveTransfer(transfer);
    }
}
