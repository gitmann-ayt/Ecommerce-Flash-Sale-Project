"""
Generates the seed dataset for FlashShoesFX: data/products.csv, data/users.csv,
data/flashsales.csv, data/orders.csv, and one placeholder image per product.

NOTE ON THE DATASET: Kaggle (the source of the Myntra Fashion Product Images
Dataset originally recommended) is not reachable from this build environment's
network allowlist, so this script generates a realistic-but-synthetic shoe
catalogue instead — real brand/model naming conventions, real category/price
patterns, but not scraped data or real product photos. See README.md for how
to swap in the real Kaggle dataset later if you want authentic photos.
"""
import csv
import hashlib
import os
import random
from datetime import datetime, timedelta

random.seed(42)

BASE = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE, "data")
IMG_DIR = os.path.join(DATA_DIR, "images")
os.makedirs(IMG_DIR, exist_ok=True)

# ---------------- CATALOGUE GENERATION ----------------

BRANDS_BY_CATEGORY = {
    "Running":  ["Nike", "Adidas", "ASICS", "New Balance", "Brooks", "Puma"],
    "Casual":   ["Converse", "Vans", "Puma", "Skechers", "Adidas", "Nike"],
    "Formal":   ["Clarks", "Bata", "Woodland", "Hush Puppies", "Florsheim"],
    "Sports":   ["Nike", "Adidas", "Puma", "Reebok", "Under Armour", "ASICS"],
    "Sandals":  ["Woodland", "Bata", "Crocs", "Skechers", "Puma"],
}

NAME_PARTS_BY_CATEGORY = {
    "Running":  ["Air Zoom Pegasus", "Ultraboost", "Gel-Kayano", "Fresh Foam", "Ghost", "RS-X Runner"],
    "Casual":   ["Chuck Taylor All Star", "Old Skool", "Suede Classic", "Cali Street", "Cloudfoam Pure", "Court Vision"],
    "Formal":   ["Oxford Leather", "Derby Classic", "Monk Strap", "Cap-Toe Brogue", "Slip-On Loafer"],
    "Sports":   ["Phantom Trainer", "Speedcat", "Flexweave Trainer", "HOVR Rise", "Court Slide"],
    "Sandals":  ["Comfort Slide", "Trail Sandal", "Classic Clog", "Sport Sandal", "Flip Flow"],
}

COLORS = ["Black", "White", "Navy", "Grey", "Red", "Blue", "Olive", "Tan", "Charcoal", "Maroon"]
GENDERS = ["Men", "Women", "Unisex"]

PRICE_RANGES = {
    "Running": (55, 140),
    "Casual":  (30, 95),
    "Formal":  (55, 160),
    "Sports":  (40, 120),
    "Sandals": (18, 55),
}

STOCK_RANGE = (8, 60)

CATEGORY_COLORS_RGB = {
    "Running":  (231, 76, 60),
    "Casual":   (52, 152, 219),
    "Formal":   (44, 62, 80),
    "Sports":   (39, 174, 96),
    "Sandals":  (243, 156, 18),
}

products = []
pid = 1
for category, name_parts in NAME_PARTS_BY_CATEGORY.items():
    brands = BRANDS_BY_CATEGORY[category]
    lo, hi = PRICE_RANGES[category]
    # ~12 products per category => ~60 total
    for _ in range(12):
        brand = random.choice(brands)
        base_name = random.choice(name_parts)
        color = random.choice(COLORS)
        gender = random.choice(GENDERS)
        name = f"{brand} {base_name} ({color})"
        price = round(random.uniform(lo, hi), 2)
        stock = random.randint(*STOCK_RANGE)
        product_id = f"P{pid:03d}"
        image_path = f"data/images/{product_id}.png"
        products.append({
            "id": product_id, "name": name, "brand": brand, "category": category,
            "gender": gender, "price": price, "stock": stock, "imagePath": image_path,
            "color": color,
        })
        pid += 1

