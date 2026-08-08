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
             ProductDetailsScreen (image, description, sizes, quantity,
             reviews), OrderHistoryScreen (live order-tracking timeline),
             AdminDashboard (includes a live concurrency stress-test tab),
             CheckoutDialog (card / wallet / cash on delivery)
  SmokeTest.java   A standalone, no-GUI-needed test that loads the CSV data
                   and fires 20+ concurrent threads at a flash sale to prove
                   InventoryManager never oversells. Run it directly to see
                   the proof: `mvn compile exec:java -Dexec.mainClass=com.flashshoes.SmokeTest`
                   (or just run the class from your IDE).
data/
  products.csv, users.csv, flashsales.csv, orders.csv, reviews.csv
  images/    One image per product (see "About the dataset")
generate_data.py         Regenerates a synthetic catalogue from scratch
import_fashion_dataset.py  Imports the real Kaggle dataset (see below)
```

## About the dataset

By default (`python3 generate_data.py`), the catalogue is **synthesized**: realistic
brand-style names, real category/price patterns across Running/Casual/Formal/Sports/Sandals,
simple drawn placeholder card images, and seeded fake reviews. This is what ships out of
the box so the app runs immediately with no downloads required.

**To use the real Kaggle dataset (real names, brands, and real photos):**

1. Download **Fashion Product Images (Small)** from Kaggle:
   https://www.kaggle.com/datasets/paramaggarwal/fashion-product-images-small
   Unzip it — you'll get a `styles.csv` file and an `images/` folder of real `.jpg` photos.
2. Run the import script from the project root:
   ```bash
   python3 import_fashion_dataset.py --styles /path/to/styles.csv --images /path/to/images --limit 300
   ```
   This rewrites `data/products.csv` with real product names/brands/prices/sizes/descriptions,
   copies the matching real photos into `data/images/`, and generates `data/reviews.csv`
   with seeded fake reviews.
   - `--limit` caps how many products get imported (sampled evenly across categories) —
     300 keeps the catalogue browsing-fast; raise it if you want more.
   - `--categories` lets you narrow to `Footwear`, `Apparel`, `Accessories`, or any
     combination (default is all three).
3. Re-run `mvn javafx:run` — the catalogue, category chips, and product photos are now
   backed by real data.
4. Your existing `users.csv` (accounts/passwords) is left untouched by the import.

**Note on categories:** the dataset doesn't have a "Khusa" tag specifically — its own
`articleType` field (Casual Shoes, Formal Shoes, Sports Shoes, Sandals, Heels, Flip Flops,
etc.) becomes the category chips you see in the catalogue, since that's what's actually in
the data.

Prices are synthesized either way, since neither dataset includes real prices — the
synthetic version uses category-based ranges (see `PRICE_RANGES` in either script).

## New: browsing & checkout flow

- **Catalogue** now has a search bar (matches name/brand) and clickable category chips
  above the product grid.
- Clicking a product card opens **Product Details**: full image, description, a size
  picker, a quantity stepper (capped at remaining stock), and real/seeded reviews with
  star ratings.
- **Checkout** now actually asks for card number/expiry/CVV when "Credit Card" is
  selected, and validates them (16-digit number, MM/YY, 3–4 digit CVV) before the order
  is allowed to proceed — it no longer silently empties the cart on an unfilled form.
- **"My Orders"** is now a full screen (`OrderHistoryScreen`) showing each order with a
  live status timeline: Placed → Confirmed → Packed → Shipped → Out for Delivery →
  Delivered. `OrderProcessor` advances a submitted order through these stages
  automatically in the background (a few seconds apart) so you can watch it progress
  during a demo.

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
