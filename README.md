# Expense Tracker (v3.7)

[📥 **Download Latest Release APK**](app/build/outputs/apk/release/app-release.apk)

A modern, offline-first personal finance and expense tracking Android application. Designed with Jetpack Compose, this application provides users with comprehensive budget controls, real-time analytics, secure biometric authentication, and data export/import utilities.

---

## Key Features

- **Transaction Management**: Effortlessly log, categorize, and track daily expenses.
- **Budget Tracking & Alerts**: Set a custom monthly budget. The app dynamically tracks spending against this limit and alerts you via local system notifications when you exceed 80% or 100% of your budget.
- **Smart Push Notifications**: Automated daily push reminders to keep you on budget without instant logging spam:
  - **8:30 AM**: Previous day transactions breakdown by category.
  - **11:00 PM**: Today-vs-yesterday comparison report with smart budget suggestions.
- **Data Insights & Analytics**: Visual charts built via Jetpack Compose Canvas (Line and Bar charts) summarizing category breakdowns. Features a **Week | Month** toggle to easily switch spending trend views.
- **Interactive Filtering**: Filter recent transaction logs directly from the home tab by **All**, **Today**, **This Week**, or **This Month**.
- **Biometric Security**: Protect sensitive financial records with fingerprint authentication.
- **Data Portability**: Import and export transaction history:
  - **Excel Import/Export**: Import from templates using a clean, responsive Excel Import Guide table. Export to Excel (.xlsx).
  - **PDF Export**: Generate and download formatted PDF logs of your transaction history.
- **Wipe & Confirmation Guards**: Added confirmation dialogs for deletes and data wipes to avoid accidental data loss.

---

## Technical Architecture

The application is built on top of robust Android development frameworks and architecture guidelines:
- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose for declarative and smooth UI rendering
- **Architecture Pattern**: MVVM (Model-View-ViewModel) separating logic, UI state, and database operations
- **Reactive Streams**: StateFlow and Coroutines for lifecycle-aware asynchronously updated UI data
- **Local Persistence**: SQLite database wrapper utilizing Room DB
- **Security**: Android Biometric Library for hardware-level secure lockscreen implementation
- **Excel/PDF Generation**: Apache POI for spreadsheet handling, and Android native `PdfDocument` for lightweight PDF creation without external library bloat

---

## Getting Started

### Prerequisites
- [Android Studio Jellyfish](https://developer.android.com/studio) or newer
- JDK 17+
- Android SDK 26 (Android 8.0 Oreo) or newer (Minimum API level)

### Installation & Execution
1. Clone or download this project to your local drive.
2. Open Android Studio, select **Open**, and navigate to the directory containing this project.
3. Allow Android Studio to complete Gradle sync and project build configuration tasks.
4. Build and run the app on an Android Emulator or a physical device.

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
