# 💍 S&A Wedding Todo — Native Android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Compose BOM](https://img.shields.io/badge/Compose-2026.02.01-4285F4?logo=android)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-34.16.0-FFCA28?logo=firebase)](https://firebase.google.com/)
[![Min SDK](https://img.shields.io/badge/minSdk-28-3DDC84?logo=android)](https://developer.android.com/)
[![CI/CD](https://img.shields.io/github/actions/workflow/status/akashhiremath25/Wedding-todos/release.yml?label=release&logo=github)](https://github.com/akashhiremath25/Wedding-todos/actions)
[![Release](https://img.shields.io/github/v/release/akashhiremath25/Wedding-todos?logo=github)](https://github.com/akashhiremath25/Wedding-todos/releases)

A fully native Android application built with **Jetpack Compose** and **Firebase** for managing Shradha & Abhishek's wedding itinerary and tasks. Features real-time sync, role-based access, local notifications, and over-the-air (OTA) self-updates.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔐 **Google Sign-In** | Secure authentication via Firebase Auth + Google Play Services. |
| 🛡️ **Role-Based Access** | Admin vs Guest permissions enforced via Firestore collections (`admins` / `allowed_guests`). |
| ⚡ **Real-Time Sync** | Live task lists synced across all devices using Firebase Firestore snapshots. |
| 🔔 **Smart Notifications** | WorkManager + AlarmReceiver schedule precision reminders for upcoming tasks. |
| 📅 **Native Pickers** | Material 3 Date & Time pickers for seamless event scheduling. |
| 🎨 **Material 3 UI** | Fluid transitions, list animations, and a custom wedding-themed color palette. |
| 🔄 **OTA Self-Updates** | Automatically checks GitHub Releases and prompts users to download the latest APK. |
| 💾 **Offline Resilience** | Tasks cached locally via `TaskStorage` (Gson) for notification reliability even when offline. |
| 🚀 **CI/CD Release Pipeline** | GitHub Actions builds signed release APKs on every version tag (`v*`). |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│                 UI Layer                     │
│   AuthScreen │ DashboardScreen │ TaskEditor  │
│        (Jetpack Compose + Material 3)        │
├─────────────────────────────────────────────┤
│              ViewModel Layer                 │
│         TaskViewModel (MVVM)                 │
│   • AuthState (Sealed Class)                 │
│   • Real-time Firestore Listeners            │
│   • Notification Scheduling                  │
├─────────────────────────────────────────────┤
│              Data Layer                      │
│   • Firebase Auth (Google Sign-In)           │
│   • Firebase Firestore (wedding_tasks)       │
│   • Local TaskStorage (Gson / SharedPrefs)   │
├─────────────────────────────────────────────┤
│           Background Workers                 │
│   • NotificationWorker (15 min periodic)     │
│   • SyncWorker (24 hr periodic)              │
│   • TaskAlarmReceiver (Exact alarms)         │
└─────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Kotlin | `2.2.10` |
| **UI Toolkit** | Jetpack Compose (BOM) | `2026.02.01` |
| **Material Design** | Material 3 + Extended Icons | — |
| **Architecture** | MVVM + StateFlow | — |
| **DI** | Manual (ViewModelFactory) | — |
| **Backend** | Firebase Auth | `24.2.0` |
| **Database** | Firebase Firestore | BOM `34.16.0` |
| **Auth Provider** | Google Play Services Auth | `21.6.0` |
| **Background Work** | WorkManager | `2.10.0` |
| **Navigation** | Compose Navigation | `2.9.8` |
| **Serialization** | Gson | `2.11.0` |
| **Build System** | Gradle Kotlin DSL | AGP `9.3.0` |
| **Min/Target SDK** | Android 9+ / API 37 | `28 / 37` |

---

## 📁 Project Structure

```
app/src/main/java/com/shradhaabhishek/weddingtodos/
├── MainActivity.kt              # Entry point, NavHost, OTA update logic
├── WeddingApplication.kt        # Notification channel & WorkManager setup
├── model/
│   └── Task.kt                  # Firestore data class with ServerTimestamp
├── ui/
│   ├── AuthScreen.kt            # Google Sign-In UI
│   ├── DashboardScreen.kt       # Task list, filters, admin actions
│   ├── TaskEditor.kt            # Add/Edit task with date/time pickers
│   ├── Branding.kt              # App logo & themed assets
│   └── theme/                   # Color, Type, Theme (Material 3)
├── viewmodel/
│   └── TaskViewModel.kt         # AuthState, Firestore listeners, CRUD
├── worker/
│   ├── NotificationWorker.kt    # Periodic notification checks
│   └── SyncWorker.kt            # Daily background sync
├── receiver/
│   └── TaskAlarmReceiver.kt     # Exact alarm broadcasts for reminders
└── util/
    ├── NotificationScheduler.kt # AlarmManager scheduling logic
    └── TaskStorage.kt           # Local Gson persistence
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- A Firebase project with **Authentication** and **Firestore** enabled

### 1. Clone the repository

```bash
git clone https://github.com/akashhiremath25/Wedding-todos.git
cd Wedding-todos
```

### 2. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an Android app with package name `wedding.todo`.
3. Download `google-services.json` and place it in:

```
app/google-services.json
```

### 3. Firestore Security Rules (Minimum)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /wedding_tasks/{task} {
      allow read, write: if request.auth != null;
    }
    match /admins/{userId} {
      allow read: if request.auth != null;
    }
    match /allowed_guests/{email} {
      allow read: if request.auth != null;
    }
  }
}
```

### 4. Firestore Collections

Create the following collections manually for role-based access:
- `admins` — Documents with IDs matching Firebase Auth UIDs of admin users.
- `allowed_guests` — Documents with IDs matching lowercase guest emails.

### 5. Build & Run

```bash
./gradlew assembleDebug
# Or run directly via Android Studio
```

---

## 🔏 Release Build (Signed APK)

Create a `local.properties` file in the project root:

```properties
KEYSTORE_PATH=/path/to/your/release-key.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

Then build:

```bash
./gradlew assembleRelease
```

The signed APK will be output to `app/build/outputs/apk/release/`.

---

## 🔄 CI/CD — GitHub Actions

Pushing a tag starting with `v` (e.g., `v1.0.3`) triggers the release workflow:

| Step | Action |
|------|--------|
| 1 | Checkout & setup Java 17 (Temurin) |
| 2 | Decode release keystore from `secrets.KEYSTORE_B64` |
| 3 | Inject `google-services.json` from secrets |
| 4 | Build signed Release APK |
| 5 | Generate SHA-256 checksum |
| 6 | Publish GitHub Release with APK + checksum |

**Required Repository Secrets:**
- `KEYSTORE_B64` — Base64-encoded release keystore
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `GOOGLE_SERVICES_JSON`

---

## 🔔 Notification System

- **Periodic Worker** (`NotificationWorker`) runs every 15 minutes to check for upcoming tasks.
- **Exact Alarms** (`TaskAlarmReceiver`) are scheduled via `AlarmManager` for precision reminders.
- **Daily Sync** (`SyncWorker`) refreshes local storage every 24 hours.
- **Real-Time Updates** — Firestore snapshot listeners immediately reschedule alarms when tasks change.

---

## 📦 OTA Self-Updates

The app fetches the latest GitHub Release tag on startup and compares it with the current `versionName`. If a newer version exists, users are prompted to download and install the updated APK directly.

---

## 📝 License

This project is private and custom-built for Shradha & Abhishek's wedding. All rights reserved.

---

## 🙏 Acknowledgments

- Built with ❤️ for Shradha & Abhishek
- UI powered by Jetpack Compose
- Backend by Firebase