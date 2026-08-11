"""
import_dataset.py — builds data/products.csv (+ copies real product images)
from the Kaggle "Fashion Product Images (Small)" dataset by paramaggarwal:
https://www.kaggle.com/datasets/paramaggarwal/fashion-product-images-small

WHAT THIS REPLACES: the old generate_data.py invented fake product names,
fake brands, and drew placeholder sneaker silhouettes with PIL. None of that
happens anymore. Every product's name, image, category, gender, and color
below is read directly from the real dataset. See the README section this
script prints for the two fields the dataset does NOT provide (price, stock)
and how they're handled.

SETUP — before running this script:
  1. Download+unzip the Kaggle dataset (you need a Kaggle account).
  2. Place it so this project has:
        kaggle_dataset/styles.csv
        kaggle_dataset/images/<id>.jpg
     (i.e. put the extracted "styles.csv" and "images" folder from the
     Kaggle zip into a "kaggle_dataset" folder next to this script.)
  3. Run:  python3 import_dataset.py
"""
import csv
import hashlib
import os
import random
import shutil
from datetime import datetime, timedelta

random.seed(42)

BASE = os.path.dirname(os.path.abspath(__file__))
KAGGLE_DIR = os.path.join(BASE, "kaggle_dataset")
KAGGLE_CSV = os.path.join(KAGGLE_DIR, "styles.csv")
KAGGLE_IMAGES = os.path.join(KAGGLE_DIR, "images")

DATA_DIR = os.path.join(BASE, "data")
IMG_DIR = os.path.join(DATA_DIR, "images")
os.makedirs(IMG_DIR, exist_ok=True)

if not os.path.isfile(KAGGLE_CSV):
    raise SystemExit(
        f"Can't find {KAGGLE_CSV}.\n"
        "Download https://www.kaggle.com/datasets/paramaggarwal/fashion-product-images-small ,\n"
        "unzip it, and place styles.csv + images/ under a 'kaggle_dataset' folder next to this script."
    )

# The five categories your app's UI already uses (see AdminDashboard's category
# dropdown). Every Kaggle articleType we accept gets mapped onto one of these,
# so the imported catalogue is a drop-in replacement — no Java/UI changes needed.
ARTICLE_TYPE_TO_CATEGORY = {
    "Casual Shoes": "Casual",
    "Flats": "Casual",
    "Formal Shoes": "Formal",
    "Heels": "Formal",
    "Sports Shoes": "Sports",
    "Sandals": "Sandals",
    "Flip Flops": "Sandals",
}
ACCEPTED_GENDERS = {"Men", "Women", "Unisex"}

# Multi-word brands seen in this dataset (Myntra listings) that a naive
# "take the first word" split would truncate — e.g. "Red Tape Men Leather
# Black Shoes" would otherwise become brand="Red" instead of "Red Tape".
# Checked longest-first, case-insensitive, against the start of the name.
MULTI_WORD_BRANDS = [
    "United Colors Of Benetton", "Call It Spring", "US Polo Assn",
    "U.S. Polo Assn", "Free Authority", "Bruno Manetti", "Lino Perros",
    "Alberto Torresi", "Hush Puppies", "New Balance", "Under Armour",
    "Steve Madden", "Metro Shoes", "San Frissco", "El Paso",
    "Red Tape", "Flying Machine", "Franco Leone", "Lee Cooper",
    "Carlton London", "Warner Bros", "Inc 5", "F Sports",
]
_MULTI_WORD_BRANDS_SORTED = sorted(MULTI_WORD_BRANDS, key=lambda b: -len(b.split()))


def extract_brand(name: str) -> str:
    """Real Myntra listings are titled "<Brand> <Gender> <Description>".
    Most brands are one word (Nike, Puma, GAS) so a plain first-word split
    works, but some are two-plus words — check the known list first."""
    lower = name.lower()
    for candidate in _MULTI_WORD_BRANDS_SORTED:
        cand_lower = candidate.lower()
        if lower == cand_lower or lower.startswith(cand_lower + " "):
            return candidate
    return name.split(" ")[0]


# --- Fields the Kaggle dataset does NOT contain: price and starting stock. ---
# These are store-set business numbers (every retailer sets its own price and
# stock for a catalogue, real photos and category or not) — not product
# identity, so assigning them here isn't "synthetic data" in the sense that
# matters. Ranges mirror realistic footwear retail pricing per category.
PRICE_RANGES = {
    "Casual":  (30, 95),
    "Formal":  (55, 160),
    "Sports":  (40, 120),
    "Sandals": (18, 55),
}
STOCK_RANGE = (8, 60)

# Cap how many real products we pull per category, so the repo/zip stays a
# reasonable size (this dataset has 44k+ images total). Raise this if you want
# a bigger catalogue.
MAX_PER_CATEGORY = 15

# ---------------- READ + FILTER THE REAL DATASET ----------------

rows_by_category = {cat: [] for cat in set(ARTICLE_TYPE_TO_CATEGORY.values())}

with open(KAGGLE_CSV, newline="", encoding="utf-8", errors="ignore") as f:
    reader = csv.DictReader(f)
    for row in reader:
        article_type = row.get("articleType", "").strip()
        gender = row.get("gender", "").strip()
        pid = row.get("id", "").strip()
        name = row.get("productDisplayName", "").strip()

        if article_type not in ARTICLE_TYPE_TO_CATEGORY:
            continue
        if gender not in ACCEPTED_GENDERS:
            continue
        if not pid or not name:
            continue

        image_src = os.path.join(KAGGLE_IMAGES, f"{pid}.jpg")
        if not os.path.isfile(image_src):
            continue  # some ids in styles.csv have no matching image file — skip them

        category = ARTICLE_TYPE_TO_CATEGORY[article_type]
        rows_by_category[category].append({
            "id": pid, "name": name, "gender": gender, "category": category,
            "colour": row.get("baseColour", "").strip(),
        })

