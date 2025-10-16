# ServitorConnect

A minimal, battery-aware **Intention Repeater** built with **Kotlin + Jetpack Compose**.
It “broadcasts” a user-provided intention in timed bursts, keeps accurate iteration counts, and survives rotation/background/screen-off via a Foreground Service.

---

## ✨ Features

* **Intention** textbox (5 lines, scrollable)
* **Burst Count** (positive ints, default **888888**)
* **Frequency**: `Max`, `3 Hz`, `8 Hz`, `Hourly` (one burst, then sleep)
* **Load File** → appends **SHA-512 hash** of the chosen file to the intention
* **Start / Stop** control
* **Live metrics**: iterations, timer `HH:MM:SS`; at `Max` also shows iterations/sec
* **Status**: “Ready” / “Broadcasting…”
* **Light/Dark Mode** checkbox (overrides system)
* **Duration (seconds)** with bounds: **1..86400** (24h default)
* **Auto-persist settings** with **DataStore**
* **ForegroundService** keeps running through rotation, app switch, and screen off
* Nature-inspired theme (spruce/moss palette)

---

## 📱 Screens

> *Add images later in `/docs` and link them here.*

* Main screen with inputs, status, metrics
* File picker (Storage Access Framework)
* Foreground notification while broadcasting

---

## 🏗️ Tech Stack

* **Language**: Kotlin
* **UI**: Jetpack Compose + Material 3
* **State**: Compose state + `StateFlow` from the service
* **Persistence**: AndroidX DataStore (Preferences)
* **Background work**: ForegroundService + coroutines
* **Min SDK**: 26, **Target SDK**: 36 (compileSdk 36)

---

## 📦 Package / App Id

```
applicationId = com.anthroteacher.servitorconnect
```

---

## 🔐 Permissions

* `POST_NOTIFICATIONS` (Android 13+, for foreground notification)
* `FOREGROUND_SERVICE_DATA_SYNC` (Android 14+, declares service type)

*No network, location, storage reads beyond the file you explicitly pick.*

---

## 🚀 Getting Started

### Prerequisites

* Android Studio **Koala** or newer
* JDK 11
* Android SDK Platforms: API **34/35/36** (as available)
* SDK Tools: **Android Emulator** (if you use an AVD)

### Clone

```bash
git clone https://github.com/<you>/ServitorConnect.git
cd ServitorConnect
```

### Open & Build

1. Open the project in Android Studio.
2. Let Gradle sync.
3. **Run** on a device or emulator.

### Create an Emulator (optional)

* Tools → **Device Manager** → **Create Device** → pick **Pixel 6a**.
* Choose a **Google APIs (x86_64)** system image (or **Google Play** if you need Play Services).
* Start the AVD and select it as the run target.

---

## 🧩 Project Structure

```
app/
 ├─ src/main/
 │   ├─ AndroidManifest.xml
 │   ├─ java/com/anthroteacher/servitorconnect/
 │   │   ├─ MainActivity.kt                     # Compose UI & intents
 │   │   ├─ data/
 │   │   │   └─ Prefs.kt                        # DataStore, Frequency enum, SavedSettings
 │   │   ├─ service/
 │   │   │   └─ ServitorService.kt              # ForegroundService, broadcasting loop
 │   │   └─ ui/theme/
 │   │       ├─ Color.kt                        # Spruce/Moss palette
 │   │       ├─ Theme.kt                        # Material theme wrapper
 │   │       └─ Type.kt                         # Typography
 │   └─ res/…                                   # icons, strings, styles
 └─ build.gradle.kts
```

---

## 🔧 How It Works

* **Start**:

  1. Saves current settings to DataStore.
  2. Starts `ServitorService` in the foreground.
  3. Disables inputs, shows “Broadcasting…”, changes button to **Stop**.

* **Burst Loop** (inside the service):

  * Repeats `burstCount` times:

    ```kotlin
    val placeholder = intention // intentional no-op assignment
    iterations++
    ```
  * Frequency:

    * `Max`: cooperative `yield()` (fast enough, throttling-friendly)
    * `3 Hz`: `delay(333ms)`
    * `8 Hz`: `delay(125ms)`
    * `Hourly`: `delay(3600000ms)` (one burst, then sleep)
  * Updates metrics every second; shows iter/s only for `Max`.

* **Stop**:

  * Cancels the job, keeps the last **iterations** and **timer**, sets status to “Ready”.
  * Re-enables controls; button label returns to **Start**.

* **Load File**:

  * Opens system picker; if a file is chosen, computes `SHA-512` and appends:
    `SHA512:<128-hex-chars>` to the **Intention** box (on a new line if needed).

---

## 🎨 Icon & Theming

* Adaptive launcher icon recommended via **New → Image Asset**.
* Palette:

  * Spruce700 `#1C4631`, Spruce500 `#2E6B4B`, Spruce300 `#63A37F`,
  * Moss100 `#E8F3EC`, Earth900 `#1C1B1F`.

---

## 🧪 Troubleshooting

* **No devices listed**: install an AVD (Device Manager), or plug in a phone with **USB debugging**.
* **Gradle can’t find DataStore alias**:

  * Ensure `gradle/libs.versions.toml` has:

    ```toml
    [versions]
    datastore = "1.1.7"
    [libraries]
    androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
    ```
  * Then use: `implementation(libs.androidx.datastore.preferences)`
  * Sync Gradle; if Studio still marks red, **Invalidate Caches / Restart**.
* **Deprecated `getSerializableExtra` warning**: code uses the 33+ overload and a pre-33 fallback.

---

## 🛡️ Privacy

* The app does not send data anywhere.
* File access happens **only** for the file you pick, to compute a **local** SHA-512 hash.

---

## 🗺️ Roadmap (nice-to-haves)

* Optional “Eco mode” for `Max` (tiny `delay(1)` cap)
* Align “Hourly” to top-of-hour (optional mode)
* Export/import settings
* Wake lock toggle for deep Doze scenarios

---

## 🤝 Contributing

PRs welcome. Keep changes small and focused:

* Compose-first UI
* No breaking changes to `applicationId`
* Maintain minSdk 26 unless there’s a strong case

---

## 📄 License

GPLv3

---

## 📬 Contact

Issues/ideas? Open a GitHub issue or drop feedback in the repo discussions.
