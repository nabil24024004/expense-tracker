# Expense Tracker (v5.0)

A gorgeous, privacy-respecting, offline-first personal finance and group expense tracking Android application. Built with **Jetpack Compose** and modern Android development best practices, it provides complete financial control, smart local notifications, advanced analytics, and seamless group expense splitting without compromising your data.

---

## 🌟 Key Features

### 💵 Core Personal Finance
- **Transaction Management**: Effortlessly log, categorize, and track daily expenses.
- **Debts & Receivables Tracking**: Easily log and track outstanding liabilities (money you owe to others and money others owe to you). Includes optional repayment integration where clearing a debt can automatically log a transaction expense under *"Debt Repayment"*.
- **Budget Tracking & Alerts**: Set a custom monthly budget. The app dynamically tracks spending against this limit and alerts you via local system notifications when you exceed 80% or 100% of your budget.
- **Interactive Filtering**: Filter recent transaction and liability logs directly by **All**, **Today**, **This Week**, or **This Month**.
- **Biometric Security**: Protect sensitive financial records with fingerprint authentication.

### 👥 Group Expenses & Split Management (New!)
- **Group Shared Ledgers**: Create groups, add members, and log shared group expenses.
- **Flexible Split Methods**: Split expenses equally, by exact amounts, percentages, or shares using a robust split calculator.
- **Clickable Balance Bubbles**: Balance bubbles dynamically transition using responsive spring physics and are positioned inside the **Expenses** tab. Click any bubble to instantly jump to the **Balances** tab for settlement details.

### 📊 Smart Alerts & Analytics
- **Smart Push Notifications**: Automated daily push reminders to keep you on budget without instant logging spam:
  - **8:30 AM**: Previous day transactions breakdown by category.
  - **11:00 PM**: Today-vs-yesterday comparison report with smart budget suggestions.
- **Liability & Spending Analytics**: Redesigned Stats page featuring:
  - **Spending Trends**: Category breakdown and weekly/monthly trends.
  - **Liability Position**: Displays Net Position (+Receivable/-Payable), Debt-to-Receivable composition ratio, settlement progress indicators, overdue warning alerts, and top partners breakdown.

### 🔒 100% Offline-First & Privacy-Focused (New!)
- **Zero Cloud Accounts**: Removed all Google, Facebook, and email sign-in requirements. All data is stored 100% locally on your device for absolute privacy.
- **Wave-Pattern Onboarding**: A clean, distraction-free startup flow with initial profile setup (name, monthly budget, and biometrics toggle).
- **Quiet Workspace**: No startup notifications or spammy welcome cards. Just open the app and start managing your finances.

### 📸 Scrapbook Food Diary & Portability (New!)
- **Polaroid-Style Scrapbook PDF**: Export your food diary into a beautiful scrapbook collage featuring warm cream backgrounds, hand-drawn star/heart doodles, Polaroid photo containers, washi-tape headers, and drop-shadowed tags.
- **Unified Data Portability**: Consolidated Excel and PDF utilities that export all transaction logs, debts, and receivables together.

### 🎨 Premium Dynamic Themes & High Contrast (New!)
- **Curated Color Palettes**: Choose from 6 beautifully named themes:
  - **Classic** (Sophisticated Red)
  - **Ocean Blue** (Sleek Dark Blue)
  - **Green Tea** (Calming Sage Green)
  - **Sunset** (Warm Orange/Purple)
  - **Grapefruit** (Vibrant Purple/Orange)
  - **Bubblegum** (Playful Pink)
- **Enhanced Dark Mode Readability**: Refactored dark palettes to use high-contrast text (`#FFFFFF` / `#B0B0B0`) for perfect legibility in low light.
- **High-Contrast Bottom Navigation Bar**: Selected tab items leverage the active theme's `PrimaryAccent` highlight and `DarkCardTextPrimary` styling to ensure perfect readability.

### ⌨️ Keyboard & Navigation Safety (New!)
- **Pinned Bottom Sheets**: Buttons are moved out of the scrolling container and pinned to the bottom of forms (`AddEntryDialog` and `AddGroupExpenseScreen`).
- **Zero Keyboard Overlap**: Combined with `imePadding()` and `navigationBarsPadding()` to guarantee save buttons never hide under system navigation keys or the soft keyboard.
- **Scroll Performance**: Optimized view caching and stable item keys for fluid list scrolling.
- **Wipe & Confirmation Guards**: Added clear confirmation dialogs for deletions and data resets.

---

## 🛠️ Technical Architecture

The application is built on top of modern Android components:
- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Declarative & Fluid UI rendering)
- **Architecture Pattern**: MVVM (Model-View-ViewModel) separating UI, Business Logic, and Database layers
- **Reactive Streams**: Kotlin Coroutines & StateFlow for lifecycle-aware state updates
- **Local Database**: Room DB (Robust SQLite object-mapping wrapper)
- **Security**: Hardware-level Biometric Authentication library
- **Data Export Utilities**: Apache POI (Excel generation) and Android native `PdfDocument` (lightweight PDF exports)

---

## 📦 Getting Started

### Prerequisites
- [Android Studio Jellyfish](https://developer.android.com/studio) or newer
- JDK 17+
- Android SDK 26 (Android 8.0 Oreo) or newer (Minimum API level)

### Installation & Execution
1. Clone or download this repository:
   ```bash
   git clone https://github.com/example/expense-tracker.git
   ```
2. Open Android Studio and select **Open**, selecting the directory of this project.
3. Allow Gradle to complete dependency sync and build configuration.
4. Connect an Android Emulator or physical test device and run the project.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
