# I.O.T Warehouse - Warehouse Management System

The warehouse management system of **I.O.T Warehouse**: a Java desktop information system for **warehouses, products, receiving, releasing and stock transfers**.
Built with **Java Swing + AWT** (GUI), **JDBC** and **Apache Derby 10.16** (database).

It keeps a live stock balance for every product in every warehouse. Receiving adds stock, releasing
deducts it, and a transfer moves it between warehouses. Every document is saved in one database
transaction (all or nothing).

Customers can also use the **Customer Portal**: they register an account, log in, browse the
product catalog and request stock from a warehouse. Staff approve the account, then approve,
reject or release each request. Releasing a request creates a normal release document.

---

## 1. Requirements

| Item | Version |
|---|---|
| Java JDK | 17 or newer (tested with JDK 21) |
| Apache NetBeans | Optional. Tested with NetBeans 31 ("Java with Ant" project) |
| Apache Derby | Already included in the `lib` folder, so nothing to install |

---

## 2. How to run

### Option A: NetBeans
1. **File > Open Project**, then select the `WarehouseManagementSystem` folder.
2. Right-click the project and choose **Run** (or press F6).

NetBeans creates `nbproject/build-impl.xml` automatically the first time the project is opened.
**Clean and Build** creates `dist/WarehouseManagementSystem.jar` (run it with `java -jar dist/WarehouseManagementSystem.jar`).

### Option B: without NetBeans
Double-click **`run.bat`**. It compiles the source code and starts the program.

### First run
On the first run the program:
1. starts the Derby database server on `localhost:1527`,
2. creates the database `WarehouseDB` in the `data` folder,
3. creates the 16 tables and loads sample data (3 warehouses, 25 products, suppliers,
   customers, about 30 days of transactions, and Customer Portal accounts and requests).

A database created by an earlier version of the program is upgraded automatically: the 3
Customer Portal tables and their demo accounts are added the next time it starts.

This takes a few seconds. Later runs start immediately.

---

## 3. Demo accounts

| Username | Password | Role | Notes |
|---|---|---|---|
| `admin` | `admin123` | Administrator | Full access, including User Management and deleting records |
| `manager` | `manager123` | Warehouse Manager | Master data, reports, void. In charge of WH-001 |
| `rreyes` | `manager123` | Warehouse Manager | In charge of WH-002 |
| `staff` | `staff123` | Warehouse Staff | Receiving, releasing, transfers. Master data is view-only |
| `agarcia` | `staff123` | Warehouse Staff | **INACTIVE**: shows that inactive accounts cannot log in |

Passwords are stored as PBKDF2 hashes, never as plain text. Change them from **File > Change Password**.

### Customer Portal accounts
On the login screen select **Login as: Customer**.

| Username | Password | Customer | Notes |
|---|---|---|---|
| `northpoint` | `customer123` | Northpoint Retail Mart | Active. Has a pending and a rejected request |
| `goldenharvest` | `customer123` | Golden Harvest Grocery | Active. Has an approved and a cancelled request |
| `lakeside` | `customer123` | Lakeside Cafe & Bakery | **PENDING** registration: cannot log in until a manager approves it |

New customers can create an account with **Register an account** on the login screen.

### What each role can do

| Module / action | Admin | Manager | Staff |
|---|:-:|:-:|:-:|
| Dashboard, Inventory, change own password | yes | yes | yes |
| Warehouses, Categories, Products, Suppliers, Customers | add, edit, delete | add, edit | view only |
| Receiving, Releasing, Stock Transfers | create, void | create, void | create |
| Reports (print / export CSV) | yes | yes | no |
| Customer Requests (approve, reject, release requests; approve customer accounts) | yes | yes | no |
| User Management | yes | no | no |

Customers only see the Customer Portal: Home, Product Catalog (IN STOCK / LOW STOCK / OUT OF
STOCK, never exact quantities), New Request, My Requests (cancel while PENDING) and My Profile.
A customer can never see another customer's requests.

---

## 4. Database

| Setting | Value |
|---|---|
| JDBC URL | `jdbc:derby://localhost:1527/WarehouseDB` |
| User / password | `app` / `app` |
| Driver | Derby Network Client (`lib/derbyclient-10.16.1.1.jar`) |
| Files | `data/WarehouseDB` (created automatically) |
| Scripts | `src/database/schema.sql` (tables 1-13), `src/database/customer_portal.sql` (tables 14-16), `src/database/sample_data.sql` (master data) |

