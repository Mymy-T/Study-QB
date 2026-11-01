# Play Console — listing copy and form answers

Everything here is ready to paste into Play Console. Character limits are noted where Google enforces them.

---

## Main store listing

**Path:** Play Console → your app → Grow users → Store presence → Main store listing

### App name *(30 char limit)*

```
Study QB
```

### Short description *(80 char limit — this is 64)*

```
Quizbowl study cards with spaced repetition and buzzer practice.
```

### Full description *(4000 char limit)*

```
Study QB is a focused study tool for quizbowl players. Build a deck of answer lines, learn the clues that actually come up, and drill them with spaced repetition or live buzzer practice.

FIND THE CLUES THAT MATTER

Enter an answer line and Study QB pulls real quizbowl questions for it from qbreader.org, then summarizes the clues that appear most often. Instead of rereading whole packets, you get the handful of facts that distinguish that answer. Filter by category and difficulty to match the tournaments you actually play. A suggestion button pulls from category frequency lists and proposes answer lines you don't have a card for yet.

BUILD A DECK YOU CONTROL

Save any analysis straight to a card, or write your own. Each card holds an answer line, a category, color coding, your personal clue notes, and a details section. Search, filter by category, sort, and star the cards you want to revisit.

TWO WAYS TO REVIEW

Classic mode shows your clue notes and lets you reveal the answer when ready. Turn on gradual clue reveal to work through hints one at a time, the way a real question unfolds. Rate each card Again, Hard, Good, or Easy and the FSRS spaced-repetition scheduler decides when you next see it.

Buzz In mode simulates a tossup. The question streams word by word, you buzz when you know it, type your answer against a countdown, and get scored the way you would at a tournament — including negs for early incorrect buzzes.

TRACK YOUR PROGRESS

See lifetime points and accuracy across sessions, plus per-card performance showing how often you have seen each answer, how often you got it, and how early in the question you tend to buzz.

STUDY REMINDERS

Set daily reminders on the weekdays you choose, or a recurring interval, so review time doesn't slip.

PRIVACY

No accounts and no sign-in. Your cards and statistics stay on your device. The Analyze and Buzz In features send your query text and card clue notes to a language-model service to generate results — see the privacy policy for details. No ads, no analytics, no tracking.
```

### Graphics checklist

| Asset | Spec | Status |
|---|---|---|
| App icon | 512×512 PNG, 32-bit | **Needed** — currently the stock Android Studio icon |
| Feature graphic | 1024×500 PNG or JPEG | **Needed** — a plain colored banner with "Study QB" is acceptable |
| Phone screenshots | 2–8, PNG/JPEG, 16:9 or 9:16 | Supplied in `docs/screenshots/` (1080×2400) |

---

## Data safety form

**Path:** Play Console → Policy and programs → App content → Data safety

The console's exact wording shifts between revisions, so these are given as substance first. Map each to whichever label the form currently shows.

### Does your app collect or share any required user data types?

**Yes.** The app transmits user-written text off the device to a third-party language-model provider (Groq) in order to produce Analyze results and Buzz In practice questions. Even though nothing is stored on a server you control, Play counts transmission off the device as collection.

### Data type to declare

Choose the item corresponding to **content the user creates in the app** — under the current taxonomy this is *App activity → Other user-generated content*, or the closest equivalent.

| Question | Answer |
|---|---|
| Is this data collected, shared, or both? | **Both** — collected (leaves device) and shared (goes to Groq, a third party) |
| Is it processed ephemerally? | **No** — you do not control Groq's retention, so do not claim this |
| Is it required or optional? | **Optional** — Classic review works fully offline; only Analyze and Buzz In transmit |
| Why is it collected? | **App functionality** only |
| Is it linked to the user's identity? | **No** — no accounts, no identifiers are sent |
| Is it used to track users across apps? | **No** |

### Security practices

| Question | Answer |
|---|---|
| Is data encrypted in transit? | **Yes** — all requests use HTTPS |
| Can users request data deletion? | **Yes** — Settings → Delete all cards, or uninstalling the app |

### Everything else — declare NOT collected

Location · Personal info (name, email, address, phone) · Financial info · Health and fitness · Messages · Photos and videos · Audio · Files and docs · Calendar · Contacts · Web browsing history · Device or other IDs · Crash logs · Diagnostics · Installed apps

There are no analytics, advertising, crash-reporting, or attribution SDKs in the build, so none of these apply.

### Privacy policy URL

Paste the public URL where you host `docs/privacy-policy.md`.

---

## Content rating questionnaire

**Path:** Play Console → Policy and programs → App content → Content ratings

Category: **Reference, News, or Educational**

| Question | Answer |
|---|---|
| Violence, blood, or gore | No |
| Sexual content or nudity | No |
| Profanity or crude humor | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Gambling or simulated gambling | No |
| Horror or fear themes | No |
| Users can interact or exchange content with each other | **No** — there are no social or multiplayer features |
| Shares the user's current location | No |
| Allows purchase of digital goods | No |
| Contains user-generated content shared publicly | No |

Expected outcome: **Everyone / PEGI 3 / ESRB Everyone** or equivalent.

---

## Other App content declarations

**Path:** Play Console → Policy and programs → App content

| Section | Answer |
|---|---|
| Ads | **No ads** — the app contains no advertising |
| Target audience and content | Select **13+** age bands. Selecting under-13 triggers the Families policy program and considerable extra requirements this app does not need |
| Government apps | No |
| Financial features | None |
| Health apps | No |
| News app | No |
| Data safety | See above |

---

## Release notes for the first internal build

**Path:** Play Console → Test and release → Testing → Internal testing → Create new release

```
First internal build.

- Analyze: find common clues for an answer line, with category and difficulty filters
- Cards: create, search, filter, sort, and star study cards
- Classic review with gradual clue reveal and FSRS spaced repetition
- Buzz In practice with word-by-word reveal, buzzing, and scoring
- Stats: lifetime totals and per-card buzz performance
- Configurable study reminders
```
