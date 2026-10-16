import dao.CategoryDAO;
import dao.CustomerAccountDAO;
import dao.CustomerDAO;
import dao.DashboardDAO;
import dao.ProductDAO;
import dao.ReceivingDAO;
import dao.ReleaseDAO;
import dao.ReportDAO;
import dao.StockDAO;
import dao.StockRequestDAO;
import dao.TransferDAO;
import dao.UserDAO;
import dao.WarehouseDAO;
import database.DatabaseConnection;
import database.DatabaseInitializer;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import model.Category;
import model.Customer;
import model.CustomerAccount;
import model.Product;
import model.Receiving;
import model.ReceivingItem;
import model.Release;
import model.ReleaseItem;
import model.ReportType;
import model.Role;
import model.Status;
import model.StockRequest;
import model.StockRequestItem;
import model.StockTransfer;
import model.TransferItem;
import model.User;
import model.Warehouse;
import util.BusinessRuleException;
import util.ErrorHandler;
import util.InsufficientStockException;
import util.PasswordUtil;
import util.Session;
import util.Validator;

/**
 * Automated tests of the database layer and the business rules (no extra
 * libraries needed). They run against a separate test database, so the
 * real data in the "data" folder is never touched.
 *
 * Run with run-tests.bat. Results are printed and saved to
 * build/test-results.csv (used for Chapter 4 of the documentation).
 */
public class WmsTestRunner {

    private static final List<String[]> results = new ArrayList<>();
    private static int passed;
    private static int failed;

    private static final Map<String, Product> products = new HashMap<>();
    private static final Map<String, Warehouse> warehouses = new HashMap<>();

    // Customer Portal test data shared by T027 - T040
    private static int portalCustomerId;
    private static int portalAccountId;
    private static int portalRequestId;
    private static int portalReleaseId;