# ---------------- SAMPLE, ASSIGN BRAND/PRICE/STOCK, COPY IMAGES ----------------

products = []
for category, rows in rows_by_category.items():
    random.shuffle(rows)
    chosen = rows[:MAX_PER_CATEGORY]
    lo, hi = PRICE_RANGES[category]
    for r in chosen:
        # Real Myntra listings are formatted "<Brand> <Gender> <Description...>",
        # e.g. "Puma Men Race Black Running Shoes" -> brand = "Puma".
        brand = extract_brand(r["name"])

        price = round(random.uniform(lo, hi), 2)
        stock = random.randint(*STOCK_RANGE)
        product_id = f"P{r['id']}"  # keep traceable back to the source dataset id
        image_dst_name = f"{product_id}.jpg"
        shutil.copyfile(
            os.path.join(KAGGLE_IMAGES, f"{r['id']}.jpg"),
            os.path.join(IMG_DIR, image_dst_name),
        )
        products.append({
            "id": product_id,
            "name": r["name"],
            "brand": brand,
            "category": category,
            "gender": r["gender"],
            "price": price,
            "stock": stock,
            "imagePath": f"data/images/{image_dst_name}",
        })

if not products:
    raise SystemExit(
        "No products matched. Check that kaggle_dataset/styles.csv and "
        "kaggle_dataset/images/ are the real, unzipped Kaggle files."
    )

# ---------------- ONE GENERIC "NO IMAGE" ICON (UI CHROME, NOT PRODUCT DATA) ----------------
# AdminDashboard.java falls back to this exact path for products an admin adds
# by hand with no photo. It's an empty-state icon, not a fake product image.
try:
    from PIL import Image, ImageDraw
    fallback = Image.new("RGB", (340, 240), (245, 245, 245))
    d = ImageDraw.Draw(fallback)
    d.rectangle([20, 60, 320, 190], outline=(160, 160, 160), width=3)
    d.text((110, 210), "No image", fill=(120, 120, 120))
    fallback.save(os.path.join(IMG_DIR, "placeholder.png"))
except ImportError:
    print("(Pillow not installed — skipping the generic 'No image' admin icon; "
          "pip install pillow if you want it regenerated.)")

# ---------------- WRITE products.csv ----------------

with open(os.path.join(DATA_DIR, "products.csv"), "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["id", "name", "brand", "category", "gender", "price", "stock", "imagePath"])
    for p in products:
        w.writerow([p["id"], p["name"], p["brand"], p["category"], p["gender"],
                    p["price"], p["stock"], p["imagePath"]])

# ---------------- WRITE users.csv (app accounts — not product data) ----------------

def sha256_hex(s):
    return hashlib.sha256(s.encode()).hexdigest()

users = [
    {"id": "U001", "role": "ADMIN", "name": "Store Admin", "email": "admin@flashshoes.com",
     "password": "admin123", "wallet": 0},
    {"id": "U002", "role": "CUSTOMER", "name": "Alex Johnson", "email": "alex@example.com",
     "password": "customer123", "wallet": 500},
]

with open(os.path.join(DATA_DIR, "users.csv"), "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["id", "role", "name", "email", "passwordHash", "walletBalance"])
    for u in users:
        w.writerow([u["id"], u["role"], u["name"], u["email"], sha256_hex(u["password"]), u["wallet"]])

# ---------------- WRITE flashsales.csv (store's own sale campaign — not product data) ----------------

now = datetime.now()
FMT = "%Y-%m-%dT%H:%M:%S"

sale_candidates = random.sample(products, min(8, len(products)))
flash_sales = []
for i, p in enumerate(sale_candidates):
    sale_id = f"FS{i+1}"
    discount = random.choice([15, 20, 25, 30, 40])
    limited_stock = random.randint(3, 12)
    if i < 5:
        start = now - timedelta(minutes=random.randint(1, 5))
        end = now + timedelta(minutes=random.randint(15, 90))
        status = "ACTIVE"
    else:
        start = now + timedelta(minutes=random.randint(2, 10))
        end = start + timedelta(hours=2)
        status = "UPCOMING"
    flash_sales.append([sale_id, p["id"], discount, start.strftime(FMT), end.strftime(FMT),
                         limited_stock, status])

with open(os.path.join(DATA_DIR, "flashsales.csv"), "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["id", "productId", "discountPercent", "startTime", "endTime", "limitedStock", "status"])
    for row in flash_sales:
        w.writerow(row)

# ---------------- WRITE empty orders.csv ----------------

with open(os.path.join(DATA_DIR, "orders.csv"), "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["orderId", "customerId", "productId", "quantity", "size", "priceAtPurchase", "timestamp", "status", "paymentMethod"])

print(f"Imported {len(products)} real products (with real photos) across "
      f"{len(rows_by_category)} categories, {len(users)} seed users, "
      f"{len(flash_sales)} flash sales.")
for cat, rows in rows_by_category.items():
    print(f"  {cat}: {min(len(rows), MAX_PER_CATEGORY)} products used (of {len(rows)} available)")