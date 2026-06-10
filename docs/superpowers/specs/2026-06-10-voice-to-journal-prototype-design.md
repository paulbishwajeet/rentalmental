# Voice-to-Journal Android Prototype — Design

## Goal

Validate the core end-to-end loop for "Rental Mental": an owner selects a rental, speaks an
update in Hindi (e.g., "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega."),
reviews the AI-interpreted structured entry, confirms it, and it's appended to that rental's
journal — stored locally as a markdown "LLM Wiki" file. This is a rough, single-user prototype;
data model and storage are intentionally throwaway/local for now. Cloud storage (Google Sheets),
real notifications, and the web app are out of scope and will be separate follow-up specs.

## 1. Architecture & Tech Stack

- **Platform**: Native Android, Kotlin + Jetpack Compose.
- **Audio capture**: `MediaRecorder`, recording short clips (e.g., `.m4a`) to app-local cache.
- **AI processing**: Audio sent directly to the **Gemini API** (e.g., `gemini-2.5-flash`, which
  supports audio input) via REST. Gemini API key stored in the app build config (not committed),
  acceptable for this personal/single-owner prototype.
- **Storage**: Local markdown files in app-internal storage — one file per rental, "LLM Wiki"
  style (YAML frontmatter + chronological journal log + reminders list). No cloud/Sheets yet.
- **Seed data**: A small bundled JSON asset listing 2-3 sample rentals, used to generate the
  initial markdown files on first app launch.

### Flow

```
[Rental List Screen] → select rental
        ↓
[Journal Screen] → renders existing markdown journal + record button
        ↓ (tap record, speak Hindi, tap stop)
[Gemini API call] → audio + context (rental info + recent journal) → structured JSON
        ↓
[Review Screen] → transcription + extracted fields (editable) + any reminder
        ↓ (user confirms / edits / discards)
[Append to rental's .md file] → back to Journal Screen, updated view
```

## 2. Data Model & Markdown "LLM Wiki" Format

### Seed data (`assets/rentals_seed.json`)

```json
[
  {
    "id": "F0-R1",
    "label": "Ground Floor - Room 1",
    "floor": 0,
    "expected_rent": 5000,
    "capacity": 2,
    "rooms": "1 room + attached bath, no kitchen",
    "electricity_responsibility": "tenant",
    "security_deposit": 10000
  }
]
```

### Per-rental markdown file (`journals/<rental_id>.md`)

Generated on first launch from seed data:

```markdown
---
rental_id: F0-R1
label: Ground Floor - Room 1
floor: 0
expected_rent: 5000
capacity: 2
rooms: 1 room + attached bath, no kitchen
electricity_responsibility: tenant
security_deposit: 10000
---

# Journal

## 2026-06-10
- **14:32 — Rent Payment**
  - Person: Prakash
  - Amount: ₹2000 (cash)
  - Note: Partial rent payment. Remaining ₹3000 promised for tomorrow.
  - Raw (Hindi): "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega."

# Reminders
- [ ] 2026-06-11 — Follow up with Prakash for remaining ₹3000 rent (from 2026-06-10 entry)
```

- Frontmatter holds the rental's "standards" (rent, capacity, electricity, deposit) — static
  reference info.
- `# Journal` is an append-only chronological log, grouped by date, with structured sub-fields
  plus the raw Hindi transcription kept for traceability/audit.
- `# Reminders` is a flat checklist Gemini appends to when it detects a follow-up — no
  scheduling/notifications yet, just visible in the file.
- Files live at `context.filesDir/journals/<rental_id>.md`.

## 3. App Screens & Flow

### Screen 1 — Rental List
- List of rentals from seed data (label, floor).
- Tap to open that rental's journal.

### Screen 2 — Journal Screen
- Header: rental label + key standards (expected rent, capacity, electricity responsibility)
  from frontmatter.
- Scrollable rendered view of `# Journal` and `# Reminders` (most recent entries first).
- Floating record button:
  - Tap once: start recording (visual indicator, e.g. waveform/pulsing icon + timer).
  - Tap again: stop recording → triggers Gemini call → navigates to Review Screen.
  - Loading state while waiting on Gemini's response.

