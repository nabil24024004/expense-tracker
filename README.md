# Expense Tracker (v3.5)

A modern, offline-first personal finance and expense tracking Android application. Designed with Jetpack Compose, this application provides users with comprehensive budget controls, real-time analytics, secure biometric authentication, and data export/import utilities.

---

## Key Features

- **Transaction Management**: Effortlessly log, categorize, and track daily expenses.
- **Budget Tracking & Alerts**: Set a custom monthly budget. The app dynamically tracks spending against this limit and alerts you via local system notifications when you exceed 80% or 100% of your budget.
- **Data Insights & Analytics**: Visual graphs built via Jetpack Compose Canvas (Line and Bar charts) summarizing category breakdowns and daily spending trends.
- **Biometric Security**: Protect sensitive financial records with fingerprint authentication.
- **Data Portability**: Import and export transaction history directly to Excel files utilizing Apache POI.
- **Unified Settings**: Adjust user profiles, budgets, biometric preferences, and visual themes seamlessly.

---

## Technical Architecture

The application is built on top of robust Android development frameworks and architecture guidelines:
- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose for declarative and smooth UI rendering
- **Architecture Pattern**: MVVM (Model-View-ViewModel) separating logic, UI state, and database operations
- **Reactive Streams**: StateFlow and Coroutines for lifecycle-aware asynchronously updated UI data
- **Local Persistence**: SQLite database wrapper utilizing Room DB
- **Security**: Android Biometric Library for hardware-level secure lockscreen implementation
- **Excel Handling**: Apache POI library for reading/writing spreadsheets

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
