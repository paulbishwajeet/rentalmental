# Project Overview
**Name:** RentalMental (Gradle root project name; app `applicationId`/namespace `com.rentalmental`). [?] No project-level README exists yet to confirm the product description — "Rental Mental" / small-town India rental property management is taken from `docs/superpowers/specs/2026-06-10-voice-to-journal-prototype-design.md`, not from a README.

**Stack:**
- Native Android, single module (`:app`), namespace `com.rentalmental`
- Kotlin (Android Gradle Plugin 8.5.0, Kotlin plugin 2.0.0, Gradle 8.7)
- Jetpack Compose UI (`compose-bom:2024.06.00`, Material3, `material-icons-extended`)
- Compose Navigation (`androidx.navigation:navigation-compose:2.7.7`)
- AndroidX Lifecycle/ViewModel + Compose integration (`lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose:2.8.1`)
- `kotlinx-serialization-json:1.6.3` (plugin: `org.jetbrains.kotlin.plugin.serialization`)
- OkHttp 4.12.0 for REST calls to the Gemini API (`gemini-2.5-flash`, audio input + structured extraction)
- `MediaRecorder` for on-device audio capture
- minSdk 26, targetSdk/compileSdk 34, Java/Kotlin target 17

**Package Manager:** Gradle via wrapper (`./gradlew`). No JS/Node `package.json` — this is a pure Android/Kotlin project.

**Key Directories:**
- `app/src/main/java/com/rentalmental/` — app source
  - `data/audio/AudioRecorder.kt` — MediaRecorder wrapper (`startRecording()`/`stopRecording()`)
  - `data/gemini/` — `GeminiClient` (OkHttp REST calls to Gemini), `GeminiPromptBuilder`, `GeminiResponseParser`
  - `data/journal/` — `JournalRepository`, `MarkdownJournal` (per-rental markdown "LLM Wiki" files)
  - `data/model/Models.kt` — `Rental`, `EntryType` (enum: RENT_PAYMENT/ELECTRICITY_PAYMENT/EXPENSE/OTHER), `JournalEntry`, `Reminder`, `GeminiExtraction`
  - `data/seed/SeedDataLoader.kt` — loads `app/src/main/assets/rentals_seed.json` on first launch
  - `ui/AppViewModel.kt` — `AndroidViewModel` holding app state (`StateFlow<List<Rental>>`, `StateFlow<String> journalText`, `StateFlow<ProcessingState>`)
  - `ui/ProcessingState.kt` — sealed interface: `Idle`, `Recording`, `Processing`, `ReadyForReview(draft, reminder)`, `Failed(message, rawTranscription)`
  - `ui/rentallist/RentalListScreen.kt`, `ui/journal/JournalScreen.kt`, `ui/review/ReviewScreen.kt` — the three Compose screens
  - `ui/navigation/AppNavigation.kt` — NavHost wiring the three screens based on `ProcessingState`
  - `MainActivity.kt` — entry point; registers `RECORD_AUDIO` permission launcher, sets `AppNavigation()`
- `app/src/main/assets/rentals_seed.json` — bundled seed data for rentals
- `app/src/main/res/values/strings.xml` — string resources
- `app/src/test/java/com/rentalmental/` — JUnit unit tests, mirrors `data/` package structure (`data/gemini/`, `data/journal/`, `data/seed/`)
- `docs/superpowers/specs/` — design specs (`YYYY-MM-DD-<topic>-design.md`)
- `docs/superpowers/plans/` — implementation plans (`YYYY-MM-DD-<feature>.md`)
- `context/` — this directory; cross-session context files

**Coding Conventions** (observed in existing source/tests, not from a written style guide):
- Sealed interfaces for UI state (e.g. `ProcessingState`)
- `@OptIn(ExperimentalMaterial3Api::class)` on screens using `Scaffold`/`TopAppBar`/`ExposedDropdownMenuBox` (required by Material3 1.2.1 from the BOM)
- Resource cleanup (e.g. `MediaRecorder.stop()`/`release()`) wrapped in try/finally
- ViewModel exceptions surfaced via `ProcessingState.Failed(message, rawTranscription)`, never silently swallowed
- Per-rental data stored as markdown files with YAML frontmatter under `context.filesDir/journals/<rental_id>.md` (the "LLM Wiki" format — see design spec for exact structure)
- Data classes in `data/model/Models.kt` use camelCase Kotlin properties; enum `EntryType` has a `fromKey()` companion factory with `OTHER` as fallback
- [?] No linter/formatter config (e.g. ktlint/detekt) found in the repo — formatting conventions are whatever Android Studio defaults produce
- Built using Subagent-Driven Development: implementer → spec-compliance reviewer → code-quality reviewer per task (see `superpowers:subagent-driven-development`)

**Testing Framework:** JUnit 4 (`junit:junit:4.13.2`) Kotlin unit tests under `app/src/test/java/com/rentalmental/`, run via `./gradlew test`. Tests use plain JUnit `Assert` (`assertEquals`/`assertTrue`/`assertFalse`), with local `sample*()` helper factories for fixtures (no test-specific DI framework). No instrumentation/UI tests (`androidTest`) exist — screen flows are verified manually on a physical device.

**Env/Config Notes:**
- `GEMINI_API_KEY` must be set in `~/.gradle/gradle.properties` (user-level), NOT in the project's `gradle.properties` — project-level values take precedence, and an empty value there will override the user's key (caused a 403 bug, fixed in commit `8df2842`).
- `BuildConfig.GEMINI_API_KEY` is wired in `app/build.gradle.kts:19-20` via `project.findProperty("GEMINI_API_KEY")`; requires `buildFeatures { buildConfig = true }`.
- `local.properties` holds `sdk.dir`; `ANDROID_HOME` env var (used by `gradlew`) points to a borrowed SDK at `/Users/bishwajeetpaul/workspace/github/MediScan/android-sdk`. [?] This is a local machine-specific path, likely needs to be set up per-developer.
- `AndroidManifest.xml` declares `RECORD_AUDIO` and `INTERNET` permissions; theme is `@android:style/Theme.Material.Light.NoActionBar`.
- `material-icons-extended` Compose dependency is required for Mic/Stop icons used on the Journal screen.

**Modules/Features List:**
- `:app` — the only Gradle module; entire app lives here (single-module project, no `:core`/`:data`/`:feature` split yet)
- Voice-to-journal prototype (branch `voice-to-journal-prototype`, spec: `docs/superpowers/specs/2026-06-10-voice-to-journal-prototype-design.md`) — record a Hindi voice note about a rental, transcribe + extract a structured entry via Gemini, review/edit, append to that rental's markdown journal. Status: implementation + manual E2E verification complete (commit `8df2842` and prior on this branch).
- [?] No other features/modules exist in the codebase yet — this prototype is the first and only feature implemented so far.
