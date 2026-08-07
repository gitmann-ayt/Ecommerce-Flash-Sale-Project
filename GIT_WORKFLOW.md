# GIT_WORKFLOW — How to commit, push, and structure this repo

This covers everything the rubric's "IDE & Version Control" and "Ethics &
Teamwork" rows check for: a real, balanced, readable commit history from all
3 members, not one mass upload at the end.

## 1. One-time setup (whoever creates the repo does this)

1. On GitHub, create a **new empty repository** (no README/.gitignore/license
   — you already have all of those). Name it something like
   `flashshoesfx-cs212` or `ecommerce-flash-sale-platform`. Keep it **Public**
   or **Private-with-instructor-added-as-collaborator** — check which your
   course requires.
2. On your machine, inside the project folder (the one containing `pom.xml`):

   ```bash
   git init
   git add .
   git commit -m "Initial commit: baseline FlashShoesFX project (models, services, UI, data, UML, docs)"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<repo-name>.git
   git push -u origin main
   ```

3. Add your two teammates as **Collaborators**: repo → Settings → Collaborators
   → Add people. They accept the invite by email/GitHub notification.
4. Every teammate then clones it locally instead of re-initializing:

   ```bash
   git clone https://github.com/<your-username>/<repo-name>.git
   cd <repo-name>
   ```

5. **Before anyone commits**, each person sets their own identity so commits
   are correctly attributed to them (this is what the instructor checks —
   don't let one person push everyone's work):

   ```bash
   git config user.name "Your Real Name"
   git config user.email "your.email@used.on.github"
   ```

## 2. Folder structure to commit (already matches what's in this zip)

```
<repo-root>/
├── .gitignore
├── README.md
├── PROJECT_NOTES.md            # rubric-to-code map (reference/viva notes)
├── GIT_WORKFLOW.md             # this file
├── ProjectReport.pdf           # ⭐ required deliverable — export from ProjectReport.docx
├── GitHubLink.txt              # ⭐ required deliverable — plain text, one line: repo URL
├── pom.xml
├── generate_data.py
├── uml/
│   ├── uml.dot                 # Graphviz source (editable)
│   ├── uml.png                 # ⭐ required deliverable (UML Class Diagram, image form)
│   └── uml.pdf                 # ⭐ required deliverable (UML Class Diagram, PDF form)
├── data/
│   ├── products.csv, users.csv, flashsales.csv, orders.csv
│   └── images/*.png
└── src/
    └── main/
        ├── java/com/flashshoes/{model,service,ui}/*.java
        └── resources/com/flashshoes/css/app.css
```

Notes:
- `target/` and `.idea/` are excluded via `.gitignore` — never commit build
  output or personal IDE settings.
- The rubric only asks for **one** of the UML diagram files (PDF or image) —
  committing both is fine and safer.
- You only need to submit **one** `uml.dot`→exported pair; if your group
  prefers draw.io/StarUML instead of the provided Graphviz one, that's fine
  too — just replace `uml/uml.png`/`uml/uml.pdf` and keep the rubric-required
  file present.

## 3. Branching strategy (keep it simple for a 3-person, 1-week project)

Don't overengineer this — a single long-lived `main` branch plus short-lived
personal feature branches works well at this scale and still produces a
readable, attributable history:

```bash
git checkout -b feature/<yourname>-<short-topic>   # e.g. feature/aytesam-checkout-validation
# ... make changes, commit as you go (see §4) ...
git push -u origin feature/<yourname>-<short-topic>
# then open a Pull Request on GitHub into main, and merge it (self-review is fine for a class project)
```

This alone visibly demonstrates "professional" version control to the
instructor — a clean PR list is easy evidence during the viva.

## 4. Commit message convention

Small, frequent, descriptive commits — not one giant commit per person.
Prefix with the area you touched:

