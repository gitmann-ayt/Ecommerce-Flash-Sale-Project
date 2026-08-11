# PROJECT_NOTES — Rubric-to-Code Map

This file exists purely as a working reference for the group and for the viva.
It is **not** one of the four required deliverables (Source Code, UML diagram,
ProjectReport.pdf, GitHubLink.txt) — but its content is exactly what should be
paraphrased into `ProjectReport.pdf`'s "design choices" section, and it's the
fastest way to answer "show me where you did X" in the viva.

## CLO 1 — UML Design & OOP Principles

| Concept | Where |
|---|---|
| Abstraction | `model/User.java` — abstract class, `getRole()` is abstract |
| Inheritance | `Customer extends User`, `Admin extends User` |
| Encapsulation | Every model field is `private`/`protected` with getters/setters; `User.passwordHash` is never exposed, only verified via `login()` |
| Polymorphism (interface) | `PaymentMethod` interface + 3 implementations (`CashOnDelivery`, `CreditCardPayment`, `WalletPayment`) — `Order`/`CheckoutDialog` code depends only on the interface |
| Polymorphism (overriding) | `getRole()` overridden differently in `Customer` and `Admin` |
| Composition | `Customer` owns exactly one `Cart` (created in constructor, never shared); `Order` owns its `OrderItem`s |
| Association/Aggregation | `Cart` references `Product` objects it doesn't own; `FlashSale` references a `Product` |
| Enums for fixed states | `SaleStatus`, `OrderStatus` |

The UML diagram (`uml/uml.png` / `uml/uml.pdf`, source in `uml/uml.dot`) uses
proper notation: hollow-triangle arrows for inheritance, dashed hollow-triangle
for interface realization, filled diamond for composition, open diamond for
"has-a" service ownership, plain arrows for association.

## CLO 2 — Concurrency & Synchronization, Logic & Efficiency

| Concept | Where |
|---|---|
| Per-resource locking (not one global lock) | `InventoryManager` — one `ReentrantLock` per product ID in a `ConcurrentHashMap`, so buying shoe A never blocks someone buying shoe B |
| Atomic counters | `FlashSale.limitedStock` is an `AtomicInteger` |
| Producer–consumer pipeline | `OrderProcessor` — checkout enqueues onto a `BlockingQueue`, a background daemon thread dequeues and settles orders, so the JavaFX thread never blocks on "payment" |
| Scheduled background thread | `FlashSaleScheduler` — `ScheduledExecutorService` ticking every second to flip sale status, always marshaling UI updates back via `Platform.runLater` |
| `synchronized` where a simple mutual-exclusion is enough | `Product.updateStock()` / `getStockQuantity()` |
| Proof of correctness under load | `SmokeTest.java` fires 20+ concurrent threads at one flash sale and asserts stock never goes negative and never oversells; the **Concurrency Demo** tab in `AdminDashboard` does the same thing live, visually, for the viva |

**Viva tip:** run `SmokeTest` first (`mvn compile exec:java -Dexec.mainClass=com.flashshoes.SmokeTest`)
to show console proof, then repeat it live in the Admin → Concurrency Demo tab
so the examiner sees the same guarantee in the actual GUI.

## CLO 3 — User Interface, File Handling

| Concept | Where |
|---|---|
| GUI framework | JavaFX (`ui/` package) — Login, Register, Customer dashboard, Admin dashboard, Checkout dialog |
| Global styling | `src/main/resources/com/flashshoes/css/app.css`, applied to every `Scene` in `MainApp.themedScene()` |
| CSV persistence | `FileManager` — loads/saves `products.csv`, `users.csv`, `flashsales.csv`, `orders.csv` in `data/` |
| Real dataset import | `import_dataset.py` — builds the catalogue from Kaggle's real "Fashion Product Images (Small)" dataset (real names/photos/categories; price and stock are store-assigned, see README's "About the dataset") |
| Data survives restart | `DataStore` loads all CSVs on first access (singleton init) and every mutation (`addProduct`, `addOrder`, `registerCustomer`, ...) immediately persists back to disk |

## CLO 4 — IDE & Version Control

See `GIT_WORKFLOW.md` for the full plan: folder structure, `.gitignore`,
branching, and a day-by-day commit schedule for the group.

## CLO 5 — Ethics & Teamwork

- Keep GitHub commit history honest: each member commits **their own** work
  under their own GitHub account/email — don't have one person push
  everyone's code.
- `ProjectReport.pdf` must state the *real* task split and *real* challenges
  you hit — the rubric explicitly rewards an accurate, detailed report over
  a vague one, and explicitly penalizes an unbalanced team (1-2 people doing
  everything) even if the app itself is great.

## Known limitations to mention proactively in the report (don't let the examiner "find" these first)

- `CreditCardPayment.processPayment()` and `CashOnDelivery` are simulated —
  no real payment gateway is called (correctly out of scope for a course project;
  say so explicitly rather than implying it's real).
- `price` and `stock` in the imported catalogue are store-assigned (category price
  bands + a default stock range), not scraped — because the Kaggle dataset is a
  product-*image* dataset, not a retail export, and no such dataset carries a
  store's live pricing/inventory. Say this plainly if asked; don't imply the prices
  are "real" market prices.
- Login sessions are in-memory only (no "remember me" / persistent session) —
  acceptable for a single-user desktop app.
- `nextProductId()`/`nextUserId()` use `Math.random()` for ID suffixes, which
  has a (very small) theoretical collision chance — fine at this data scale,
  worth a one-line mention as a "future improvement" in the report.