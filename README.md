# Smart Expense Splitter

A Spring Boot backend that splits group expenses and settles debts using a **greedy debt-minimization algorithm**. Enter who paid what, and the system calculates each person's net balance and generates the smallest practical set of settlement transactions — instead of everyone paying everyone.

**Live demo:** https://smart-expense-splitter-4011.onrender.com
*(Free-tier hosting — first load can take 30–50s if the server was asleep.)*

---

## The Problem

When a group shares expenses, tracking who owes whom gets messy fast.

Example:
- A pays ₹800 for the group
- B pays ₹400
- C pays ₹200

Everyone's share is ₹466.67. Naively, this could mean many small payments criss-crossing between everyone. This project calculates each person's **net balance** and works out a much smaller set of transactions that settles everyone up.

---

## Screenshots

<img width="1920" height="1080" alt="homepage" src="https://github.com/user-attachments/assets/7694cb96-264d-49e0-b28e-de74a5a0ca39" />
<img width="1920" height="1080" alt="adding user and group" src="https://github.com/user-attachments/assets/c430a453-6f51-42ff-bf90-097638322881" />
<img width="1920" height="1080" alt="tripname" src="https://github.com/user-attachments/assets/2e98aac7-b390-451a-93ae-020fe8ca34d7" />
<img width="1920" height="1080" alt="expenses  " src="https://github.com/user-attachments/assets/3eaf4662-8788-4408-ac56-ec936ada9627" />
<img width="1920" height="1080" alt="amount" src="https://github.com/user-attachments/assets/7c84e0b7-d3fe-45e5-bf85-8ad47f68d1ce" />


<!-- Add screenshots here, e.g.: -->
<!-- ![Add People](screenshots/add-people.png) -->
<!-- ![Net Balances](screenshots/balances.png) -->
<!-- ![Settlement Result](screenshots/settlement.png) -->

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot |
| Data Access | Spring Data JPA |
| Database | H2 (file-based, persists across restarts) |
| Build Tool | Maven |
| API Docs | springdoc-openapi (Swagger UI) |
| Frontend | Plain HTML/CSS/JS (served as a Spring Boot static resource) |
| Deployment | Docker on Render |

---

## Architecture

Standard layered architecture:

```
Client (Browser / Postman)
        ↓
   Controller      → handles HTTP requests/responses
        ↓
   Service         → business logic (including the settlement algorithm)
        ↓
   Repository      → Spring Data JPA, talks to the database
        ↓
   H2 Database
```

Package structure (`com.settlewise.app`):

```
model        → User, Group, Expense (JPA entities)
repository   → UserRepository, GroupRepository, ExpenseRepository
service      → UserService, GroupService, ExpenseService, SettlementService
controller   → UserController, GroupController, ExpenseController, SettlementController
dto          → response/request objects
```

**Key relationships:**
- `User` ↔ `Group` — Many-to-Many (a user can belong to multiple groups, a group has multiple users)
- `Expense` → `User` / `Group` — Many-to-One (an expense belongs to one group and has one payer)

---

## The Algorithm: Greedy Debt Minimization

### Problem it's solving

This is an implementation of the **Optimal Account Balancing** problem (related to LeetCode 465). Given a list of who-paid-what, minimize the number of transactions needed to settle all debts.

> **Note on accuracy:** The truly minimum number of transactions is an NP-hard problem in the general case — solving it exactly requires trying different subsets of people, which is exponential. This project uses a **greedy heuristic** (largest debtor ↔ largest creditor) which produces a small, practical number of transactions efficiently, but is **not mathematically guaranteed to be the absolute minimum** in every case. This is a deliberate, documented trade-off for a project of this scope.

### Step-by-step logic

**1. Calculate net balance per person:**
```
Net Balance = Total Paid − Total Fair Share
```
- Positive → this person is owed money (creditor)
- Negative → this person owes money (debtor)
- Zero → already settled

**2. Split people into two groups:**
- Debtors (negative balance)
- Creditors (positive balance)

