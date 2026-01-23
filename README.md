# MAD25_P03_CA_Garence
Single-player “Wack-a-Mole” game

“This is a development done to satisfy the 
assignment portion for module “Mobile App development” under 
Ngee Ann Polytechnic for AY24/25”

---

# Tools used:
- **ChatGPT (OpenAI)** – used as a support tool for clarifying implementation approach and small code fragments.
- **Perplexity AI** – used to cross-check general understanding of Room/Compose concepts and confirm typical query patterns.

# Where AI was used
## AI was used in two main areas:

1. **Database (Room) setup guidance**
   - Multi-user storage (User + Score tables)
   - DAO queries
   - Basic password hashing flow

2. **Leaderboard logic**
   - Show each user’s personal best score
   - Highlight the current user visually (different background color)

---

# Example prompts/questions asked:

“How do I set up Room with a UserEntity and ScoreEntity, and sign in/sign up using DAO queries?”

“How do I store a user’s score at game end and retrieve the personal best using Room?”

“How do I write a Room query to show a leaderboard of best score per user, including users with no scores showing as 0?”

“How do I highlight the current user row in the leaderboard UI (different background color) in Compose?”

# Code/design influenced by AI:
A) Sign-in / Sign-up validation + database persistence

What changed: Added input checks (prevent blank sign-in/sign-up), used Room DAO to insert/load users, and used a simple hash for stored passwords/PIN.

Before (simplified):
Button(onClick = { onSignInSuccess() }) { Text("Sign In") }
Button(onClick = { onSignUpSuccess() }) { Text("Sign Up") }


After (simplified):
if (u.isBlank() || p.isBlank()) {
    message = "Please fill in username and password."
    return@launch
}
val hashed = hashPassword(p)
val user = db.userDao().signIn(u, hashed)

---

Why refactored:
To meet the advanced requirement of persistent multi-user support (store users, authenticate on next app launch, and set current user on sign-in). 

Key takeaway:
Room + DAO provides reliable persistence across app restarts, and basic validation prevents invalid accounts and confusing UI behaviour.

---

B) Leaderboard: best score per user (including “0” when no games played)

What changed: Instead of only showing players who already have scores, the leaderboard uses a query pattern that can return all users, and treats “no score yet” as 0.

Before (common issue):
Leaderboard only displayed users with score rows (new accounts missing).
UI had type inference/unresolved references due to missing data model/query mapping.

After (concept):
Use a result model like LeaderboardRow(userId, username, bestScore)
Use a LEFT JOIN + MAX + COALESCE approach so users with no scores appear as 0.

---

Why refactored:
Requirement expects “Personal best score against other users,” and it’s clearer when every created account appears (even before playing), with best score defaulting to 0. 


Key takeaway:
Designing the query correctly (grouping + left join + default values) simplifies UI logic and avoids edge cases like “missing users.”

---

# Lessons learnt
Room schema design matters: clean separation of users and scores makes it easy to track multiple attempts per user and compute personal best.
DAO queries drive UI stability: a good query (returning all required fields in the correct shape) prevents many Compose/typing errors.
