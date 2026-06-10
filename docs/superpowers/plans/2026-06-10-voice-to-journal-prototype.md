# Voice-to-Journal Android Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal Android app where an owner picks a rental, speaks a Hindi update, reviews Gemini's structured interpretation, confirms it, and has it appended to a local markdown "LLM Wiki" journal file for that rental.

**Architecture:** Native Android app (Kotlin + Jetpack Compose, single Activity, Compose Navigation). A `JournalRepository` reads/writes per-rental markdown files in app-internal storage. Audio is recorded with `MediaRecorder`, sent to the Gemini API (`gemini-2.5-flash`, audio input) along with rental context and recent journal text, and the JSON response is parsed into a draft journal entry shown on a Review screen before being appended to the markdown file.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Navigation, kotlinx-serialization-json, OkHttp, JUnit (unit tests), Gemini REST API.

---

## Reference: Markdown Journal Format

Each rental has a file `journals/<rental_id>.md`. Initial (empty) form:

```
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

# Reminders
```

After appending one journal entry:

```
---
rental_id: F0-R1
...
---

# Journal

### 2026-06-10 14:32 — Rent Payment
- Person: Prakash
- Amount: ₹2000 (cash)
- Note: Partial rent payment. Remaining ₹3000 promised for tomorrow.
- Raw (Hindi): "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega."

# Reminders
```

After appending a reminder:

```
# Reminders

- [ ] 2026-06-11 — Follow up with Prakash for remaining ₹3000 rent
```

This is a flattened, single-level version of the spec's grouped format (each entry carries its
own date/time in a `### ` heading instead of nesting under `## <date>`), chosen because it makes
appends a simple "insert before `# Reminders`" operation with no need to find/merge into existing
date sections — much easier to implement correctly and test.

---

## Task 1: Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/com/rentalmental/MainActivity.kt`
- Create: `.gitignore`

- [ ] **Step 1: Create root Gradle settings**

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RentalMental"
include(":app")
```

- [ ] **Step 2: Create root build file**

`build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
}
```

- [ ] **Step 3: Create gradle.properties**

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
# Set your Gemini API key here for local builds (do not commit a real key):
GEMINI_API_KEY=
```

- [ ] **Step 4: Create .gitignore**

`.gitignore`:

```
.gradle/
build/
.idea/
local.properties
*.iml
.DS_Store
captures/
.cxx/
```

- [ ] **Step 5: Create app module build file**

`app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.rentalmental"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rentalmental"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        val geminiApiKey = (project.findProperty("GEMINI_API_KEY") as String?) ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 6: Create AndroidManifest.xml**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar"
        android:supportsRtl="true">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Create strings.xml**

`app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Rental Mental</string>
</resources>
```

- [ ] **Step 8: Create placeholder MainActivity**

`app/src/main/java/com/rentalmental/MainActivity.kt`:

```kotlin
package com.rentalmental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface {
                Text("Rental Mental")
            }
        }
    }
}
```

- [ ] **Step 9: Generate the Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.7`
Expected: creates `gradlew`, `gradlew.bat`, and `gradle/wrapper/` files.

If `gradle` is not installed locally, open the project in Android Studio once — it will
generate the wrapper automatically. Subsequent steps assume `./gradlew` is available.

- [ ] **Step 10: Build the project to verify scaffolding**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties .gitignore app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/java/com/rentalmental/MainActivity.kt gradlew gradlew.bat gradle/
git commit -m "Scaffold Rental Mental Android project"
```

---

## Task 2: Data Models

**Files:**
- Create: `app/src/main/java/com/rentalmental/data/model/Models.kt`

- [ ] **Step 1: Create the data model file**

`app/src/main/java/com/rentalmental/data/model/Models.kt`:

```kotlin
package com.rentalmental.data.model

data class Rental(
    val id: String,
    val label: String,
    val floor: Int,
    val expectedRent: Int,
    val capacity: Int,
    val rooms: String,
    val electricityResponsibility: String,
    val securityDeposit: Int
)

enum class EntryType(val label: String) {
    RENT_PAYMENT("Rent Payment"),
    ELECTRICITY_PAYMENT("Electricity Payment"),
    EXPENSE("Expense"),
    OTHER("Other");

    companion object {
        fun fromKey(key: String?): EntryType =
            entries.find { it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}

data class JournalEntry(
    val date: String,
    val time: String,
    val type: EntryType,
    val person: String,
    val amount: Int?,
    val paymentMethod: String?,
    val summary: String,
    val rawTranscription: String
)

data class Reminder(
    val dueDate: String,
    val description: String,
    val done: Boolean = false
)

data class GeminiExtraction(
    val transcription: String,
    val entryType: EntryType,
    val person: String,
    val amount: Int?,
    val paymentMethod: String?,
    val summary: String,
    val reminder: Reminder?
)
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/model/Models.kt
git commit -m "Add core data models"
```

---

## Task 3: Markdown Journal Read/Write Logic

**Files:**
- Create: `app/src/test/java/com/rentalmental/data/journal/MarkdownJournalTest.kt`
- Create: `app/src/main/java/com/rentalmental/data/journal/MarkdownJournal.kt`

- [ ] **Step 1: Write failing tests for all four functions**

`app/src/test/java/com/rentalmental/data/journal/MarkdownJournalTest.kt`:

```kotlin
package com.rentalmental.data.journal

import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownJournalTest {

    private fun sampleRental() = Rental(
        id = "F0-R1",
        label = "Ground Floor - Room 1",
        floor = 0,
        expectedRent = 5000,
        capacity = 2,
        rooms = "1 room + attached bath, no kitchen",
        electricityResponsibility = "tenant",
        securityDeposit = 10000
    )

    private fun sampleEntry(time: String, person: String) = JournalEntry(
        date = "2026-06-10",
        time = time,
        type = EntryType.RENT_PAYMENT,
        person = person,
        amount = 2000,
        paymentMethod = "cash",
        summary = "Partial rent payment.",
        rawTranscription = "raw transcription"
    )

    @Test
    fun `generateInitialMarkdown contains frontmatter and section headings`() {
        val markdown = generateInitialMarkdown(sampleRental())

        assertTrue(markdown.startsWith("---\n"))
        assertTrue(markdown.contains("rental_id: F0-R1"))
        assertTrue(markdown.contains("label: Ground Floor - Room 1"))
        assertTrue(markdown.contains("expected_rent: 5000"))
        assertTrue(markdown.contains("\n# Journal\n"))
        assertTrue(markdown.contains("\n# Reminders"))
        assertTrue(markdown.indexOf("# Journal") < markdown.indexOf("# Reminders"))
    }

    @Test
    fun `appendJournalEntry inserts entry block before Reminders section`() {
        val initial = generateInitialMarkdown(sampleRental())
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "14:32",
            type = EntryType.RENT_PAYMENT,
            person = "Prakash",
            amount = 2000,
            paymentMethod = "cash",
            summary = "Partial rent payment. Remaining ₹3000 promised for tomorrow.",
            rawTranscription = "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega."
        )

        val updated = appendJournalEntry(initial, entry)

        assertTrue(updated.contains("### 2026-06-10 14:32 — Rent Payment"))
        assertTrue(updated.contains("- Person: Prakash"))
        assertTrue(updated.contains("- Amount: ₹2000 (cash)"))
        assertTrue(updated.contains("- Note: Partial rent payment. Remaining ₹3000 promised for tomorrow."))
        assertTrue(updated.contains("- Raw (Hindi): \"Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega.\""))
        assertTrue(updated.indexOf("### 2026-06-10 14:32") < updated.indexOf("# Reminders"))
        assertTrue(updated.indexOf("# Journal") < updated.indexOf("### 2026-06-10 14:32"))
    }

    @Test
    fun `appendJournalEntry omits amount line when amount is null`() {
        val initial = generateInitialMarkdown(sampleRental())
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "09:00",
            type = EntryType.OTHER,
            person = "Sunita",
            amount = null,
            paymentMethod = null,
            summary = "Asked about water leakage in bathroom.",
            rawTranscription = "Sunita ne bola bathroom mein paani leak ho raha hai."
        )

        val updated = appendJournalEntry(initial, entry)

        assertFalse(updated.contains("- Amount:"))
        assertTrue(updated.contains("- Note: Asked about water leakage in bathroom."))
    }