### Screen 3 — Review Screen
- Raw transcription (Hindi text) — read-only reference.
- Extracted structured fields, **editable**:
  - Date/time (defaults to now)
  - Type (Rent Payment / Electricity / Expense / Other — dropdown)
  - Person name
  - Amount
  - Payment method (cash/online — if mentioned)
  - Note/summary (free text, editable)
- If Gemini detected a reminder: editable reminder block (date + description), with a toggle to
  include/exclude it.
- Actions:
  - **Confirm** → appends entry (and reminder, if included) to the rental's `.md` file, returns
    to Journal Screen.
  - **Discard** → discards everything, returns to Journal Screen, no changes.
  - **Re-record** → discard and go back to recording.

## 4. Gemini Integration

### Request

Audio file (`.m4a`/`.amr`, single utterance) sent to Gemini via `generateContent` (inline base64
audio data), `gemini-2.5-flash`. Accompanying text prompt includes:

- Today's date (so "kal" / "tomorrow" resolves correctly).
- The rental's frontmatter context (label, expected rent, capacity, electricity responsibility).
- A short tail of recent journal entries (e.g., last 3-5) for continuity.
- Instructions to transcribe the Hindi audio and extract a structured journal entry + optional
  reminder, returned as JSON.

### Response schema

```json
{
  "transcription": "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega.",
  "entry_type": "rent_payment",
  "person": "Prakash",
  "amount": 2000,
  "payment_method": "cash",
  "summary": "Partial rent payment. Remaining ₹3000 promised for tomorrow.",
  "reminder": {
    "has_reminder": true,
    "due_date": "2026-06-11",
    "description": "Follow up with Prakash for remaining ₹3000 rent"
  }
}
```

### Handling

- Parse JSON → pre-fill Review Screen fields.
- `entry_type` constrained to a small enum (`rent_payment`, `electricity_payment`, `expense`,
  `other`) — shown as an editable dropdown so the user can correct misclassification.
- If `reminder.has_reminder` is `false`, the reminder block is hidden/collapsed but the user can
  manually add one.

## 5. Error Handling & Edge Cases

- **Recording issues**: mic permission not granted → prompt before allowing record; very
  short/empty recording → inline error, retry without leaving the screen.
- **Gemini API failures** (network error, timeout, rate limit, malformed response): show an
  error state with "Retry" (re-send the same audio without re-recording). If JSON doesn't parse
  or is missing required fields, fall back to a Review Screen with just the raw transcription
  and blank structured fields for manual entry — never silently lose the recording.
- **Ambiguous/incomplete extraction**: leave fields blank/flagged on Review Screen rather than
  guessing — user fills them in before confirming.
- **File write errors**: show error toast, keep entry data in memory so the user can retry
  confirm without redoing the recording.
- **No internet connectivity**: detect before recording/sending and show a clear message; keep
  recorded audio so the user can retry once back online.

No retry queues or offline-sync for this prototype — just ensure nothing is silently lost and
the user always lands on a screen where they can fix or retry.

## 6. Testing Approach

- **Manual end-to-end testing** on a physical device/emulator: record real Hindi utterances
  (including the example above), verify transcription accuracy, extracted fields, and reminder
  detection across varied phrasings (different transaction types, multiple people, online vs
  cash, partial payments).
- **Unit tests** for markdown read/write logic (parsing frontmatter, appending journal entries,
  appending reminders) — file format correctness matters for future phases (Sheets migration,
  alerts, etc.).
- **Unit tests** for JSON response parsing/validation (handling missing fields, malformed JSON,
  enum fallback for `entry_type`).
- No automated UI tests for this prototype phase — manual verification of the three-screen flow
  is sufficient given the small surface area.

## Out of Scope (Future Phases)

- Google Sheets / cloud storage migration.
- Real push notifications/alerts for reminders.
- Web app.
- Multi-tenant-per-rental payment splitting logic, security deposit refund calculations, etc.
  (will inform the future "real" data model).