```
model: add validation to Order.calculateTotal
service: fix race condition window in InventoryManager.releaseStock
ui: wire app.css theme into MainApp scenes
docs: fill in Challenges Faced section of ProjectReport
data: regenerate products.csv with 60 items
```

Aim for **at least 4-6 commits per person over the week** — vague messages
like "update" or "fix stuff", or one single end-of-week commit per person,
are explicitly penalized by the rubric ("commits are infrequent or vague").

## 5. Suggested one-week schedule

Adjust dates to your actual deadline — the important part is the *shape*:
spread out, all 3 people touching the repo most days, integration happening
mid-week (not the night before submission).

| Day | Person A (Core & Concurrency) | Person B (Customer UI) | Person C (Admin UI & Data) |
|---|---|---|---|
| **Day 1** | Review/clean up `model/` package; re-verify `InventoryManager` locking with `SmokeTest`; commit any fixes | Review `LoginScreen`/`RegisterScreen`; add missing input validation + error messages | Review `FileManager`/CSV schema; regenerate fresh `data/*.csv` via `generate_data.py`; commit |
| **Day 2** | Add 2-3 more unit-style checks to `SmokeTest` (e.g. wallet payment failure path, order status transitions) | Polish `CustomerDashboard` cart/checkout flow; verify `app.css` renders correctly on your machine | Polish `AdminDashboard`; verify the Concurrency Demo tab still works after any InventoryManager changes |
| **Day 3** | Pair with Person C: confirm `DataStore` persistence round-trips correctly after a crash/restart | Pair with Person A: add proper error dialogs when checkout fails (out of stock, payment declined) | Add/verify product image handling and any remaining CSV edge cases |
| **Day 4** | Start filling `PROJECT_NOTES.md` → `ProjectReport.docx` §3 (Concurrency) with real screenshots/output | Fill `ProjectReport.docx` §5 (UI) with real screenshots | Fill `ProjectReport.docx` §4 (File Handling) |
| **Day 5** | Group session: walk through `uml/uml.dot`, correct anything that drifted from the actual code, re-render | Group session: draft §6 (UML evolution) and §7 (Challenges) together — these need to reflect what *actually* happened | Group session: fill §8 (Task Division) honestly, matching the commit history |
| **Day 6** | Full run-through rehearsal for viva: run `SmokeTest`, then demo Concurrency Demo tab live | Full run-through: demo customer flow (browse → cart → checkout → order) | Full run-through: demo admin flow (add product, start flash sale, view orders) |
| **Day 7** | Export `ProjectReport.docx` → PDF, final proofread as a group. Create `GitHubLink.txt`. Confirm `.zip` deliverable structure matches §2 above. **Submit early and grab a viva slot**, then create the Google Calendar event and invite all 3 members + instructor. | | |

## 6. Producing the required text/PDF files at the end

```bash
# GitHubLink.txt — literally just the URL, nothing else
echo "https://github.com/<your-username>/<repo-name>" > GitHubLink.txt

# ProjectReport.pdf — after filling in every [ placeholder ] in ProjectReport.docx:
#   Word/LibreOffice: File -> Export As -> PDF
git add GitHubLink.txt ProjectReport.pdf ProjectReport.docx
git commit -m "docs: finalize ProjectReport and GitHubLink for submission"
git push
```

## 7. Final submission zip

The course wants **one .zip** containing: source code, UML diagram,
`ProjectReport.pdf`, `GitHubLink.txt`. From your repo root:

```bash
cd ..
zip -r FinalSubmission_GroupXX.zip <repo-folder-name> -x "<repo-folder-name>/target/*" -x "<repo-folder-name>/.git/*" -x "<repo-folder-name>/.idea/*"
```

(Replace `GroupXX` with your actual group number/name per the course page.)

## 8. Viva reminder (from the assignment sheet, don't skip this)

Submit early, get a viva slot early, then **create a Google Calendar event
for the viva slot and invite every group member** — the assignment sheet
calls this out explicitly as a requirement, separate from the zip submission.