    @Test
    fun `appendJournalEntry can append multiple entries in order`() {
        var markdown = generateInitialMarkdown(sampleRental())
        markdown = appendJournalEntry(markdown, sampleEntry(time = "09:00", person = "Sunita"))
        markdown = appendJournalEntry(markdown, sampleEntry(time = "18:00", person = "Prakash"))

        val firstIndex = markdown.indexOf("Sunita")
        val secondIndex = markdown.indexOf("Prakash")
        assertTrue(firstIndex in 0 until secondIndex)
        assertTrue(secondIndex < markdown.indexOf("# Reminders"))
    }

    @Test
    fun `appendReminder adds checklist item after Reminders heading`() {
        val initial = generateInitialMarkdown(sampleRental())
        val reminder = Reminder(
            dueDate = "2026-06-11",
            description = "Follow up with Prakash for remaining ₹3000 rent"
        )

        val updated = appendReminder(initial, reminder)

        assertTrue(updated.contains("- [ ] 2026-06-11 — Follow up with Prakash for remaining ₹3000 rent"))
        assertTrue(updated.indexOf("# Reminders") < updated.indexOf("- [ ] 2026-06-11"))
    }

    @Test
    fun `appendReminder can append multiple reminders`() {
        var markdown = generateInitialMarkdown(sampleRental())
        markdown = appendReminder(markdown, Reminder("2026-06-11", "First reminder"))
        markdown = appendReminder(markdown, Reminder("2026-06-12", "Second reminder"))

        assertTrue(markdown.contains("- [ ] 2026-06-11 — First reminder"))
        assertTrue(markdown.contains("- [ ] 2026-06-12 — Second reminder"))
        assertTrue(markdown.indexOf("First reminder") < markdown.indexOf("Second reminder"))
    }

    @Test
    fun `extractRecentJournalText returns most recent entries up to the limit`() {
        var markdown = generateInitialMarkdown(sampleRental())
        markdown = appendJournalEntry(markdown, sampleEntry(time = "08:00", person = "Person1"))
        markdown = appendJournalEntry(markdown, sampleEntry(time = "09:00", person = "Person2"))
        markdown = appendJournalEntry(markdown, sampleEntry(time = "10:00", person = "Person3"))

        val recent = extractRecentJournalText(markdown, maxEntries = 2)

        assertFalse(recent.contains("Person1"))
        assertTrue(recent.contains("Person2"))
        assertTrue(recent.contains("Person3"))
        assertFalse(recent.contains("# Reminders"))
    }

