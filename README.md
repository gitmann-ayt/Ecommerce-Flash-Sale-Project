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
src/main/java/com/flashshoes/
model/ Plain domain classes — User hierarchy, Product, FlashSale, Cart,
Order, OrderItem, PaymentMethod + 3 implementations
service/ Business logic — InventoryManager (thread-safe stock),
FlashSaleScheduler (countdown thread), OrderProcessor
(producer-consumer order settlement), FileManager (CSV I/O),
DataStore (wires everything together, single source of truth)
ui/ JavaFX screens — LoginScreen, RegisterScreen, CustomerDashboard,
AdminDashboard (includes a live concurrency stress-test tab),
CheckoutDialog
SmokeTest.java A standalone, no-GUI-needed test that loads the CSV data
and fires 20+ concurrent threads at a flash sale to prove
InventoryManager never oversells. Run it directly to see
the proof: mvn compile exec:java -Dexec.mainClass=com.flashshoes.SmokeTest
(or just run the class from your IDE).
data/
products.csv, users.csv, flashsales.csv, orders.csv
images/ Real product photos copied from the Kaggle dataset (see "About the dataset")
kaggle_dataset/ Where you place the downloaded Kaggle dataset (gitignored — not committed)
import_dataset.py Builds data/*.csv + data/images/ from the real Kaggle dataset

## About the dataset

Product photos, names, categories, gender, and color all come from Kaggle's
**Fashion Product Images (Small)** dataset by paramaggarwal:
https://www.kaggle.com/datasets/paramaggarwal/fashion-product-images-small

Nothing about product identity is invented — `import_dataset.py` reads the real
`styles.csv`, keeps only footwear rows (Casual/Formal/Sports Shoes, Sandals, Flip
Flops, Flats, Heels), maps them onto this app's five category buckets
(Running/Casual/Formal/Sports/Sandals), derives `brand` from the real product title
(Myntra listings are titled `"<Brand> <Gender> <Description>"`), and copies the real
photo for each chosen product into `data/images/`.

Two fields genuinely don't exist in this dataset — no product-photo dataset carries a
store's live price or stock, because that's internal business data, not a catalog
attribute — so `import_dataset.py` assigns those the way any store owner would: a
category-based price band and a default stock range, both set once, near the top of
the script, not per-item guesswork. Search that file for `PRICE_RANGES` and
`STOCK_RANGE` if you want to change them.

**To (re)build the dataset:**
1. Download "Fashion Product Images (Small)" from Kaggle (needs a free Kaggle account).
2. Unzip it so this project folder has `kaggle_dataset/styles.csv` and
   `kaggle_dataset/images/<id>.jpg`.
3. Run `python3 import_dataset.py` (needs `pip install pillow` for the one generic
   "no image" admin icon — everything else uses the standard library only).

Re-running it regenerates `data/*.csv` fresh, including flash sales timed relative to
"now" — useful right before a demo/viva so countdowns look fresh instead of already
expired. `kaggle_dataset/` itself is gitignored (it's ~280MB of someone else's
dataset) — only the filtered, copied-out `data/images/` subset gets committed.

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
| Admin UI & Data | Person C | `AdminDashboard`, `FileManager`, `import_dataset.py` |

Everyone should still touch each other's areas over the project (small fixes, polish,
testing) so GitHub commit history shows balanced contribution — the rubric checks this
explicitly.

## What's left for your group to do

This is a complete, compiling, working baseline — not a stub. A CSS theme
(`resources/com/flashshoes/css/app.css`), the UML diagram (`uml/`), and a
`ProjectReport.docx` template are already included. Realistically still needed:
- Run `import_dataset.py` against the real Kaggle data (see "About the dataset" above)
  and re-check the catalogue looks right (brand extraction, category mapping)
- More thorough input validation and error dialogs
- Fill in the `[ placeholders ]` in `ProjectReport.docx` with your group's actual
  challenges, UML evolution, and task division, then export it to PDF
- Push to GitHub, get `GitHubLink.txt` ready, and get on the viva slot list early