# ---------------- PLACEHOLDER IMAGE GENERATION ----------------

from PIL import Image, ImageDraw, ImageFont

def draw_shoe_silhouette(draw, x, y, w, h, color):
    """A very simple stylised sneaker-profile silhouette, not a real photo."""
    # sole
    draw.rounded_rectangle([x, y + h * 0.72, x + w, y + h * 0.85], radius=6, fill=(30, 30, 30))
    # body (rough shoe blob)
    body = [
        (x + w * 0.05, y + h * 0.72),
        (x + w * 0.05, y + h * 0.55),
        (x + w * 0.20, y + h * 0.40),
        (x + w * 0.55, y + h * 0.32),
        (x + w * 0.80, y + h * 0.38),
        (x + w * 0.95, y + h * 0.55),
        (x + w * 0.95, y + h * 0.72),
    ]
    draw.polygon(body, fill=color)
    # laces area
    for i in range(4):
        lx = x + w * (0.35 + i * 0.08)
        draw.line([(lx, y + h * 0.40), (lx + w * 0.05, y + h * 0.50)], fill=(255, 255, 255), width=2)

def make_placeholder(product):
    W, H = 340, 240
    img = Image.new("RGB", (W, H), (250, 250, 250))
    draw = ImageDraw.Draw(img)
    color = CATEGORY_COLORS_RGB.get(product["category"], (100, 100, 100))
    draw.rectangle([0, 0, W, H], fill=(255, 255, 255))
    draw.rectangle([0, 0, W, 28], fill=color)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 14)
        small_font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 12)
    except Exception:
        font = ImageFont.load_default()
        small_font = font
    draw.text((8, 6), product["category"].upper(), fill=(255, 255, 255), font=font)
    draw_shoe_silhouette(draw, 20, 40, W - 40, H - 90, color)
    draw.text((10, H - 42), product["brand"], fill=(20, 20, 20), font=font)
    draw.text((10, H - 22), product["name"].split("(")[0].strip(), fill=(90, 90, 90), font=small_font)
    img.save(os.path.join(IMG_DIR, f"{product['id']}.png"))

for p in products:
    make_placeholder(p)

# generic fallback used by admin-added products
fallback = Image.new("RGB", (340, 240), (245, 245, 245))
d = ImageDraw.Draw(fallback)
draw_shoe_silhouette(d, 20, 40, 300, 150, (150, 150, 150))
d.text((10, 210), "No image", fill=(120, 120, 120))
fallback.save(os.path.join(IMG_DIR, "placeholder.png"))

# ---------------- WRITE products.csv ----------------

with open(os.path.join(DATA_DIR, "products.csv"), "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["id", "name", "brand", "category", "gender", "price", "stock", "imagePath"])
    for p in products:
        w.writerow([p["id"], p["name"], p["brand"], p["category"], p["gender"],
                    p["price"], p["stock"], p["imagePath"]])

# ---------------- WRITE users.csv ----------------

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

# ---------------- WRITE flashsales.csv ----------------

now = datetime.now()
FMT = "%Y-%m-%dT%H:%M:%S"

# pick a handful of products across categories to be on flash sale right now / soon
sale_candidates = random.sample(products, 8)
flash_sales = []
for i, p in enumerate(sale_candidates):
    sale_id = f"FS{i+1}"
    discount = random.choice([15, 20, 25, 30, 40])
    limited_stock = random.randint(3, 12)
    if i < 5:
        # already running, ends in 5-90 minutes so a demo mid-run shows real countdowns
        start = now - timedelta(minutes=random.randint(1, 5))
        end = now + timedelta(minutes=random.randint(15, 90))
        status = "ACTIVE"
    else:
        # upcoming, starts a few minutes from now
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
    w.writerow(["orderId", "customerId", "productId", "quantity", "priceAtPurchase", "timestamp", "status", "paymentMethod"])

print(f"Generated {len(products)} products, {len(users)} users, {len(flash_sales)} flash sales.")
