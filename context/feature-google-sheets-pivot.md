# Feature: Google Sheets Voice Entry (No-Gemini Pivot)

**Branch:** nogemini
**Previous branch:** voice-to-journal-prototype (Gemini-based POC, now superseded)

## Progress Log
- **2026-06-10 → 2026-06-11:** (On `voice-to-journal-prototype` branch) Built and verified the Gemini-based voice-to-journal POC end-to-end on a physical device. See `context/feature-voice-to-journal-prototype.md` for details.
- **2026-06-21:** Pivoted the entire app away from Gemini. Deleted Gemini module, journal storage, seed data, review screen, old tests (30 files changed). Replaced with: Google Sign-In, Google Sheets REST API via OkHttp, Android SpeechRecognizer for Hindi STT, ML Kit for Hindi→English on-device translation, SharedPreferences for local property/room config. Added Property→Room hierarchy with full CRUD. New UI: SignIn → PropertyList → RoomList → VoiceEntry. Added voice entry history (last 10 from Sheets, paginated "Show more"). Added pastel-colored cards, empty state hints, keyboard capitalization. Fixed bugs: room rename not syncing sheet tab name, room delete not removing sheet tab. Commits: `0b1d9ab`, `4882de5`.

## Current State
The app is fully functional on the `nogemini` branch. The complete flow works: Google Sign-In → create property (auto-creates Google Spreadsheet) → add rooms (auto-creates sheet tabs) → record Hindi voice note via SpeechRecognizer → edit text → save (ML Kit translates to English, inserts row at top of sheet). Recent entries are displayed below the input area with pagination. Property and room CRUD syncs with Google Sheets (create/rename/delete tabs). Build passes (`./gradlew assembleDebug` ✅). Tested manually on a physical Samsung device.

**We stopped at:** All planned work for the pivot is complete. The app is usable and distributable as an APK. The `nogemini` branch has not been merged to `voice-to-journal-prototype` (or any main branch) — that decision is pending.

**Single next action:** Decide whether to merge `nogemini` into `voice-to-journal-prototype` (or a new main branch), push, and/or create a PR. Then start planning the periodic processing job that will read Google Sheets entries and generate actions/wiki.

## Key Files
### New (created this session)
- `data/auth/GoogleAuthManager.kt` — Google Sign-In wrapper, requests Sheets OAuth scope
- `data/sheets/SheetsClient.kt` — Google Sheets REST API (create spreadsheet, add/rename/delete sheet tabs, insert rows at top, read rows for history)
- `data/speech/SpeechRecognizerManager.kt` — Android SpeechRecognizer for Hindi (`hi-IN`), partial + final results
- `data/translation/TranslationManager.kt` — ML Kit Hindi→English on-device translation with model download
- `data/store/PropertyStore.kt` — SharedPreferences CRUD for properties and rooms (JSON via kotlinx-serialization)
- `data/model/Models.kt` — `Property(id, name, address, spreadsheetId)` and `Room(id, propertyId, label, floor, expectedRent, tenantName, securityDeposit)`
- `ui/auth/SignInScreen.kt` — Google Sign-In button, shown on first launch
- `ui/property/PropertyListScreen.kt` — Property list with colored cards, CRUD dialogs, empty state
- `ui/room/RoomListScreen.kt` — Room list with property name header, colored cards, CRUD dialogs, empty state
- `ui/voice/VoiceEntryScreen.kt` — Compact voice input (4-5 lines), recent entries history from Sheets, "Show more" pagination
- `ui/theme/CardColors.kt` — 8 pastel colors for list item backgrounds

### Significantly modified
- `ui/AppViewModel.kt` — Complete rewrite: manages auth, properties, rooms, speech, translation, Sheets operations, history pagination
- `ui/navigation/AppNavigation.kt` — Complete rewrite: signIn → propertyList → roomList → voiceEntry
- `app/build.gradle.kts` — Removed Gemini BuildConfig, added `play-services-auth` and `mlkit:translate`

### Deleted (Gemini-era files)
- `data/gemini/` (GeminiClient, GeminiPromptBuilder, GeminiResponseParser)
- `data/journal/` (JournalRepository, MarkdownJournal)
- `data/audio/AudioRecorder.kt`, `data/seed/SeedDataLoader.kt`, `assets/rentals_seed.json`
- `ui/review/ReviewScreen.kt`, `ui/ProcessingState.kt`, `ui/rentallist/RentalListScreen.kt`, `ui/journal/JournalScreen.kt`
- All unit tests under `app/src/test/` (tested deleted code)

## Decisions Made
- **Gemini removed entirely** — user feedback: skip LLM intelligence, just capture raw speech. A separate periodic job will process entries later for actions/wiki.
- **Voice entries are append-only** — no edit/delete of saved entries. Corrections are new entries. The periodic processing job will reconcile.
- **Google Sheets as backend** — one spreadsheet per property (auto-created), one sheet tab per room (auto-created). Row format: DateTime | Hindi Text | English Translation. New rows inserted at row 2 (below header) so latest is always on top.
- **ML Kit for translation** — on-device, free, no cloud API needed. Falls back to "(translation unavailable)" if model download fails.
- **SharedPreferences + JSON for local config** — simplest approach for property/room metadata. Google Sheet is only for voice entries.
- **Legacy Google Sign-In API** — deprecated but simpler than Credential Manager for this prototype. Produces deprecation warnings at compile time but works fine.
- **APK distribution model** — recipients don't need their own Google Cloud project. They just install, sign in with their Google account, and spreadsheets are created in their own Drive. Developer's OAuth consent screen must either list them as test users or be published.

## Open Questions
- Should `nogemini` be merged into `voice-to-journal-prototype` or a new main branch?
- When to build the periodic processing job that reads Sheets entries and generates actions/wiki?
- Should the OAuth consent screen be published (removes test-user restriction, shows "unverified app" warning) or kept in testing mode with manual test-user additions?
- Should a root-level `README.md` be created with setup instructions (Google Cloud Console steps, SHA-1, etc.)?
- No unit tests exist currently — the old tests were deleted with the Gemini code. Should new tests be written for SheetsClient, PropertyStore, etc.?