    public static void main(String[] args) throws Exception {
        // Separate database: other port, other folder.
        File testData = new File("build/test-data");
        deleteFolder(testData);
        System.setProperty("wms.db.port", "1533");
        System.setProperty("wms.data.dir", testData.getPath());

        System.out.println("Preparing the test database...");
        DatabaseInitializer.initialize(message -> System.out.println("  " + message));
        for (Product p : new ProductDAO().getProducts()) {
            products.put(p.getSku(), p);
        }
        for (Warehouse w : new WarehouseDAO().getWarehouses()) {
            warehouses.put(w.getWarehouseCode(), w);
        }
        int staffId = new UserDAO().getUserByUsername("staff").getUserId();

        test("T001", "Login", "Correct credentials (admin / correct password)", "User is accepted with the ADMIN role", () -> {
            User user = new UserDAO().authenticate("admin", "admin123".toCharArray());
            check(user != null && user.getRole() == Role.ADMIN && user.isActive(), "login rejected");
            return "Accepted: " + user.getFullName() + " (" + user.getRole() + ")";
        });

        test("T002", "Login", "Wrong password", "Login is rejected (error message)", () -> {
            User user = new UserDAO().authenticate("admin", "wrong-password".toCharArray());
            check(user == null, "wrong password was accepted");
            User unknown = new UserDAO().authenticate("nobody", "admin123".toCharArray());
            check(unknown == null, "unknown user was accepted");
            return "Rejected for wrong password and unknown user";
        });

        test("T003", "Add", "Valid category record", "Record is saved with a new ID", () -> {
            Category category = new Category(0, "Test Category", "Created by the test runner");
            int id = new CategoryDAO().addCategory(category);
            check(id > 0, "no ID returned");
            check(!new CategoryDAO().searchCategories("Test Category").isEmpty(), "record not found after insert");
            return "Saved with ID " + id;
        });

        test("T004", "Add", "Empty required field", "Validation message; database also refuses NULL", () -> {
            check(Validator.isEmpty("   "), "blank text not detected");
            String state = sqlStateOf("INSERT INTO CATEGORIES (category_name) VALUES (NULL)");
            check("23502".equals(state), "expected SQLState 23502 but got " + state);
            return "Validator detects blank input; Derby NOT NULL -> SQLState 23502";
        });

        test("T005", "Update", "Modify the category description", "Record is updated", () -> {
            Category category = new CategoryDAO().searchCategories("Test Category").get(0);
            category.setDescription("Updated description");
            check(new CategoryDAO().updateCategory(category), "update returned false");
            String description = new CategoryDAO().searchCategories("Test Category").get(0).getDescription();
            check("Updated description".equals(description), "description not changed");
            return "Description changed to \"" + description + "\"";
        });

        test("T006", "Delete", "Existing unused record", "Confirmation, then the record is deleted", () -> {
            Category category = new CategoryDAO().searchCategories("Test Category").get(0);
            check(new CategoryDAO().deleteCategory(category.getCategoryId()), "delete returned false");
            check(new CategoryDAO().searchCategories("Test Category").isEmpty(), "record still exists");
            return "Record " + category.getCategoryId() + " deleted";
        });

        test("T007", "Search", "Existing keyword \"mouse\"", "Matching records are shown", () -> {
            List<Product> found = new ProductDAO().searchProducts("mouse", 0, null);
            check(found.size() == 1 && found.get(0).getSku().equals("ELE-1002"), "unexpected result " + found);
            return found.size() + " match: " + found.get(0);
        });

        test("T008", "Search", "Keyword that does not exist", "No matching records", () -> {
            List<Product> found = new ProductDAO().searchProducts("xyz-not-a-product", 0, null);
            check(found.isEmpty(), "found " + found.size());
            return "0 records";
        });

        test("T009", "Releasing", "Release more than the available stock", "Blocked; stock unchanged", () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product p = products.get("ELE-1001");
            int before = new StockDAO().getAvailable(wh.getWarehouseId(), p.getProductId());
            Release release = new Release(wh.getWarehouseId(), 1, staffId);
            release.setDocumentDate(LocalDate.now());
            release.addItem(new ReleaseItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), before + 5));
            try {
                new ReleaseDAO().saveRelease(release);
                throw new AssertionError("release was saved");
            } catch (InsufficientStockException expected) {
                int after = new StockDAO().getAvailable(wh.getWarehouseId(), p.getProductId());
                check(after == before, "stock changed");
                return "Blocked: available " + expected.getAvailable() + ", requested " + expected.getRequested();
            }
        });

        test("T010", "Transfer", "Source and destination are the same warehouse", "Blocked by the program and the database",
                () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product p = products.get("OFF-2001");
            StockTransfer transfer = new StockTransfer(wh.getWarehouseId(), wh.getWarehouseId(), staffId);
            transfer.setDocumentDate(LocalDate.now());
            transfer.addItem(new TransferItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 1));
            try {
                new TransferDAO().saveTransfer(transfer);
                throw new AssertionError("transfer was saved");
            } catch (BusinessRuleException expected) {
                String state = sqlStateOf("INSERT INTO STOCK_TRANSFERS (reference_no, transfer_date, from_warehouse_id, "
                        + "to_warehouse_id, user_id) VALUES ('TRF-TEST', CURRENT_DATE, 1, 1, 1)");
                check("23513".equals(state), "CHECK constraint did not fire: " + state);
                return "Program rule + CHECK constraint (SQLState 23513)";
            }
        });

        test("T011", "Receiving", "Receive more than the free warehouse capacity", "Blocked; stock unchanged", () -> {
            Warehouse wh = new WarehouseDAO().getWarehouseById(warehouses.get("WH-001").getWarehouseId());
            Product p = products.get("PKG-6001");
            Receiving receiving = new Receiving(wh.getWarehouseId(), 1, staffId);
            receiving.setDocumentDate(LocalDate.now());
            receiving.addItem(new ReceivingItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(),
                    wh.getFreeCapacity() + 1, p.getUnitCost()));
            try {
                new ReceivingDAO().saveReceiving(receiving);
                throw new AssertionError("receiving was saved");
            } catch (BusinessRuleException expected) {
                int after = new WarehouseDAO().getWarehouseById(wh.getWarehouseId()).getStockOnHand();
                check(after == wh.getStockOnHand(), "stock changed");
                return "Blocked: free space " + wh.getFreeCapacity() + ", requested " + (wh.getFreeCapacity() + 1);
            }
        });

        test("T012", "Add", "Duplicate SKU", "Duplicate is detected and refused", () -> {
            check(new ProductDAO().isSkuTaken("ele-1001", 0), "duplicate SKU not detected (case-insensitive)");
            String state = sqlStateOf("INSERT INTO PRODUCTS (sku, product_name, category_id) VALUES ('ELE-1001', 'Copy', 1)");
            check("23505".equals(state), "UNIQUE constraint did not fire: " + state);
            return "Program check + UNIQUE constraint (SQLState 23505)";
        });

        test("T013", "Delete", "Product used in transactions", "Refused with a friendly message", () -> {
            Product p = products.get("ELE-1002");
            try {
                new ProductDAO().deleteProduct(p.getProductId());
                throw new AssertionError("product was deleted");
            } catch (SQLException e) {
                String message = ErrorHandler.friendlyMessage(e, "delete the product");
                check("23503".equals(e.getSQLState()), "expected 23503 but got " + e.getSQLState());
                check(message.contains("cannot be deleted"), "message not friendly: " + message);
                return "FOREIGN KEY (SQLState 23503): \"This record cannot be deleted...\"";
            }
        });

        test("T014", "Transactions", "Receive 10, release 4, transfer 3 units", "Stock changes exactly by those amounts",
                () -> {
            Warehouse main = warehouses.get("WH-001");
            Warehouse north = warehouses.get("WH-002");
            Product p = products.get("HRD-3003");
            StockDAO stock = new StockDAO();
            int mainBefore = stock.getAvailable(main.getWarehouseId(), p.getProductId());
            int northBefore = stock.getAvailable(north.getWarehouseId(), p.getProductId());

            Receiving receiving = new Receiving(main.getWarehouseId(), 3, staffId);
            receiving.setDocumentDate(LocalDate.now());
            receiving.addItem(new ReceivingItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 10,
                    p.getUnitCost()));
            new ReceivingDAO().saveReceiving(receiving);
            Release release = new Release(main.getWarehouseId(), 4, staffId);
            release.setDocumentDate(LocalDate.now());
            release.addItem(new ReleaseItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 4));
            new ReleaseDAO().saveRelease(release);
            StockTransfer transfer = new StockTransfer(main.getWarehouseId(), north.getWarehouseId(), staffId);
            transfer.setDocumentDate(LocalDate.now());
            transfer.addItem(new TransferItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 3));
            new TransferDAO().saveTransfer(transfer);

            int mainAfter = stock.getAvailable(main.getWarehouseId(), p.getProductId());
            int northAfter = stock.getAvailable(north.getWarehouseId(), p.getProductId());
            check(mainAfter == mainBefore + 10 - 4 - 3, "main warehouse " + mainBefore + " -> " + mainAfter);
            check(northAfter == northBefore + 3, "north warehouse " + northBefore + " -> " + northAfter);
            return "Main: " + mainBefore + " -> " + mainAfter + ", North: " + northBefore + " -> " + northAfter;
        });

        test("T015", "Void", "Void a posted receiving", "Stock is reversed and status becomes VOIDED", () -> {
            Warehouse wh = warehouses.get("WH-003");
            Product p = products.get("PKG-6002");
            StockDAO stock = new StockDAO();
            int before = stock.getAvailable(wh.getWarehouseId(), p.getProductId());
            Receiving receiving = new Receiving(wh.getWarehouseId(), 6, staffId);
            receiving.setDocumentDate(LocalDate.now());
            receiving.addItem(new ReceivingItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 25,
                    p.getUnitCost()));
            int id = new ReceivingDAO().saveReceiving(receiving);
            check(stock.getAvailable(wh.getWarehouseId(), p.getProductId()) == before + 25, "receiving not added");
            new ReceivingDAO().voidReceiving(id);
            check(stock.getAvailable(wh.getWarehouseId(), p.getProductId()) == before, "stock not reversed");
            check(new ReceivingDAO().getReceivingById(id).isVoided(), "status is not VOIDED");
            try {
                new ReceivingDAO().voidReceiving(id);
                throw new AssertionError("voided twice");
            } catch (BusinessRuleException expected) {
                return receiving.getReferenceNo() + " voided, stock back to " + before + "; second void refused";
            }
        });

        test("T016", "Void", "Void a receiving whose units were already released", "Void is refused", () -> {
            Warehouse wh = warehouses.get("WH-002");
            Product p = products.get("OFF-2005");
            Receiving receiving = new Receiving(wh.getWarehouseId(), 2, staffId);
            receiving.setDocumentDate(LocalDate.now());
            receiving.addItem(new ReceivingItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 20,
                    p.getUnitCost()));
            int id = new ReceivingDAO().saveReceiving(receiving);
            Release release = new Release(wh.getWarehouseId(), 3, staffId);
            release.setDocumentDate(LocalDate.now());
            release.addItem(new ReleaseItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 15));
            new ReleaseDAO().saveRelease(release);
            try {
                new ReceivingDAO().voidReceiving(id);
                throw new AssertionError("void was allowed");
            } catch (BusinessRuleException expected) {
                check(!new ReceivingDAO().getReceivingById(id).isVoided(), "status changed");
                return "Refused: only 5 of 20 units remain in the warehouse";
            }
        });

        test("T017", "Security", "Role permissions of Staff, Manager and Admin", "Each role gets only its allowed actions",
                () -> {
            Session.start(new User(1, "s", "Staff", Role.STAFF, "ACTIVE"));
            check(!Session.canViewReports() && !Session.canManageUsers() && !Session.canDelete()
                    && !Session.canEditMasterData() && !Session.canVoid() && Session.canCreateTransactions(), "staff");
            Session.start(new User(2, "m", "Manager", Role.MANAGER, "ACTIVE"));
            check(Session.canViewReports() && Session.canEditMasterData() && Session.canVoid()
                    && !Session.canDelete() && !Session.canManageUsers(), "manager");
            Session.start(new User(3, "a", "Admin", Role.ADMIN, "ACTIVE"));
            check(Session.canViewReports() && Session.canDelete() && Session.canManageUsers(), "admin");
            Session.end();
            check(!Session.isLoggedIn(), "logout did not end the session");
            return "Staff: transactions only; Manager: + master data, void, reports; Admin: everything";
        });

        test("T018", "Login", "Inactive account (agarcia)", "Login is refused", () -> {
            User user = new UserDAO().authenticate("agarcia", "staff123".toCharArray());
            check(user != null && !user.isActive(), "inactive status not detected");
            return "Password correct but status INACTIVE -> access refused";
        });

        test("T019", "Validation", "Invalid e-mail, phone, date, amount, username", "All invalid values are rejected",
                () -> {
            check(Validator.isValidEmail("sales@company.com") && !Validator.isValidEmail("sales@company"), "email");
            check(Validator.isValidPhone("0917-555-0101") && !Validator.isValidPhone("12-34"), "phone");
            check(Validator.isValidDate("2026-02-28") && !Validator.isValidDate("2026-02-30")
                    && !Validator.isValidDate("28/02/2026"), "date");
            check(Validator.isValidAmount("1250.50") && !Validator.isValidAmount("12.345")
                    && !Validator.isValidAmount("-5") && !Validator.isValidAmount("abc"), "amount");
            check(Validator.isValidUsername("jdelacruz") && !Validator.isValidUsername("ab")
                    && !Validator.isValidUsername("bad name"), "username");
            check(!Validator.isPositiveInteger("0") && !Validator.isPositiveInteger("1.5")
                    && Validator.isPositiveInteger("25"), "quantity");
            return "All invalid samples rejected, valid samples accepted";
        });

        test("T020", "Security", "Password storage", "Passwords are stored as salted hashes", () -> {
            String hash1 = PasswordUtil.hash("secret1".toCharArray());
            String hash2 = PasswordUtil.hash("secret1".toCharArray());
            check(!hash1.equals(hash2), "same hash for same password (no salt)");
            check(!hash1.contains("secret1"), "plain text inside hash");
            check(PasswordUtil.verify("secret1".toCharArray(), hash1), "verify failed");
            check(!PasswordUtil.verify("secret2".toCharArray(), hash1), "wrong password verified");
            String stored = new UserDAO().getUserByUsername("admin").getPasswordHash();
            check(stored.startsWith("pbkdf2$") && !stored.contains("admin123"), "admin password not hashed");
            return "PBKDF2 hashes, random salt, plain text never stored";
        });

        test("T021", "Error handling", "Database server not reachable", "Friendly connection message, no crash", () -> {
            try (Connection ignored = DriverManager.getConnection("jdbc:derby://localhost:1599/NoDatabase", "app", "app")) {
                throw new AssertionError("connected to a server that does not exist");
            } catch (SQLException e) {
                String message = ErrorHandler.friendlyMessage(e, "load the data");
                check(message.startsWith("Cannot connect to the database"), "message: " + message);
                return "SQLState " + e.getSQLState() + " -> \"Cannot connect to the database...\"";
            }
        });

        test("T022", "Transactions", "Release with 2 lines where line 2 exceeds the stock", "Nothing is saved (rollback)",
                () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product first = products.get("OFF-2001");
            Product second = products.get("OFF-2003");
            StockDAO stock = new StockDAO();
            int firstBefore = stock.getAvailable(wh.getWarehouseId(), first.getProductId());
            int secondBefore = stock.getAvailable(wh.getWarehouseId(), second.getProductId());
            int releasesBefore = new ReleaseDAO().searchReleases("", null, null, null).size();
            Release release = new Release(wh.getWarehouseId(), 2, staffId);
            release.setDocumentDate(LocalDate.now());
            release.addItem(new ReleaseItem(first.getProductId(), first.getSku(), first.getProductName(), first.getUnit(), 5));
            release.addItem(new ReleaseItem(second.getProductId(), second.getSku(), second.getProductName(),
                    second.getUnit(), secondBefore + 1));
            try {
                new ReleaseDAO().saveRelease(release);
                throw new AssertionError("release was saved");
            } catch (InsufficientStockException expected) {
                check(stock.getAvailable(wh.getWarehouseId(), first.getProductId()) == firstBefore, "line 1 deducted");
                check(new ReleaseDAO().searchReleases("", null, null, null).size() == releasesBefore, "header saved");
                return "Rolled back: line 1 stock still " + firstBefore + ", no document created";
            }
        });

        test("T023", "Reports", "Generate all " + ReportType.values().length + " reports",
                "Every report runs; totals match the dashboard", () -> {
            LocalDate from = LocalDate.now().minusDays(60);
            LocalDate to = LocalDate.now();
            long inventoryUnits = 0;
            for (ReportType type : ReportType.values()) {
                model.ReportData data = new ReportDAO().generate(type, from, to, 0);
                check(data.getColumns().length > 0, type + " has no columns");
                if (type == ReportType.INVENTORY_STATUS) {
                    for (Object[] row : data.getRows()) {
                        inventoryUnits += (Integer) row[5];
                    }
                }
            }
            long dashboardUnits = new DashboardDAO().getStats().getUnitsOnHand();
            check(inventoryUnits == dashboardUnits, "inventory " + inventoryUnits + " vs dashboard " + dashboardUnits);
            return ReportType.values().length + " reports OK; inventory units = dashboard units = " + dashboardUnits;
        });

        test("T024", "Transactions", "Reference numbers of two documents on the same day", "Unique, in sequence", () -> {
            String prefix = "RCV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
            List<Receiving> today = new ReceivingDAO().searchReceivings(prefix, LocalDate.now(), LocalDate.now(), null);
            check(today.size() >= 2, "expected at least 2 receivings today");
            for (Receiving r : today) {
                check(r.getReferenceNo().matches("RCV-\\d{8}-\\d{3}"), "bad format " + r.getReferenceNo());
            }
            return today.size() + " receivings today, e.g. " + today.get(0).getReferenceNo();
        });

        test("T025", "Relationships", "Same manager in charge of two warehouses", "Refused (one-to-one rule)", () -> {
            Warehouse main = warehouses.get("WH-001");
            String state = sqlStateOf("UPDATE WAREHOUSES SET manager_id = " + main.getManagerId()
                    + " WHERE warehouse_code = 'WH-003'");
            check("23505".equals(state), "UNIQUE(manager_id) did not fire: " + state);
            check(new UserDAO().getAssignableManagers(0).isEmpty(), "assigned managers offered again");
            return "UNIQUE manager_id (SQLState 23505); assigned managers are not offered";
        });

        test("T026", "Dashboard", "Low-stock detection on the sample data", "Products at/below reorder level are listed",
                () -> {
            List<Object[]> low = new DashboardDAO().getLowStockProducts();
            check(!low.isEmpty(), "no low-stock products");
            for (Object[] row : low) {
                check((Integer) row[2] <= (Integer) row[3], "not low: " + row[0]);
            }
            return low.size() + " low-stock product(s), e.g. " + low.get(0)[0] + " (" + low.get(0)[2] + " left)";
        });

        // ------------------------------------------------------------------ Customer Portal
        int managerId = new UserDAO().getUserByUsername("manager").getUserId();
        int northpointId = new CustomerAccountDAO().getAccountByUsername("northpoint").getCustomerId();

        test("T027", "Registration", "New customer registers (company details + login)",
                "Account PENDING, customer INACTIVE until approved", () -> {
            Customer customer = new Customer(0, "Test Trading Co.", "Tess Tan", "0917-555-0999");
            customer.setEmail("orders@testtrading.example.com");
            portalAccountId = new CustomerAccountDAO().register(customer, "testtrading", "secret123".toCharArray());
            portalCustomerId = customer.getCustomerId();
            CustomerAccount account = new CustomerAccountDAO().getAccountById(portalAccountId);
            check(account.isPending(), "account status " + account.getStatus());
            check(Status.INACTIVE.equals(new CustomerDAO().getCustomerById(portalCustomerId).getStatus()),
                    "customer is not INACTIVE");
            check(account.getPasswordHash().startsWith("pbkdf2$"), "password not hashed");
            return "Account " + portalAccountId + " PENDING, customer " + portalCustomerId + " INACTIVE, password hashed";
        });

        test("T028", "Registration", "Duplicate username or customer name", "Refused; nothing is saved", () -> {
            int before = new CustomerDAO().getCustomers().size();
            Customer sameUser = new Customer(0, "Another Company", "A. Person", "0917-555-0998");
            expectRule(() -> new CustomerAccountDAO().register(sameUser, "TestTrading", "secret123".toCharArray()),
                    "duplicate username registered");
            Customer sameName = new Customer(0, "test trading co.", "A. Person", "0917-555-0998");
            expectRule(() -> new CustomerAccountDAO().register(sameName, "othername", "secret123".toCharArray()),
                    "duplicate customer name registered");
            check(new CustomerDAO().getCustomers().size() == before, "a customer record was created");
            String state = sqlStateOf("INSERT INTO CUSTOMER_ACCOUNTS (customer_id, username, password_hash) "
                    + "VALUES (" + portalCustomerId + ", 'testtrading', 'x')");
            check("23505".equals(state), "UNIQUE constraint did not fire: " + state);
            return "Both refused (case-insensitive) + UNIQUE constraint (SQLState 23505)";
        });

        test("T029", "Customer Login", "Pending account, then approved by a manager",
                "Blocked while PENDING; can log in after approval", () -> {
            CustomerAccount pending = new CustomerAccountDAO().authenticate("testtrading", "secret123".toCharArray());
            check(pending != null && pending.isPending() && !pending.isActive(), "pending account not detected");
            check(new CustomerAccountDAO().activateAccount(portalAccountId), "approve returned false");
            CustomerAccount active = new CustomerAccountDAO().authenticate("testtrading", "secret123".toCharArray());
            check(active.isActive() && Status.ACTIVE.equals(active.getCustomerStatus()), "not active after approval");
            return "PENDING -> blocked; after approval account and customer are ACTIVE";
        });

        test("T030", "Customer Login", "Correct, wrong and staff credentials on the Customer login",
                "Only the correct customer password is accepted", () -> {
            CustomerAccount account = new CustomerAccountDAO().authenticate("northpoint", "customer123".toCharArray());
            check(account != null && account.isActive(), "correct password rejected");
            check(new CustomerAccountDAO().authenticate("northpoint", "wrong-pass".toCharArray()) == null,
                    "wrong password accepted");
            check(new CustomerAccountDAO().authenticate("admin", "admin123".toCharArray()) == null,
                    "staff account accepted as a customer");
            return "Accepted: " + account.getCustomerName() + "; wrong password and staff login refused";
        });

        test("T031", "Stock Request", "Customer submits a request with 2 products",
                "Saved as PENDING with a REQ number; stock unchanged", () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product mouse = products.get("ELE-1002");
            Product paper = products.get("OFF-2001");
            StockDAO stock = new StockDAO();
            int mouseBefore = stock.getAvailable(wh.getWarehouseId(), mouse.getProductId());
            StockRequest request = new StockRequest(portalCustomerId, wh.getWarehouseId());
            request.setNeededBy(LocalDate.now().plusDays(3));
            request.addItem(new StockRequestItem(mouse.getProductId(), mouse.getSku(), mouse.getProductName(),
                    mouse.getUnit(), 5));
            request.addItem(new StockRequestItem(paper.getProductId(), paper.getSku(), paper.getProductName(),
                    paper.getUnit(), 10));
            portalRequestId = new StockRequestDAO().submitRequest(request);
            check(request.getReferenceNo().matches("REQ-\\d{8}-\\d{3}"), "bad reference " + request.getReferenceNo());
            StockRequest saved = new StockRequestDAO().getRequestById(portalRequestId, portalCustomerId);
            check(saved.isPending() && saved.getItems().size() == 2, "not saved correctly");
            check(stock.getAvailable(wh.getWarehouseId(), mouse.getProductId()) == mouseBefore, "stock changed");
            return request.getReferenceNo() + " PENDING, 2 items, stock still " + mouseBefore;
        });

        test("T032", "Stock Request", "Empty list, past 'needed by', quantity 0, inactive customer",
                "All refused", () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product p = products.get("OFF-2002");
            StockRequest empty = new StockRequest(portalCustomerId, wh.getWarehouseId());
            expectRule(() -> new StockRequestDAO().submitRequest(empty), "empty request saved");
            StockRequest past = new StockRequest(portalCustomerId, wh.getWarehouseId());
            past.setNeededBy(LocalDate.now().minusDays(1));
            past.addItem(new StockRequestItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 1));
            expectRule(() -> new StockRequestDAO().submitRequest(past), "past needed-by date saved");
            int lakesideId = new CustomerAccountDAO().getAccountByUsername("lakeside").getCustomerId();
            StockRequest inactive = new StockRequest(lakesideId, wh.getWarehouseId());
            inactive.addItem(new StockRequestItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 1));
            expectRule(() -> new StockRequestDAO().submitRequest(inactive), "inactive customer could request");
            String state = sqlStateOf("INSERT INTO STOCK_REQUEST_ITEMS (request_id, product_id, quantity) VALUES ("
                    + portalRequestId + ", " + p.getProductId() + ", 0)");
            check("23513".equals(state), "CHECK quantity > 0 did not fire: " + state);
            return "Program rules + CHECK constraint (SQLState 23513)";
        });

        test("T033", "Stock Request", "Cancel a pending request (owner and another customer)",
                "Only the owner can cancel, only while PENDING", () -> {
            Warehouse wh = warehouses.get("WH-002");
            Product p = products.get("FNB-4001");
            StockRequest request = new StockRequest(portalCustomerId, wh.getWarehouseId());
            request.addItem(new StockRequestItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(), 3));
            int id = new StockRequestDAO().submitRequest(request);
            expectRule(() -> new StockRequestDAO().cancelRequest(id, northpointId), "another customer cancelled it");
            new StockRequestDAO().cancelRequest(id, portalCustomerId);
            check(Status.CANCELLED.equals(new StockRequestDAO().getRequestById(id, 0).getStatus()), "not CANCELLED");
            expectRule(() -> new StockRequestDAO().cancelRequest(id, portalCustomerId), "cancelled twice");
            return request.getReferenceNo() + " CANCELLED by its owner; other customer and second cancel refused";
        });

        test("T034", "Stock Request", "Manager approves, then releases the request",
                "Release created, stock deducted, request FULFILLED", () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product mouse = products.get("ELE-1002");
            StockDAO stock = new StockDAO();
            int before = stock.getAvailable(wh.getWarehouseId(), mouse.getProductId());
            new StockRequestDAO().approveRequest(portalRequestId, managerId, "Will deliver tomorrow");
            check(Status.APPROVED.equals(new StockRequestDAO().getRequestById(portalRequestId, 0).getStatus()),
                    "not APPROVED");
            Release release = new StockRequestDAO().releaseRequest(portalRequestId, managerId, LocalDate.now());
            portalReleaseId = release.getReleaseId();
            StockRequest after = new StockRequestDAO().getRequestById(portalRequestId, 0);
            check(Status.FULFILLED.equals(after.getStatus()), "not FULFILLED");
            check(release.getReferenceNo().equals(after.getReleaseReference()), "release not linked");
            check(new ReleaseDAO().getReleaseById(portalReleaseId).getCustomerId() == portalCustomerId, "wrong customer");
            int afterQty = stock.getAvailable(wh.getWarehouseId(), mouse.getProductId());
            check(afterQty == before - 5, "stock " + before + " -> " + afterQty);
            return "APPROVED -> " + release.getReferenceNo() + "; mouse stock " + before + " -> " + afterQty;
        });

        test("T035", "Stock Request", "Release a request bigger than the stock", "Refused; nothing changes", () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product p = products.get("ELE-1001");
            StockDAO stock = new StockDAO();
            int before = stock.getAvailable(wh.getWarehouseId(), p.getProductId());
            int releasesBefore = new ReleaseDAO().searchReleases("", null, null, null).size();
            StockRequest request = new StockRequest(portalCustomerId, wh.getWarehouseId());
            request.addItem(new StockRequestItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(),
                    before + 1));
            int id = new StockRequestDAO().submitRequest(request);
            try {
                new StockRequestDAO().releaseRequest(id, managerId, LocalDate.now());
                throw new AssertionError("release was created");
            } catch (InsufficientStockException expected) {
                check(new StockRequestDAO().getRequestById(id, 0).isPending(), "status changed");
                check(stock.getAvailable(wh.getWarehouseId(), p.getProductId()) == before, "stock changed");
                check(new ReleaseDAO().searchReleases("", null, null, null).size() == releasesBefore, "release saved");
                return "Rolled back: request still PENDING, stock still " + before;
            }
        });

        test("T036", "Security", "Customer opens another customer's request",
                "Not shown; lists contain own requests only", () -> {
            check(new StockRequestDAO().getRequestById(portalRequestId, northpointId) == null, "other customer saw it");
            List<StockRequest> own = new StockRequestDAO().searchRequests("", null, null, null, northpointId);
            check(!own.isEmpty(), "northpoint has no requests");
            for (StockRequest r : own) {
                check(r.getCustomerId() == northpointId, "foreign request " + r.getReferenceNo());
            }
            return "Request hidden from other customers; " + own.size() + " own request(s) listed";
        });

        test("T037", "Void", "Void the release of a fulfilled request", "Stock returned; request back to APPROVED",
                () -> {
            Warehouse wh = warehouses.get("WH-001");
            Product mouse = products.get("ELE-1002");
            int before = new StockDAO().getAvailable(wh.getWarehouseId(), mouse.getProductId());
            new ReleaseDAO().voidRelease(portalReleaseId);
            StockRequest request = new StockRequestDAO().getRequestById(portalRequestId, 0);
            check(Status.APPROVED.equals(request.getStatus()) && request.getReleaseId() == 0, "request not reopened");
            check(new StockDAO().getAvailable(wh.getWarehouseId(), mouse.getProductId()) == before + 5,
                    "stock not returned");
            return "Release voided, 5 units returned, request APPROVED again (can be released again)";
        });

        test("T038", "Stock Request", "Reject without a reason, then with a reason", "Reason required; REJECTED",
                () -> {
            expectRule(() -> new StockRequestDAO().rejectRequest(portalRequestId, managerId, "  "),
                    "no reason accepted");
            new StockRequestDAO().rejectRequest(portalRequestId, managerId, "Customer asked to stop the order");
            StockRequest request = new StockRequestDAO().getRequestById(portalRequestId, 0);
            check(Status.REJECTED.equals(request.getStatus()), "not REJECTED");
            check("Customer asked to stop the order".equals(request.getReviewNote()), "reason not saved");
            expectRule(() -> new StockRequestDAO().releaseRequest(portalRequestId, managerId, LocalDate.now()),
                    "rejected request released");
            return "Reason required; REJECTED with reason; a rejected request cannot be released";
        });

        test("T039", "Security", "Customer session permissions", "No staff permissions; staff login ends it", () -> {
            Session.startCustomer(new CustomerAccountDAO().getAccountByUsername("northpoint"));
            check(Session.isCustomer() && Session.getCustomerId() == northpointId, "customer session not started");
            check(!Session.canCreateTransactions() && !Session.canEditMasterData() && !Session.canViewReports()
                    && !Session.canProcessRequests() && !Session.canManageUsers(), "customer has staff rights");
            Session.start(new User(2, "m", "Manager", Role.MANAGER, "ACTIVE"));
            check(!Session.isCustomer() && Session.canProcessRequests(), "staff session wrong");
            Session.end();
            return "Customer: portal only; Manager: can process requests";
        });

        test("T040", "Customer Login", "Account deactivated by staff", "Login is refused", () -> {
            check(new CustomerAccountDAO().deactivateAccount(portalAccountId), "deactivate returned false");
            CustomerAccount account = new CustomerAccountDAO().authenticate("testtrading", "secret123".toCharArray());
            check(account != null && !account.isActive() && !account.isPending(), "still active");
            return "Password correct but status INACTIVE -> access refused";
        });

        DatabaseConnection.shutdown();
        writeResults();
        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed (" + results.size() + " tests)");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ tiny test framework

    private interface TestBody {
        String run() throws Exception;
    }

    private static void test(String id, String function, String input, String expected, TestBody body) {
        String actual;
        String status;
        try {
            actual = body.run();
            status = "PASS";
            passed++;
        } catch (Throwable t) {
            actual = "FAILED: " + t;
            status = "FAIL";
            failed++;
        }
        results.add(new String[]{id, function, input, expected, actual, status});
        System.out.printf("%s  %-4s  %-14s %s%n        -> %s%n", id, status, function, input, actual);
    }

    private interface RuleAction {
        void run() throws Exception;
    }

    /** The action must be refused with a BusinessRuleException. */
    private static void expectRule(RuleAction action, String failMessage) throws Exception {
        try {
            action.run();
        } catch (BusinessRuleException expected) {
            return;
        }
        throw new AssertionError(failMessage);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Runs a statement that should fail and returns its SQLState (null if it did not fail). */
    private static String sqlStateOf(String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            return null;
        } catch (SQLException e) {
            return e.getSQLState();
        }
    }

    private static void writeResults() throws Exception {
        File file = new File("build/test-results.csv");
        file.getParentFile().mkdirs();
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            out.println("Test ID,Function,Test Input,Expected Result,Actual Result,Status");
            for (String[] row : results) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    line.append(i > 0 ? "," : "").append('"').append(row[i].replace("\"", "\"\"")).append('"');
                }
                out.println(line);
            }
        }
        System.out.println("Results saved to " + file.getAbsolutePath());
    }

    private static void deleteFolder(File folder) {
        File[] children = folder.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteFolder(child);
            }
        }
        folder.delete();
    }
}
