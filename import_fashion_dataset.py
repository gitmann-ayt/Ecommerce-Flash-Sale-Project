"""
Converts the Kaggle "Fashion Product Images (Small)" dataset
(https://www.kaggle.com/datasets/paramaggarwal/fashion-product-images-small)
into FlashShoesFX's data/ format: data/products.csv, data/reviews.csv,
data/flashsales.csv, data/orders.csv, and copies real product photos into
data/images/.

HOW TO USE
----------
1. Download the dataset from Kaggle and unzip it. You should end up with a
   `styles.csv` file and an `images/` folder full of `<id>.jpg` files.
2. Run:
       python import_fashion_dataset.py --styles /path/to/styles.csv --images /path/to/images --limit 300
3. Re-run the app — data/products.csv, data/reviews.csv etc. are now backed
   by real names, brands, categories and real photos instead of the
   synthetic generate_data.py catalogue.

NOTES
-----
- The dataset has NO price column, so prices are synthesised from realistic
  ranges per articleType/category (same approach generate_data.py used).
- The dataset does not have a "Khusa" (traditional South Asian footwear) tag
  specifically — Myntra's own taxonomy doesn't include it. Categories used
  here come straight from the dataset's own `articleType` field (Casual
  Shoes, Formal Shoes, Sports Shoes, Sandals, Heels, Flip Flops, Flats,
  etc.), which maps cleanly onto category chips like Casual/Formal/Sports.
- --limit caps how many products get imported (sampled evenly across
  articleTypes) so the JavaFX app stays fast to browse; raise it if you want
  the full ~44k catalogue (not recommended for a demo app).
"""
import argparse
import csv
import hashlib
import os
import random
import shutil
from datetime import datetime, timedelta

random.seed(42)

BASE = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE, "data")
IMG_DIR = os.path.join(DATA_DIR, "images")

# Rough real-world price bands per articleType (USD). Anything not listed
# falls back to a sane default range based on masterCategory.
PRICE_RANGES = {
    "Casual Shoes": (30, 90), "Sports Shoes": (40, 130), "Formal Shoes": (55, 150),
    "Sandals": (18, 55), "Sports Sandals": (20, 60), "Flip Flops": (10, 30),
    "Heels": (25, 90), "Flats": (20, 60), "Boots": (45, 140),
    "Tshirts": (12, 35), "Shirts": (18, 55), "Jeans": (25, 75),
    "Jackets": (35, 120), "Trousers": (20, 60), "Shorts": (15, 40),
    "Watches": (30, 250), "Handbags": (25, 150), "Belts": (10, 40),
    "Sunglasses": (15, 90), "Wallets": (10, 45), "Backpacks": (20, 80),
}
DEFAULT_RANGE_BY_MASTER = {
    "Footwear": (25, 100), "Apparel": (15, 60), "Accessories": (10, 80),
}

SIZE_SETS = {
    "Footwear": ["6", "7", "8", "9", "10", "11"],
    "Apparel": ["S", "M", "L", "XL", "XXL"],
    "Accessories": ["One Size"],
}

REVIEWER_NAMES = [
    "Ayesha K.", "Bilal R.", "Sara M.", "Hamza T.", "Fatima A.", "Usman S.",
    "Zainab N.", "Ali H.", "Mahnoor I.", "Omar F.", "Hira W.", "Danish Q.",
    "Sana J.", "Faisal B.", "Rimsha L.", "Talha Z.",
]
POSITIVE_COMMENTS = [
    "Great quality for the price, fits true to size.",
    "Really happy with this — looks even better in person.",
    "Comfortable and stylish, would buy again.",
    "Exactly as described, fast to get used to and looks great.",
    "Good value, matched the photos well.",
]
MIXED_COMMENTS = [
    "Decent, but sizing ran a little small for me.",
    "Good product overall, delivery took a bit long.",
    "Nice look, material could be a bit better for the price.",
]

def synth_price(article_type, master_category):
    lo, hi = PRICE_RANGES.get(article_type, DEFAULT_RANGE_BY_MASTER.get(master_category, (15, 60)))
    return round(random.uniform(lo, hi), 2)

def synth_sizes(master_category):
    return SIZE_SETS.get(master_category, ["One Size"])

def synth_description(row):
    return (f"{row['productDisplayName']} — a {row['baseColour'].lower()} "
            f"{row['articleType'].lower()} from the {row['season']} {row['year']} collection, "
            f"great for {row['usage'].lower()} wear.")

