# FlashShoesFX

A flash-sale shoe e-commerce desktop app built in JavaFX for CS-212 (Object Oriented Programming).
Customers browse a shoe catalogue and grab limited-stock items during timed flash sales;
admins manage the catalogue, sales, and orders. The whole project demonstrates inheritance,
polymorphism, encapsulation, Java threading with proper synchronization, and CSV-based file
persistence — see `PROJECT_NOTES.md` for how each rubric item maps to actual code.

## Quick start

**Requirements:** JDK 17+ and Maven (or an IDE with Maven support — IntelliJ IDEA detects
`pom.xml` automatically).

```bash
mvn javafx:run
```

That's it — Maven downloads JavaFX for you and launches the app. If your IDE doesn't use
Maven, see "Running without Maven" below.

### Demo logins
| Role | Email | Password |
|---|---|---|
| Admin | `admin@flashshoes.com` | `admin123` |
| Customer | `alex@example.com` | `customer123` |

Or register a new customer account from the login screen.

### Running without Maven (manual JavaFX SDK)
1. Download the JavaFX SDK for your OS from https://openjfx.io (matching your JDK version).
2. In IntelliJ: File → Project Structure → Libraries → add the SDK's `lib` folder.
3. Add a VM option to your run configuration:
   `--module-path "path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml`
4. Run `com.flashshoes.ui.MainApp`.

## Project structure

```
src/main/java/com/flashshoes/
  model/     Plain domain classes — User hierarchy, Product, FlashSale, Cart,
             Order, OrderItem, PaymentMethod + 3 implementations
  service/   Business logic — InventoryManager (thread-safe stock),
             FlashSaleScheduler (countdown thread), OrderProcessor
             (producer-consumer order settlement), FileManager (CSV I/O),
             DataStore (wires everything together, single source of truth)
  ui/        JavaFX screens — LoginScreen, RegisterScreen, CustomerDashboard,
             AdminDashboard (includes a live concurrency stress-test tab),
             CheckoutDialog
  SmokeTest.java   A standalone, no-GUI-needed test that loads the CSV data
                   and fires 20+ concurrent threads at a flash sale to prove
                   InventoryManager never oversells. Run it directly to see
                   the proof: `mvn compile exec:java -Dexec.mainClass=com.flashshoes.SmokeTest`
                   (or just run the class from your IDE).
data/
  products.csv, users.csv, flashsales.csv, orders.csv
  images/    One placeholder card image per product (see "About the dataset")
generate_data.py   Regenerates all of the above from scratch
```

## About the dataset

The original plan was to use Kaggle's Myntra Fashion Product Images Dataset filtered to
footwear. That dataset needs to be downloaded from kaggle.com, which wasn't reachable from
the sandbox this project was built in — so `generate_data.py` instead **synthesizes** a
realistic 60-item shoe catalogue (real brand-style names, real category/price patterns
across Running/Casual/Formal/Sports/Sandals) and draws a simple labeled placeholder card
per product instead of a real photo.

**To swap in the real Kaggle dataset and real photos:**
1. Download "Fashion Product Images Dataset" from Kaggle, filter `styles.csv` to
   `masterCategory == "Footwear"`.
2. Match each row's `id` to its image in the dataset's `images/` folder.
3. Write your own loader (or adapt `generate_data.py`) that maps those columns into
   `data/products.csv`'s schema (`id,name,brand,category,gender,price,stock,imagePath`) —
   you'll need to invent a `price` column since the dataset doesn't include one.
4. Point `imagePath` at the real downloaded image files instead of `data/images/*.png`.

Re-running `python3 generate_data.py` at any point regenerates fresh synthetic data
(including flash sales timed relative to "now" — useful right before a demo/viva so
countdowns look fresh instead of already-expired).

## Demoing the concurrency requirement

For the viva, the most convincing thing to show is the **Concurrency Demo** tab in the
Admin dashboard: pick a flash sale, set a thread count higher than its remaining stock,
and run it. You'll see exactly as many threads succeed as there was stock, the rest
correctly rejected, and stock lands at exactly zero — never negative. This calls the
exact same `InventoryManager.reserveFlashSaleStock()` method the real checkout flow uses.

## Suggested 3-person task split (adjust as needed)

| Area | Owner | Covers |
|---|---|---|
| Core & Concurrency | Person A | `model/`, `InventoryManager`, `FlashSaleScheduler`, `OrderProcessor`, `SmokeTest` |
| Customer-facing UI | Person B | `LoginScreen`, `RegisterScreen`, `CustomerDashboard`, `CheckoutDialog` |
| Admin UI & Data | Person C | `AdminDashboard`, `FileManager`, `generate_data.py`, dataset swap if you go for real photos |

Everyone should still touch each other's areas over the project (small fixes, polish,
testing) so GitHub commit history shows balanced contribution — the rubric checks this
explicitly.

## What's left for your group to do

This is a complete, compiling, working baseline — not a stub. Realistically still needed:
- Polish pass on UI styling (currently functional, minimally styled — a good CSS file
  in `resources/` would go a long way for the "User Interface" rubric row)
- More thorough input validation and error dialogs
- `UML Class Diagram` export in draw.io/StarUML with strict UML notation (a working
  version of this diagram was built earlier in this conversation as a design reference)
- `ProjectReport.pdf` — design choices, what changed from the original UML while coding,
  and your actual task division
- Push to GitHub, get `GitHubLink.txt` ready, and get on the viva slot list early