    @Test
    fun `extractRecentJournalText returns empty string when no entries exist`() {
        val markdown = generateInitialMarkdown(sampleRental())

        val recent = extractRecentJournalText(markdown, maxEntries = 5)

        assertEquals("", recent)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.journal.MarkdownJournalTest"`
Expected: FAIL (compile error — `generateInitialMarkdown`, `appendJournalEntry`, `appendReminder`,
`extractRecentJournalText` are unresolved references).

- [ ] **Step 3: Implement MarkdownJournal.kt**

`app/src/main/java/com/rentalmental/data/journal/MarkdownJournal.kt`:

```kotlin
package com.rentalmental.data.journal

import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental

private const val JOURNAL_HEADING = "# Journal"
private const val REMINDERS_HEADING = "# Reminders"

fun generateInitialMarkdown(rental: Rental): String {
    return buildString {
        appendLine("---")
        appendLine("rental_id: ${rental.id}")
        appendLine("label: ${rental.label}")
        appendLine("floor: ${rental.floor}")
        appendLine("expected_rent: ${rental.expectedRent}")
        appendLine("capacity: ${rental.capacity}")
        appendLine("rooms: ${rental.rooms}")
        appendLine("electricity_responsibility: ${rental.electricityResponsibility}")
        appendLine("security_deposit: ${rental.securityDeposit}")
        appendLine("---")
        appendLine()
        appendLine(JOURNAL_HEADING)
        appendLine()
        appendLine(REMINDERS_HEADING)
    }
}

private fun formatEntryBlock(entry: JournalEntry): String {
    return buildString {
        appendLine("### ${entry.date} ${entry.time} — ${entry.type.label}")
        appendLine("- Person: ${entry.person}")
        if (entry.amount != null) {
            val method = entry.paymentMethod?.let { " ($it)" } ?: ""
            appendLine("- Amount: ₹${entry.amount}$method")
        }
        appendLine("- Note: ${entry.summary}")
        append("- Raw (Hindi): \"${entry.rawTranscription}\"")
    }
}

fun appendJournalEntry(markdown: String, entry: JournalEntry): String {
    val lines = markdown.lines().toMutableList()
    val reminderIndex = lines.indexOfFirst { it.trim() == REMINDERS_HEADING }
    require(reminderIndex >= 0) { "Markdown missing '$REMINDERS_HEADING' section" }

    val toInsert = mutableListOf<String>()
    toInsert.addAll(formatEntryBlock(entry).lines())
    toInsert.add("")

    val insertAt = if (reminderIndex > 0 && lines[reminderIndex - 1].isNotBlank()) {
        lines.add(reminderIndex, "")
        reminderIndex + 1
    } else {
        reminderIndex
    }

    lines.addAll(insertAt, toInsert)
    return lines.joinToString("\n")
}

fun appendReminder(markdown: String, reminder: Reminder): String {
    val lines = markdown.lines().toMutableList()
    val reminderIndex = lines.indexOfFirst { it.trim() == REMINDERS_HEADING }
    require(reminderIndex >= 0) { "Markdown missing '$REMINDERS_HEADING' section" }

    val checkbox = if (reminder.done) "[x]" else "[ ]"
    lines.add("- $checkbox ${reminder.dueDate} — ${reminder.description}")
    return lines.joinToString("\n")
}

fun extractRecentJournalText(markdown: String, maxEntries: Int): String {
    val lines = markdown.lines()
    val journalIndex = lines.indexOfFirst { it.trim() == JOURNAL_HEADING }
    val reminderIndex = lines.indexOfFirst { it.trim() == REMINDERS_HEADING }
    if (journalIndex < 0 || reminderIndex < 0 || reminderIndex <= journalIndex) return ""

    val journalLines = lines.subList(journalIndex + 1, reminderIndex)
    val entryStarts = journalLines.withIndex()
        .filter { (_, line) -> line.trim().startsWith("### ") }
        .map { it.index }

    if (entryStarts.isEmpty()) return ""

    val firstIncluded = entryStarts.takeLast(maxEntries).first()
    return journalLines.subList(firstIncluded, journalLines.size)
        .joinToString("\n")
        .trim()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.journal.MarkdownJournalTest"`
Expected: `BUILD SUCCESSFUL`, all 8 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/journal/MarkdownJournal.kt app/src/test/java/com/rentalmental/data/journal/MarkdownJournalTest.kt
git commit -m "Add markdown journal read/write logic with tests"
```

---

## Task 4: Seed Data Loader and Sample Rentals

**Files:**
- Create: `app/src/test/java/com/rentalmental/data/seed/SeedDataLoaderTest.kt`
- Create: `app/src/main/java/com/rentalmental/data/seed/SeedDataLoader.kt`
- Create: `app/src/main/assets/rentals_seed.json`

- [ ] **Step 1: Write failing test for parseRentals**

`app/src/test/java/com/rentalmental/data/seed/SeedDataLoaderTest.kt`:

```kotlin
package com.rentalmental.data.seed

import org.junit.Assert.assertEquals
import org.junit.Test

class SeedDataLoaderTest {

    @Test
    fun `parseRentals maps JSON fields to Rental objects`() {
        val json = """
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
              },
              {
                "id": "F1-R1",
                "label": "First Floor - Room 1",
                "floor": 1,
                "expected_rent": 6000,
                "capacity": 3,
                "rooms": "2 rooms + shared bath + kitchen",
                "electricity_responsibility": "tenant",
                "security_deposit": 12000
              }
            ]
        """.trimIndent()

        val rentals = SeedDataLoader.parseRentals(json)

        assertEquals(2, rentals.size)
        assertEquals("F0-R1", rentals[0].id)
        assertEquals("Ground Floor - Room 1", rentals[0].label)
        assertEquals(0, rentals[0].floor)
        assertEquals(5000, rentals[0].expectedRent)
        assertEquals(2, rentals[0].capacity)
        assertEquals("1 room + attached bath, no kitchen", rentals[0].rooms)
        assertEquals("tenant", rentals[0].electricityResponsibility)
        assertEquals(10000, rentals[0].securityDeposit)
        assertEquals("F1-R1", rentals[1].id)
        assertEquals(3, rentals[1].capacity)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.seed.SeedDataLoaderTest"`
Expected: FAIL (compile error — `SeedDataLoader` is unresolved).

- [ ] **Step 3: Implement SeedDataLoader.kt**

`app/src/main/java/com/rentalmental/data/seed/SeedDataLoader.kt`:

```kotlin
package com.rentalmental.data.seed

import android.content.Context
import com.rentalmental.data.model.Rental
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RentalDto(
    val id: String,
    val label: String,
    val floor: Int,
    val expected_rent: Int,
    val capacity: Int,
    val rooms: String,
    val electricity_responsibility: String,
    val security_deposit: Int
)

object SeedDataLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseRentals(jsonText: String): List<Rental> {
        val dtos = json.decodeFromString<List<RentalDto>>(jsonText)
        return dtos.map {
            Rental(
                id = it.id,
                label = it.label,
                floor = it.floor,
                expectedRent = it.expected_rent,
                capacity = it.capacity,
                rooms = it.rooms,
                electricityResponsibility = it.electricity_responsibility,
                securityDeposit = it.security_deposit
            )
        }
    }

    fun loadFromAssets(context: Context): List<Rental> {
        val jsonText = context.assets.open("rentals_seed.json")
            .bufferedReader()
            .use { it.readText() }
        return parseRentals(jsonText)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.seed.SeedDataLoaderTest"`
Expected: `BUILD SUCCESSFUL`, test passes.

- [ ] **Step 5: Create the bundled seed data asset**

`app/src/main/assets/rentals_seed.json`:

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
  },
  {
    "id": "F0-R2",
    "label": "Ground Floor - Room 2",
    "floor": 0,
    "expected_rent": 4500,
    "capacity": 1,
    "rooms": "1 room + shared bath, no kitchen",
    "electricity_responsibility": "tenant",
    "security_deposit": 9000
  },
  {
    "id": "F1-R1",
    "label": "First Floor - Room 1",
    "floor": 1,
    "expected_rent": 7000,
    "capacity": 3,
    "rooms": "2 rooms + attached bath + kitchen",
    "electricity_responsibility": "tenant",
    "security_deposit": 14000
  }
]
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/seed/SeedDataLoader.kt app/src/test/java/com/rentalmental/data/seed/SeedDataLoaderTest.kt app/src/main/assets/rentals_seed.json
git commit -m "Add seed data loader and sample rentals"
```

---

## Task 5: Journal Repository

**Files:**
- Create: `app/src/test/java/com/rentalmental/data/journal/JournalRepositoryTest.kt`
- Create: `app/src/main/java/com/rentalmental/data/journal/JournalRepository.kt`

- [ ] **Step 1: Write failing tests for JournalRepository**

`app/src/test/java/com/rentalmental/data/journal/JournalRepositoryTest.kt`:

```kotlin
package com.rentalmental.data.journal

import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class JournalRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repository: JournalRepository

    private val rental = Rental(
        id = "F0-R1",
        label = "Ground Floor - Room 1",
        floor = 0,
        expectedRent = 5000,
        capacity = 2,
        rooms = "1 room + attached bath, no kitchen",
        electricityResponsibility = "tenant",
        securityDeposit = 10000
    )

    @Before
    fun setUp() {
        tempDir = File.createTempFile("journals", "").apply {
            delete()
            mkdirs()
        }
        repository = JournalRepository(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `initializeIfNeeded creates a markdown file per rental`() {
        repository.initializeIfNeeded(listOf(rental))

        val file = File(tempDir, "F0-R1.md")
        assertTrue(file.exists())
        assertTrue(file.readText().contains("rental_id: F0-R1"))
    }

    @Test
    fun `initializeIfNeeded does not overwrite an existing file`() {
        repository.initializeIfNeeded(listOf(rental))
        val file = File(tempDir, "F0-R1.md")
        file.appendText("\nextra content")

        repository.initializeIfNeeded(listOf(rental))

        assertTrue(file.readText().contains("extra content"))
    }

    @Test
    fun `appendEntry writes the entry to the rental's journal file`() {
        repository.initializeIfNeeded(listOf(rental))
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "14:32",
            type = EntryType.RENT_PAYMENT,
            person = "Prakash",
            amount = 2000,
            paymentMethod = "cash",
            summary = "Partial rent payment.",
            rawTranscription = "raw"
        )

        repository.appendEntry("F0-R1", entry)

        assertTrue(repository.readJournal("F0-R1").contains("Prakash"))
    }

    @Test
    fun `appendReminderToJournal writes the reminder to the rental's journal file`() {
        repository.initializeIfNeeded(listOf(rental))
        repository.appendReminderToJournal("F0-R1", Reminder("2026-06-11", "Follow up with Prakash"))

        val content = repository.readJournal("F0-R1")
        assertTrue(content.contains("- [ ] 2026-06-11 — Follow up with Prakash"))
    }

    @Test
    fun `recentContext returns recent journal entries for the rental`() {
        repository.initializeIfNeeded(listOf(rental))
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "14:32",
            type = EntryType.RENT_PAYMENT,
            person = "Prakash",
            amount = 2000,
            paymentMethod = "cash",
            summary = "Partial rent payment.",
            rawTranscription = "raw"
        )
        repository.appendEntry("F0-R1", entry)

        val context = repository.recentContext("F0-R1", maxEntries = 5)

        assertTrue(context.contains("Prakash"))
    }

    @Test
    fun `readJournal returns empty string for unknown rental`() {
        assertEquals("", repository.readJournal("UNKNOWN"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.journal.JournalRepositoryTest"`
Expected: FAIL (compile error — `JournalRepository` is unresolved).

- [ ] **Step 3: Implement JournalRepository.kt**

`app/src/main/java/com/rentalmental/data/journal/JournalRepository.kt`:

```kotlin
package com.rentalmental.data.journal

import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import java.io.File

class JournalRepository(private val journalsDir: File) {

    init {
        if (!journalsDir.exists()) journalsDir.mkdirs()
    }

    private fun fileFor(rentalId: String) = File(journalsDir, "$rentalId.md")

    fun initializeIfNeeded(rentals: List<Rental>) {
        for (rental in rentals) {
            val file = fileFor(rental.id)
            if (!file.exists()) {
                file.writeText(generateInitialMarkdown(rental))
            }
        }
    }

    fun readJournal(rentalId: String): String {
        val file = fileFor(rentalId)
        return if (file.exists()) file.readText() else ""
    }

    fun appendEntry(rentalId: String, entry: JournalEntry) {
        val file = fileFor(rentalId)
        file.writeText(appendJournalEntry(file.readText(), entry))
    }

    fun appendReminderToJournal(rentalId: String, reminder: Reminder) {
        val file = fileFor(rentalId)
        file.writeText(appendReminder(file.readText(), reminder))
    }

    fun recentContext(rentalId: String, maxEntries: Int = 5): String {
        return extractRecentJournalText(readJournal(rentalId), maxEntries)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.journal.JournalRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/journal/JournalRepository.kt app/src/test/java/com/rentalmental/data/journal/JournalRepositoryTest.kt
git commit -m "Add journal repository for reading and writing rental journals"
```

---

## Task 6: Gemini Prompt Builder

**Files:**
- Create: `app/src/test/java/com/rentalmental/data/gemini/GeminiPromptBuilderTest.kt`
- Create: `app/src/main/java/com/rentalmental/data/gemini/GeminiPromptBuilder.kt`

- [ ] **Step 1: Write failing tests for the prompt builder**

`app/src/test/java/com/rentalmental/data/gemini/GeminiPromptBuilderTest.kt`:

```kotlin
package com.rentalmental.data.gemini

import com.rentalmental.data.model.Rental
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPromptBuilderTest {

    private val rental = Rental(
        id = "F0-R1",
        label = "Ground Floor - Room 1",
        floor = 0,
        expectedRent = 5000,
        capacity = 2,
        rooms = "1 room + attached bath, no kitchen",
        electricityResponsibility = "tenant",
        securityDeposit = 10000
    )

    @Test
    fun `buildPrompt includes rental details, date, and JSON schema`() {
        val prompt = GeminiPromptBuilder.buildPrompt(rental, recentJournalText = "", todayDate = "2026-06-10")

        assertTrue(prompt.contains("Ground Floor - Room 1"))
        assertTrue(prompt.contains("₹5000"))
        assertTrue(prompt.contains("2026-06-10"))
        assertTrue(prompt.contains("\"transcription\""))
        assertTrue(prompt.contains("\"entry_type\""))
        assertTrue(prompt.contains("\"reminder\""))
    }

    @Test
    fun `buildPrompt includes recent journal context when provided`() {
        val prompt = GeminiPromptBuilder.buildPrompt(
            rental,
            recentJournalText = "### 2026-06-09 10:00 — Rent Payment\n- Person: Prakash",
            todayDate = "2026-06-10"
        )

        assertTrue(prompt.contains("Recent journal entries"))
        assertTrue(prompt.contains("### 2026-06-09 10:00 — Rent Payment"))
    }

    @Test
    fun `buildPrompt omits recent journal section when empty`() {
        val prompt = GeminiPromptBuilder.buildPrompt(rental, recentJournalText = "", todayDate = "2026-06-10")

        assertFalse(prompt.contains("Recent journal entries"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.gemini.GeminiPromptBuilderTest"`
Expected: FAIL (compile error — `GeminiPromptBuilder` is unresolved).

- [ ] **Step 3: Implement GeminiPromptBuilder.kt**

`app/src/main/java/com/rentalmental/data/gemini/GeminiPromptBuilder.kt`:

```kotlin
package com.rentalmental.data.gemini

import com.rentalmental.data.model.Rental

object GeminiPromptBuilder {

    fun buildPrompt(rental: Rental, recentJournalText: String, todayDate: String): String {
        return buildString {
            appendLine("You are an assistant that helps an Indian landlord journal rental activity.")
            appendLine("Today's date is $todayDate (format YYYY-MM-DD).")
            appendLine()
            appendLine("Rental details:")
            appendLine("- Label: ${rental.label}")
            appendLine("- Expected monthly rent: ₹${rental.expectedRent}")
            appendLine("- Capacity: ${rental.capacity}")
            appendLine("- Electricity responsibility: ${rental.electricityResponsibility}")
            appendLine()
            if (recentJournalText.isNotBlank()) {
                appendLine("Recent journal entries for context:")
                appendLine(recentJournalText)
                appendLine()
            }
            appendLine("Listen to the attached audio, which is in Hindi. Transcribe it, then extract a structured journal entry.")
            appendLine("Respond with ONLY a JSON object matching this schema (no markdown fences):")
            appendLine(
                """
                {
                  "transcription": string,
                  "entry_type": one of "rent_payment" | "electricity_payment" | "expense" | "other",
                  "person": string,
                  "amount": number or null,
                  "payment_method": "cash" | "online" | null,
                  "summary": string,
                  "reminder": {
                    "has_reminder": boolean,
                    "due_date": "YYYY-MM-DD" or null,
                    "description": string or null
                  } or null
                }
                """.trimIndent()
            )
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.gemini.GeminiPromptBuilderTest"`
Expected: `BUILD SUCCESSFUL`, all 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/gemini/GeminiPromptBuilder.kt app/src/test/java/com/rentalmental/data/gemini/GeminiPromptBuilderTest.kt
git commit -m "Add Gemini prompt builder"
```

---

## Task 7: Gemini Response Parser

**Files:**
- Create: `app/src/test/java/com/rentalmental/data/gemini/GeminiResponseParserTest.kt`
- Create: `app/src/main/java/com/rentalmental/data/gemini/GeminiResponseParser.kt`

- [ ] **Step 1: Write failing tests for the response parser**

`app/src/test/java/com/rentalmental/data/gemini/GeminiResponseParserTest.kt`:

```kotlin
package com.rentalmental.data.gemini

import com.rentalmental.data.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiResponseParserTest {

    @Test
    fun `parse extracts all fields from a well-formed response`() {
        val raw = """
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
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        result!!
        assertEquals("Prakash", result.person)
        assertEquals(EntryType.RENT_PAYMENT, result.entryType)
        assertEquals(2000, result.amount)
        assertEquals("cash", result.paymentMethod)
        assertNotNull(result.reminder)
        assertEquals("2026-06-11", result.reminder!!.dueDate)
        assertEquals("Follow up with Prakash for remaining ₹3000 rent", result.reminder!!.description)
    }

    @Test
    fun `parse handles response wrapped in markdown code fences`() {
        val raw = """
            ```json
            {
              "transcription": "test",
              "entry_type": "expense",
              "person": "Owner",
              "amount": 500,
              "payment_method": null,
              "summary": "Bought cleaning supplies.",
              "reminder": null
            }
            ```
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        result!!
        assertEquals(EntryType.EXPENSE, result.entryType)
        assertEquals(500, result.amount)
        assertNull(result.paymentMethod)
        assertNull(result.reminder)
    }

    @Test
    fun `parse returns null reminder when has_reminder is false`() {
        val raw = """
            {
              "transcription": "test",
              "entry_type": "other",
              "person": "Sunita",
              "amount": null,
              "payment_method": null,
              "summary": "General note.",
              "reminder": { "has_reminder": false, "due_date": null, "description": null }
            }
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        assertNull(result!!.reminder)
    }

    @Test
    fun `parse returns null for malformed JSON`() {
        val result = GeminiResponseParser.parse("not valid json")

        assertNull(result)
    }

    @Test
    fun `parse falls back to OTHER for unknown entry_type`() {
        val raw = """
            {
              "transcription": "test",
              "entry_type": "something_unexpected",
              "person": "Owner",
              "amount": null,
              "payment_method": null,
              "summary": "note",
              "reminder": null
            }
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        assertEquals(EntryType.OTHER, result!!.entryType)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.gemini.GeminiResponseParserTest"`
Expected: FAIL (compile error — `GeminiResponseParser` is unresolved).

- [ ] **Step 3: Implement GeminiResponseParser.kt**

`app/src/main/java/com/rentalmental/data/gemini/GeminiResponseParser.kt`:

```kotlin
package com.rentalmental.data.gemini

import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.GeminiExtraction
import com.rentalmental.data.model.Reminder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object GeminiResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawText: String): GeminiExtraction? {
        val cleaned = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val obj = json.parseToJsonElement(cleaned).jsonObject

            val transcription = obj["transcription"]?.jsonPrimitive?.contentOrNull ?: ""
            val entryType = EntryType.fromKey(obj["entry_type"]?.jsonPrimitive?.contentOrNull)
            val person = obj["person"]?.jsonPrimitive?.contentOrNull ?: ""
            val amount = obj["amount"]?.jsonPrimitive?.intOrNull
            val paymentMethod = obj["payment_method"]?.jsonPrimitive?.contentOrNull
            val summary = obj["summary"]?.jsonPrimitive?.contentOrNull ?: ""

            val reminderObj = obj["reminder"] as? JsonObject
            val reminder = if (reminderObj != null &&
                reminderObj["has_reminder"]?.jsonPrimitive?.booleanOrNull == true
            ) {
                Reminder(
                    dueDate = reminderObj["due_date"]?.jsonPrimitive?.contentOrNull ?: "",
                    description = reminderObj["description"]?.jsonPrimitive?.contentOrNull ?: ""
                )
            } else null

            GeminiExtraction(
                transcription = transcription,
                entryType = entryType,
                person = person,
                amount = amount,
                paymentMethod = paymentMethod,
                summary = summary,
                reminder = reminder
            )
        } catch (e: Exception) {
            null
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rentalmental.data.gemini.GeminiResponseParserTest"`
Expected: `BUILD SUCCESSFUL`, all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/gemini/GeminiResponseParser.kt app/src/test/java/com/rentalmental/data/gemini/GeminiResponseParserTest.kt
git commit -m "Add Gemini response parser with fallback handling"
```

---

## Task 8: Gemini API Client

This task has no automated tests — it makes a real network call to the Gemini API and is
verified manually in Task 14 (end-to-end test on device).

**Files:**
- Create: `app/src/main/java/com/rentalmental/data/gemini/GeminiClient.kt`

- [ ] **Step 1: Implement GeminiClient.kt**

`app/src/main/java/com/rentalmental/data/gemini/GeminiClient.kt`:

```kotlin
package com.rentalmental.data.gemini

import com.rentalmental.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    fun transcribeAndExtract(audioFile: File, prompt: String, mimeType: String = "audio/mp4"): String {
        val audioBase64 = android.util.Base64.encodeToString(
            audioFile.readBytes(), android.util.Base64.NO_WRAP
        )

        val requestJson = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray()
                                .put(JSONObject().apply { put("text", prompt) })
                                .put(
                                    JSONObject().apply {
                                        put(
                                            "inline_data",
                                            JSONObject().apply {
                                                put("mime_type", mimeType)
                                                put("data", audioBase64)
                                            }
                                        )
                                    }
                                )
                        )
                    }
                )
            )
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$endpoint?key=$apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Gemini request failed: ${response.code} ${response.body?.string()}")
            }
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            val responseJson = JSONObject(responseBody)
            return responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Set your Gemini API key for local builds**

Edit `gradle.properties` and set:

```properties
GEMINI_API_KEY=your-actual-key-here
```

Do not commit your real key — `gradle.properties` with a real key should stay local. If you
want to keep a key out of `gradle.properties` entirely, instead create
`~/.gradle/gradle.properties` (outside the repo) with the same `GEMINI_API_KEY=...` line; Gradle
merges both files and the user-level one takes precedence.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/gemini/GeminiClient.kt
git commit -m "Add Gemini API client for audio transcription and extraction"
```

---

## Task 9: Audio Recorder

This task has no automated tests — `MediaRecorder` requires a real device/emulator with a
microphone and is verified manually in Task 14.

**Files:**
- Create: `app/src/main/java/com/rentalmental/data/audio/AudioRecorder.kt`

- [ ] **Step 1: Implement AudioRecorder.kt**

`app/src/main/java/com/rentalmental/data/audio/AudioRecorder.kt`:

```kotlin
package com.rentalmental.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File {
        val file = File(context.cacheDir, "journal_${System.currentTimeMillis()}.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            try {
                prepare()
            } catch (e: IOException) {
                throw IllegalStateException("Failed to prepare MediaRecorder", e)
            }
            start()
        }

        recorder = mediaRecorder
        return file
    }

    fun stopRecording(): File {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile ?: throw IllegalStateException("No active recording")
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rentalmental/data/audio/AudioRecorder.kt
git commit -m "Add audio recorder wrapper around MediaRecorder"
```

---

## Task 10: Rental List Screen

**Files:**
- Create: `app/src/main/java/com/rentalmental/ui/rentallist/RentalListScreen.kt`

- [ ] **Step 1: Implement RentalListScreen.kt**

`app/src/main/java/com/rentalmental/ui/rentallist/RentalListScreen.kt`:

```kotlin
package com.rentalmental.ui.rentallist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rentalmental.data.model.Rental

@Composable
fun RentalListScreen(
    rentals: List<Rental>,
    onRentalSelected: (Rental) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Rental Mental") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(rentals, key = { it.id }) { rental ->
                ListItem(
                    headlineContent = { Text(rental.label) },
                    supportingContent = {
                        Text("Floor ${rental.floor} · Capacity ${rental.capacity} · Rent ₹${rental.expectedRent}")
                    },
                    modifier = Modifier.clickable { onRentalSelected(rental) }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rentalmental/ui/rentallist/RentalListScreen.kt
git commit -m "Add rental list screen"
```

---

## Task 11: Journal Screen and Processing State

**Files:**
- Create: `app/src/main/java/com/rentalmental/ui/ProcessingState.kt`
- Create: `app/src/main/java/com/rentalmental/ui/journal/JournalScreen.kt`

- [ ] **Step 1: Define the shared ProcessingState type**

`app/src/main/java/com/rentalmental/ui/ProcessingState.kt`:

```kotlin
package com.rentalmental.ui

import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder

sealed interface ProcessingState {
    object Idle : ProcessingState
    object Recording : ProcessingState
    object Processing : ProcessingState
    data class ReadyForReview(val draft: JournalEntry, val reminder: Reminder?) : ProcessingState
    data class Failed(val message: String, val rawTranscription: String?) : ProcessingState
}
```

- [ ] **Step 2: Implement JournalScreen.kt**

`app/src/main/java/com/rentalmental/ui/journal/JournalScreen.kt`:

```kotlin
package com.rentalmental.ui.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalmental.data.model.Rental
import com.rentalmental.ui.ProcessingState

@Composable
fun JournalScreen(
    rental: Rental,
    journalText: String,
    processingState: ProcessingState,
    onRecordToggle: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rental.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onRecordToggle) {
                if (processingState is ProcessingState.Recording) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop recording")
                } else {
                    Icon(Icons.Filled.Mic, contentDescription = "Record")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Expected rent: ₹${rental.expectedRent} · Capacity: ${rental.capacity}")
            Spacer(modifier = Modifier.height(8.dp))

            if (processingState is ProcessingState.Processing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Listening and processing...")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (processingState is ProcessingState.Failed) {
                Text("Error: ${processingState.message}", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = journalText,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            )
        }
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/rentalmental/ui/ProcessingState.kt app/src/main/java/com/rentalmental/ui/journal/JournalScreen.kt
git commit -m "Add journal screen and processing state model"
```

---

## Task 12: Review Screen

**Files:**
- Create: `app/src/main/java/com/rentalmental/ui/review/ReviewScreen.kt`

- [ ] **Step 1: Implement ReviewScreen.kt**

`app/src/main/java/com/rentalmental/ui/review/ReviewScreen.kt`:

```kotlin
package com.rentalmental.ui.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder

@Composable
fun ReviewScreen(
    draft: JournalEntry,
    initialReminder: Reminder?,
    onConfirm: (JournalEntry, Reminder?) -> Unit,
    onDiscard: () -> Unit,
    onReRecord: () -> Unit
) {
    var person by remember { mutableStateOf(draft.person) }
    var amountText by remember { mutableStateOf(draft.amount?.toString() ?: "") }
    var paymentMethod by remember { mutableStateOf(draft.paymentMethod ?: "") }
    var summary by remember { mutableStateOf(draft.summary) }
    var entryType by remember { mutableStateOf(draft.type) }
    var includeReminder by remember { mutableStateOf(initialReminder != null) }
    var reminderDate by remember { mutableStateOf(initialReminder?.dueDate ?: "") }
    var reminderDesc by remember { mutableStateOf(initialReminder?.description ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Review entry") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Text("Transcription (Hindi)", style = MaterialTheme.typography.labelLarge)
            Text(draft.rawTranscription, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = entryType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    EntryType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                entryType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = person,
                onValueChange = { person = it },
                label = { Text("Person") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment method (cash/online)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeReminder, onCheckedChange = { includeReminder = it })
                Text("Add reminder")
            }
            if (includeReminder) {
                OutlinedTextField(
                    value = reminderDate,
                    onValueChange = { reminderDate = it },
                    label = { Text("Reminder date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reminderDesc,
                    onValueChange = { reminderDesc = it },
                    label = { Text("Reminder description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Button(onClick = {
                    val entry = draft.copy(
                        type = entryType,
                        person = person,
                        amount = amountText.toIntOrNull(),
                        paymentMethod = paymentMethod.takeUnless { it.isBlank() },
                        summary = summary
                    )
                    val reminder = if (includeReminder && reminderDate.isNotBlank() && reminderDesc.isNotBlank()) {
                        Reminder(dueDate = reminderDate, description = reminderDesc)
                    } else null
                    onConfirm(entry, reminder)
                }) {
                    Text("Confirm")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onDiscard) {
                    Text("Discard")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onReRecord) {
                    Text("Re-record")
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rentalmental/ui/review/ReviewScreen.kt
git commit -m "Add review screen for confirming journal entries"
```

---

## Task 13: App ViewModel, Navigation, and Activity Wiring

**Files:**
- Create: `app/src/main/java/com/rentalmental/ui/AppViewModel.kt`
- Create: `app/src/main/java/com/rentalmental/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/rentalmental/MainActivity.kt`

- [ ] **Step 1: Implement AppViewModel.kt**

`app/src/main/java/com/rentalmental/ui/AppViewModel.kt`:

```kotlin
package com.rentalmental.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rentalmental.data.audio.AudioRecorder
import com.rentalmental.data.gemini.GeminiClient
import com.rentalmental.data.gemini.GeminiPromptBuilder
import com.rentalmental.data.gemini.GeminiResponseParser
import com.rentalmental.data.journal.JournalRepository
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import com.rentalmental.data.seed.SeedDataLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JournalRepository(File(application.filesDir, "journals"))
    private val geminiClient = GeminiClient()
    private val audioRecorder = AudioRecorder(application)

    private val _rentals = MutableStateFlow<List<Rental>>(emptyList())
    val rentals: StateFlow<List<Rental>> = _rentals

    private val _journalText = MutableStateFlow("")
    val journalText: StateFlow<String> = _journalText

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    var selectedRental: Rental? = null
        private set

    private var lastAudioFile: File? = null
    private var lastPrompt: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = SeedDataLoader.loadFromAssets(application)
            repository.initializeIfNeeded(loaded)
            _rentals.update { loaded }
        }
    }

    fun selectRental(rentalId: String) {
        selectedRental = _rentals.value.find { it.id == rentalId }
        refreshJournal()
    }

    private fun refreshJournal() {
        val rental = selectedRental ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _journalText.update { repository.readJournal(rental.id) }
        }
    }

    fun startRecording() {
        _processingState.update { ProcessingState.Recording }
        audioRecorder.startRecording()
    }

    fun stopRecordingAndProcess() {
        val rental = selectedRental ?: return
        _processingState.update { ProcessingState.Processing }
        viewModelScope.launch(Dispatchers.IO) {
            val audioFile = audioRecorder.stopRecording()
            lastAudioFile = audioFile

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val recentContext = repository.recentContext(rental.id)
            val prompt = GeminiPromptBuilder.buildPrompt(rental, recentContext, today)
            lastPrompt = prompt

            processWithGemini(audioFile, prompt)
        }
    }

    fun retryProcessing() {
        val audioFile = lastAudioFile
        val prompt = lastPrompt
        if (audioFile == null || prompt == null) {
            _processingState.update { ProcessingState.Idle }
            return
        }
        _processingState.update { ProcessingState.Processing }
        viewModelScope.launch(Dispatchers.IO) {
            processWithGemini(audioFile, prompt)
        }
    }

    private fun processWithGemini(audioFile: File, prompt: String) {
        try {
            val rawResponse = geminiClient.transcribeAndExtract(audioFile, prompt)
            val extraction = GeminiResponseParser.parse(rawResponse)

            if (extraction == null) {
                _processingState.update {
                    ProcessingState.Failed(
                        "Could not understand the response. Please fill in details manually.",
                        rawResponse
                    )
                }
            } else {
                val now = Date()
                val draft = JournalEntry(
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
                    time = SimpleDateFormat("HH:mm", Locale.US).format(now),
                    type = extraction.entryType,
                    person = extraction.person,
                    amount = extraction.amount,
                    paymentMethod = extraction.paymentMethod,
                    summary = extraction.summary,
                    rawTranscription = extraction.transcription
                )
                _processingState.update { ProcessingState.ReadyForReview(draft, extraction.reminder) }
            }
        } catch (e: Exception) {
            _processingState.update { ProcessingState.Failed(e.message ?: "Unknown error", null) }
        }
    }

    fun confirmEntry(entry: JournalEntry, reminder: Reminder?) {
        val rental = selectedRental ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.appendEntry(rental.id, entry)
            if (reminder != null) {
                repository.appendReminderToJournal(rental.id, reminder)
            }
            refreshJournal()
            _processingState.update { ProcessingState.Idle }
        }
    }

    fun discard() {
        _processingState.update { ProcessingState.Idle }
    }
}
```

- [ ] **Step 2: Implement AppNavigation.kt**

`app/src/main/java/com/rentalmental/ui/navigation/AppNavigation.kt`:

```kotlin
package com.rentalmental.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rentalmental.ui.AppViewModel
import com.rentalmental.ui.ProcessingState
import com.rentalmental.ui.journal.JournalScreen
import com.rentalmental.ui.rentallist.RentalListScreen
import com.rentalmental.ui.review.ReviewScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = viewModel()

    val rentals by viewModel.rentals.collectAsState()
    val journalText by viewModel.journalText.collectAsState()
    val processingState by viewModel.processingState.collectAsState()

    NavHost(navController = navController, startDestination = "rentalList") {
        composable("rentalList") {
            RentalListScreen(
                rentals = rentals,
                onRentalSelected = { rental ->
                    viewModel.selectRental(rental.id)
                    navController.navigate("journal")
                }
            )
        }
        composable("journal") {
            val rental = viewModel.selectedRental ?: return@composable
            val state = processingState

            if (state is ProcessingState.ReadyForReview) {
                ReviewScreen(
                    draft = state.draft,
                    initialReminder = state.reminder,
                    onConfirm = { entry, reminder -> viewModel.confirmEntry(entry, reminder) },
                    onDiscard = { viewModel.discard() },
                    onReRecord = {
                        viewModel.discard()
                        viewModel.startRecording()
                    }
                )
            } else {
                JournalScreen(
                    rental = rental,
                    journalText = journalText,
                    processingState = state,
                    onRecordToggle = {
                        if (state is ProcessingState.Recording) {
                            viewModel.stopRecordingAndProcess()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    onRetry = { viewModel.retryProcessing() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

- [ ] **Step 3: Wire up MainActivity with permission request and navigation**

Replace the contents of `app/src/main/java/com/rentalmental/MainActivity.kt`:

```kotlin
package com.rentalmental

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.rentalmental.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* no-op; recording will fail gracefully if denied */ }
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            AppNavigation()
        }
    }
}
```

- [ ] **Step 4: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests from Tasks 3-7 pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/rentalmental/ui/AppViewModel.kt app/src/main/java/com/rentalmental/ui/navigation/AppNavigation.kt app/src/main/java/com/rentalmental/MainActivity.kt
git commit -m "Wire up app navigation, view model, and permission request"
```

---

## Task 14: Manual End-to-End Verification

**Files:** none (manual testing only)

- [ ] **Step 1: Install the app on a device or emulator**

Run: `./gradlew :app:installDebug`
Expected: app installs and launches showing "Rental Mental" with the rental list (3 rentals
from `rentals_seed.json`).

- [ ] **Step 2: Grant microphone permission**

On first launch, accept the microphone permission prompt. If denied, recording will fail —
re-grant via system Settings > Apps > Rental Mental > Permissions if needed.

- [ ] **Step 3: Record the example utterance**

Tap a rental (e.g., "Ground Floor - Room 1") to open its journal. The journal should show the
empty `# Journal` / `# Reminders` sections from the seed file. Tap the mic FAB, speak (in
Hindi): "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega.", then tap the
stop FAB.

Expected: a "Listening and processing..." indicator appears, then the Review screen opens with:
- Transcription showing the Hindi sentence
- Type: "Rent Payment"
- Person: "Prakash"
- Amount: "2000"
- Payment method: "cash" (or blank if Gemini didn't infer it — fill in manually)
- Note summarizing the partial payment
- Reminder checkbox checked, with tomorrow's date and a description about the remaining ₹3000

- [ ] **Step 4: Confirm the entry**

Tap "Confirm". Expected: returns to the Journal screen, and the journal text now shows the new
`### <date> <time> — Rent Payment` block with Prakash's details, and the `# Reminders` section
shows the new checklist item.

- [ ] **Step 5: Test discard and re-record**

Record a second utterance, then on the Review screen tap "Discard". Expected: returns to the
Journal screen with no new entry added. Record again and tap "Re-record" instead — expected:
discards the current draft and immediately starts a new recording.

- [ ] **Step 6: Test varied phrasings**

Repeat Step 3 with at least these variations and confirm reasonable extraction (correcting
fields manually on the Review screen where Gemini gets it wrong is expected and fine):
- An electricity payment: e.g., "Sunita ne electricity bill ke 800 rupaye online diye hai."
- An expense with no person/tenant involved: e.g., "Aaj safai ke liye 300 rupaye diye."
- An entry with no reminder: confirm the reminder checkbox is unchecked and the journal entry
  is still saved correctly without a `# Reminders` addition.

- [ ] **Step 7: Test error handling**

Turn off the device's network connection, then record an utterance and stop. Expected: the
Journal screen shows an error message and a "Retry" button (per `ProcessingState.Failed`).
Turn the network back on and tap "Retry" — expected: processing succeeds and the Review screen
opens without needing to re-record.

- [ ] **Step 8: Record observations**

Note any systematic transcription/extraction issues (e.g., consistently wrong entry type for
certain phrasings) in a follow-up note — these may inform prompt tuning in a future iteration,
but are not blockers for this prototype.

---

## Out of Scope (carried over from spec)

- Google Sheets / cloud storage migration
- Real push notifications/alerts for reminders
- Web app
- Multi-tenant-per-rental payment splitting, security deposit refund calculations