def sha256_hex(s):
    return hashlib.sha256(s.encode()).hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--styles", required=True, help="Path to styles.csv from the Kaggle dataset")
    ap.add_argument("--images", required=True, help="Path to the images/ folder from the Kaggle dataset")
    ap.add_argument("--limit", type=int, default=300, help="Max number of products to import")
    ap.add_argument("--categories", default="Footwear,Apparel,Accessories",
                     help="Comma-separated masterCategory values to include")
    args = ap.parse_args()

    wanted_master = set(c.strip() for c in args.categories.split(","))
    os.makedirs(IMG_DIR, exist_ok=True)

    # ---------------- read + filter styles.csv ----------------
    rows_by_type = {}
    with open(args.styles, newline="", encoding="utf-8", errors="ignore") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row.get("masterCategory") not in wanted_master:
                continue
            img_path = os.path.join(args.images, f"{row['id']}.jpg")
            if not os.path.exists(img_path):
                continue
            rows_by_type.setdefault(row["articleType"], []).append(row)

    if not rows_by_type:
        print("No matching rows found — check --styles/--images paths and --categories.")
        return

    # sample roughly evenly across articleTypes up to --limit
    types = list(rows_by_type.keys())
    per_type = max(1, args.limit // len(types))
    selected = []
    for t in types:
        pool = rows_by_type[t]
        random.shuffle(pool)
        selected.extend(pool[:per_type])
    random.shuffle(selected)
    selected = selected[:args.limit]

    # ---------------- build products + copy images ----------------
    products = []
    for row in selected:
        pid = f"P{row['id']}"
        image_dest = os.path.join(IMG_DIR, f"{pid}.jpg")
        shutil.copyfile(os.path.join(args.images, f"{row['id']}.jpg"), image_dest)

        name = row["productDisplayName"].strip()
        brand = name.split(" ")[0] if name else "Generic"
        category = row["articleType"]
        gender = row.get("gender", "Unisex")
        price = synth_price(category, row["masterCategory"])
        stock = random.randint(8, 60)
        sizes = synth_sizes(row["masterCategory"])
        description = synth_description(row)

        products.append({
            "id": pid, "name": name, "brand": brand, "category": category,
            "gender": gender, "price": price, "stock": stock,
            "imagePath": f"data/images/{pid}.jpg",
            "sizes": "|".join(sizes), "description": description,
        })

    # ---------------- write products.csv ----------------
    with open(os.path.join(DATA_DIR, "products.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "name", "brand", "category", "gender", "price", "stock", "imagePath", "sizes", "description"])
        for p in products:
            w.writerow([p["id"], p["name"], p["brand"], p["category"], p["gender"],
                        p["price"], p["stock"], p["imagePath"], p["sizes"], p["description"]])

    # ---------------- write reviews.csv (fake, seeded) ----------------
    reviews = []
    rid = 1
    for p in products:
        n = random.randint(2, 6)
        for _ in range(n):
            rating = random.choices([5, 4, 3, 2], weights=[45, 35, 15, 5])[0]
            comment = random.choice(POSITIVE_COMMENTS if rating >= 4 else MIXED_COMMENTS)
            date = (datetime.now() - timedelta(days=random.randint(1, 180))).date().isoformat()
            reviews.append([f"R{rid}", p["id"], random.choice(REVIEWER_NAMES), rating, comment, date])
            rid += 1

    with open(os.path.join(DATA_DIR, "reviews.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["reviewId", "productId", "reviewerName", "rating", "comment", "date"])
        w.writerows(reviews)

    # ---------------- write flashsales.csv ----------------
    now = datetime.now()
    FMT = "%Y-%m-%dT%H:%M:%S"
    sale_candidates = random.sample(products, min(8, len(products)))
    flash_sales = []
    for i, p in enumerate(sale_candidates):
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
        flash_sales.append([f"FS{i+1}", p["id"], discount, start.strftime(FMT), end.strftime(FMT), limited_stock, status])

    with open(os.path.join(DATA_DIR, "flashsales.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "productId", "discountPercent", "startTime", "endTime", "limitedStock", "status"])
        w.writerows(flash_sales)

    # ---------------- reset orders.csv ----------------
    with open(os.path.join(DATA_DIR, "orders.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["orderId", "customerId", "productId", "quantity", "priceAtPurchase", "timestamp", "status", "paymentMethod", "size"])

    # ---------------- ensure users.csv exists (don't clobber real accounts) ----------------
    users_path = os.path.join(DATA_DIR, "users.csv")
    if not os.path.exists(users_path):
        with open(users_path, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["id", "role", "name", "email", "passwordHash", "walletBalance"])
            w.writerow(["U001", "ADMIN", "Store Admin", "admin@flashshoes.com", sha256_hex("admin123"), 0])
            w.writerow(["U002", "CUSTOMER", "Alex Johnson", "alex@example.com", sha256_hex("customer123"), 500])

    print(f"Imported {len(products)} real products across {len(types)} categories, "
          f"{len(reviews)} fake reviews, {len(flash_sales)} flash sales.")
    print("Categories found:", ", ".join(sorted(rows_by_type.keys())))


if __name__ == "__main__":
    main()