**Viewing the tables in NetBeans while the program runs:**
**Services > Databases > New Connection > Java DB (Network)**. If NetBeans asks for the driver,
choose `lib/derbyclient-10.16.1.1.jar`. Then enter host `localhost`, port `1527`, database
`WarehouseDB`, user `app`, password `app`. Right-click a table and choose **View Data**.
Do not start NetBeans' own "Java DB" server; the program starts its own.

**Reset the database:** close the program and delete the `data` folder. The next run creates a
fresh database with the sample data.

### Tables (16)
`USERS`, `CATEGORIES`, `PRODUCTS`, `WAREHOUSES`, `SUPPLIERS`, `CUSTOMERS`, `WAREHOUSE_STOCK`,
`RECEIVINGS`, `RECEIVING_ITEMS`, `RELEASES`, `RELEASE_ITEMS`, `STOCK_TRANSFERS`, `TRANSFER_ITEMS`,
and for the Customer Portal `CUSTOMER_ACCOUNTS`, `STOCK_REQUESTS`, `STOCK_REQUEST_ITEMS`.
There are 26 foreign keys, covering one-to-one, one-to-many and many-to-many relationships
(see `docs`).

---

## 5. Project structure

```
src/
  Main.java                 program entry point
  database/                 DatabaseConnection, DatabaseInitializer, SampleDataSeeder, SQL scripts
  model/                    one class per table (User, Product, Warehouse, Receiving, ...)
  dao/                      database operations (INSERT/SELECT/UPDATE/DELETE with PreparedStatement)
  ui/                       screens (LoginFrame, DashboardFrame, one panel per module, dialogs)
  ui/components/            reusable Swing/AWT components (buttons, cards, chart, date picker) and the
                            dark theme (DarkMetalTheme + painted combo boxes, scroll bars, tabs, inputs)
  ui/resources/             the I.O.T Warehouse logo (iot_logo.png)
  util/                     validation, password hashing, session, error handling, table helpers
test/
  WmsTestRunner.java        40 automated tests (run-tests.bat)
lib/                        Apache Derby 10.16.1.1 jars
docs/                       project documentation
```

---

## 6. Automated tests

Double-click **`run-tests.bat`**. The tests use a separate test database (`build/test-data`,
port 1533), so your real data is never touched. Results are saved to `build/test-results.csv`.

---

## 7. Defense / presentation walkthrough (Section 43, items A-J)

| # | What to show | How |
|---|---|---|
| A | Login | Log in with a wrong password (error message), then as `admin`. Also try `agarcia` (inactive). |
| B | Database connection | The splash screen and the status bar show `Connected to Apache Derby`. Open the tables in NetBeans (Section 4). |
| C | Add | **Products**: fill in the form and click **Save**. Or **Receiving**: add items and click **Save Receiving**. |
| D | View | Every module shows its records in a JTable. **Inventory** shows the stock per warehouse. |
| E | Search | Type a keyword (e.g. `mouse`) and press Enter. Transaction history also filters by date range and status. |
| F | Update | Click a row, change a field and click **Update**. Show the change in NetBeans (View Data). |
| G | Delete | Delete an unused record (confirmation dialog), e.g. the "Safety Equipment" category. |
| H | Relationships | Try to delete a supplier that has receivings: the foreign key blocks it. The dashboard and reports JOIN the tables. |
| I | Error handling | Leave a required field empty, type a bad e-mail, release more than the stock, or transfer to the same warehouse. |
| J | Reports | **Reports**: Inventory Status, Low Stock, Stock Movement History, and more. Use **Print** and **Export CSV**. |

Stock demo: note a product's stock in **Inventory**, receive 10 units, and the stock goes up by 10.
Void the receiving from **Receiving History** and the stock goes back down.

Customer Portal demo:
1. On the login screen click **Register an account** and create a customer. Try to log in: it is PENDING.
2. Log in as `manager`. The dashboard shows a banner. Open **Customer Requests > Customer Accounts** and approve it.
3. Log in as the new customer (Login as: Customer). Browse the **Product Catalog**, then send a **New Request**.
4. Log in as `manager` again. In **Customer Requests** click **Approve**, then **Release Stock**.
   A release document is created and the stock goes down. The customer sees the request as FULFILLED.

---

## 8. Troubleshooting

| Problem | Solution |
|---|---|
| "Port 1527 is already used" | Close NetBeans' Java DB server (Services > Databases > Java DB > Stop Server) or other Derby programs. |
| "Java not found" when running `run.bat` | Install JDK 17 or newer and make sure `java` and `javac` are on the PATH. |
| Want a clean database | Close the program and delete the `data` folder. |
| Error details | See `data/error.log`. |

To put your group's names on the **Help > About** screen, edit the constants at the top of
`src/ui/AboutDialog.java`.