**3. Greedily settle the largest amounts first:**
```
while both heaps are non-empty:
    debtor   = pop the person who owes the most
    creditor = pop the person who is owed the most

    amount = min(|debtor's debt|, creditor's credit)

    record transaction: debtor pays creditor `amount`

    update debtor's remaining debt
    update creditor's remaining credit

    if debtor still owes something → push back onto debtor heap
    if creditor is still owed something → push back onto creditor heap
```

**4. Repeat until both heaps are empty.** Every remaining balance is now zero.

### Why PriorityQueue (Max-Heap)?

Java's `PriorityQueue` is a **min-heap by default** — it always gives you the *smallest* element first. Since we need the *largest* debtor and *largest* creditor each round, we invert this using a custom comparator (`(a, b) -> b.getBalance() - a.getBalance()`), which turns it into a max-heap.

**Why not just use an ArrayList and scan for the max each time?**
- With an ArrayList, finding the max is O(n) every single round, and there can be up to n rounds → O(n²) overall.
- With a heap, extracting the max is O(log n), giving O(n log n) overall — noticeably faster for larger groups.

### Complexity

| Operation | Complexity | Why |
|---|---|---|
| Building the heaps | O(n log n) | Inserting n people, each insert is O(log n) |
| Settlement loop | O(n log n) | At most n−1 transactions, each involves O(log n) heap operations |
| **Total time** | **O(n log n)** | n = number of people with non-zero balance |
| **Space** | **O(n)** | Storing balances and heap entries |

### Worked example

```
A: -₹500   (owes ₹500)
B: +₹300   (is owed ₹300)
C: +₹200   (is owed ₹200)
```

Round 1: largest debtor = A (-500), largest creditor = B (+300)
→ A pays B ₹300. A's balance becomes -200. B is now settled (removed).

Round 2: largest debtor = A (-200), largest creditor = C (+200)
→ A pays C ₹200. Both now settled.

**Result: 2 transactions** (`A → B: ₹300`, `A → C: ₹200`) instead of every possible pairwise payment.

---

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/users` | Create a user |
| POST | `/api/groups` | Create a group |
| POST | `/api/groups/{groupId}/users/{userId}` | Add a user to a group |
| POST | `/api/expenses` | Log an expense |
| GET | `/api/settlements/{groupId}/balances` | Get raw net balances per person |
| GET | `/api/settlements/{groupId}` | Get minimized settlement transactions |

Full interactive API docs available via Swagger UI at `/swagger-ui.html` when running locally or on the deployed instance.

---

## Running Locally

**Prerequisites:** JDK 21, Maven (or use the included Maven wrapper), IntelliJ IDEA (optional).

```bash
# Clone the repo
git clone https://github.com/jananiramesh495-ui/smart-expense-splitter.git
cd smart-expense-splitter

# Run with Maven wrapper
./mvnw spring-boot:run
```

Then open:
- App UI: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/settlewisedb`
  - Username: `sa`, Password: *(blank)*

---

## Deployment

Deployed on **Render** using a multi-stage `Dockerfile` (Render has no native Java runtime, so the app is containerized). Data persists via file-based H2 storage on disk.

---

## Known Scope Decisions

These are deliberate trade-offs made to keep the project focused and achievable within the timeline — not oversights:

- **Equal-split expenses only** — no custom per-person share amounts (e.g. "A pays more than B for the same expense").
- **No authentication/security layer** — out of scope; the focus is the algorithm and core CRUD + settlement flow.
- **"Mark as paid" is UI-only** — visual confirmation, not persisted to the database.
- **Greedy, not provably optimal** — see the algorithm section above.

---

## Tech Notes

- Uses **file-based H2** (`jdbc:h2:file:./data/settlewisedb`), not in-memory H2, so data survives server restarts. (In-memory H2 uses `jdbc:h2:mem:...` and wipes on every restart — deliberately avoided here.)
- H2 web console enabled at `/h2-console` for inspecting the database directly during development.
- `@Table(name = "users")` is required on the `User` entity because `USER` is a reserved SQL keyword in H2 — using it as a table name causes a syntax conflict.
- CORS is enabled globally for `/api/**` so the frontend (served from the same origin, but useful for local dev against a separate frontend) can call the backend freely.
