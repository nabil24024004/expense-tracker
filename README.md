# Expense Tracker (v5.0) 📱💰

[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/Compose-Jetpack-blue.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Room-v2.6.0-green.svg)](https://developer.android.com/training/data-storage/room)
[![Security](https://img.shields.io/badge/Biometric-Secured-darkgreen.svg)](https://developer.android.com/training/sign-in/biometrics)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A modern, offline-first personal finance and expense tracking Android application. Designed with Jetpack Compose, this app provides comprehensive budget controls, multi-account wallets, recurring payment planning, mathematical amount parsing, tag search, real-time analytics, biometric security, and full data export/import utilities.

📥 [**Download Latest Release APK**](file:///d:/expense-tracker/app/build/outputs/apk/release/app-release.apk)

---

## 🌟 Core App Features

### 1. Dashboard Overview & Budgeting
* **Remaining Budget Tracker**: A high-visibility progress indicator showing spent vs. remaining funds relative to your selected budget period (Weekly, Monthly, or Custom range).
* **Dynamic Spending Trends**: A smooth, interactive 7-day spending line chart to visualize cash outflows.
* **Quick Spend Hub**: Horizontal scrolling category shortcuts (Food, Shopping, Travel, Bills, etc.) to log standard transactions with a single tap.
* **Quick Insights**: Smart cards highlighting spend alerts, budget overruns, and general saving advice.

### 2. Multi-Account & Wallet System
* **Custom Wallets**: Create and manage distinct accounts representing Cash, Bank Accounts, Credit Cards, or Savings Accounts.
* **Personalized Cards**: Assign custom names, type icons, starting balances, and color hexes to match your physical cards.
* **Inter-Account Transfers**: Record fund transfers between wallets (e.g., ATM withdrawals, credit card payments), automatically recalculating both account balances.
* **Net Balance Exclusions**: Toggle accounts (like high-yield savings) to be excluded from your primary dashboard "Total Balance".

### 3. Smart Transaction Logging
* **Math Calculator Keyboard**: Write equations directly in the amount field (e.g., `250 + 120 * 1.05`) with a live calculation preview before saving.
* **Hashtag Multi-Tagging**: Add custom tags to transactions (e.g., `#vacation`, `#client-x`) separate from categories, facilitating cross-category searching.
* **Transaction Types**: Label logs as **Expense** (reduces wallet balance) or **Income** (adds to wallet balance).

### 4. Planned & Recurring Payments
* **Subscription & Bill Scheduler**: Automate recurring cycles (Daily, Weekly, Monthly, Yearly) or schedule one-time future payments.
* **Overdue Highlights**: Highlights upcoming or missed cycles on your dashboard in red.
* **Pay / Skip Actions**: Tap **Pay** to automatically log the transaction to its wallet and schedule the next cycle, or tap **Skip** to advance the schedule without logging a transaction.

### 5. Debts & Receivables
* **Lending & Borrowing Ledger**: Track debts (what you owe) and dues (what others owe you) with person names and due dates.
* **Integrated Settlement**: Record full or partial repayments with a toggle to automatically post corresponding income/expense transactions in your ledger.

### 6. Ledger & History Controls
* **Search & Grouping**: Search logs by title, category, or tag. Group records by Week, Month, or Year with calculated subtotals.
* **Advanced Filters**: Filter lists by transaction type, category, date range (with custom calendar ranges), and sorting order.
* **Visual Badges**: Every transaction card displays its category icon, associated wallet label (in its custom color), and tag chips.

### 7. Privacy, Security & Portability
* **Privacy Masking Toggle**: Hide balances and income on the dashboard and logs (displayed as `••••`) when using the app in public.
* **Biometric Lock**: Protect sensitive financial records with secure fingerprint or face unlock.
* **Data Portability**: Export your transaction ledger to JSON or CSV, or import external files to restore backups.

---

## 💡 Guidelines to Achieve 100% Efficiency

Follow these best practices to get maximum utility and automation out of your personal finance tracker:

### 1. Map Out All Wallets and Start Balances
Do not use the app as a single "Cash" ledger. Create separate wallets for your physical cash, payroll bank account, credit card, and savings. By tracking inter-account transfers (like credit card payments or cash withdrawals), your net worth calculations remain accurate without throwing off your category-based budgets.

### 2. Automate Constant Expenses with Scheduled Cycles
Schedule your fixed bills, recurring subscriptions (Netflix, Spotify, gym), and salary dates under **Planned Payments**. Instead of manual entry, you will receive overdue indicators on your dashboard. Tapping **Pay** instantly logs them with the correct wallet, category, and amount, cutting down log times by 90%.

### 3. Master Hashtag Tags for Events or Projects
Use Categories for *what* the item is (e.g., Food, Transport) and Hashtags for *why* it happened (e.g., `#SummerTrip2026`, `#OfficeSetup`). Under the Ledger tab, search for the hashtag to see the exact cost of an event or project across multiple categories, giving you a distinct view of holiday or client-project expenses.

### 4. Use the Amount Calculator for Shared Expenses
When buying items in bulk or splitting bills, use the math evaluator inside the amount field (e.g., `4500 / 3` or `1200 + 450 + 900`). This eliminates the need to switch back and forth between a separate calculator app and the tracker.

### 5. Align Budget Cycles to Your Payroll
If you get paid on a non-calendar schedule (e.g., every 15th of the month or bi-weekly), set a **Custom Budget Period** instead of the default calendar month. This aligns your "Remaining Balance" directly with your active cash flow, ensuring you don't overspend during the final week of a cycle.

### 6. Toggle Privacy Mode in Public
Keep the **Hide Balance** and **Hide Income** toggles enabled in Settings. This allows you to log transactions on the go in public transport, restaurants, or cafes without revealing your net worth or monthly earnings to shoulder-surfers.

### 7. Bridge Debt Repayment to Your Ledger
When clearing debts in the Debts section, always keep the **Log as Transaction** toggle checked. This automatically adjusts your account balances and updates your budget records so that debt settlements are accounted for in your monthly savings rate.

---

## 🛠️ Technical Architecture

The application is built on top of robust Android development frameworks and architecture guidelines:
* **Language**: 100% Kotlin
* **UI Framework**: Jetpack Compose for declarative and smooth UI rendering
* **Architecture Pattern**: MVVM (Model-View-ViewModel) separating logic, UI state, and database operations
* **Reactive Streams**: StateFlow and Coroutines for lifecycle-aware asynchronously updated UI data
* **Local Persistence**: SQLite database wrapper utilizing Room DB (Schema Version `4`)
* **Security**: Android Biometric Library for hardware-level secure lockscreen implementation
* **Excel/PDF Generation**: Apache POI for spreadsheet handling, and Android native `PdfDocument` for lightweight PDF creation without external library bloat

---

## 🚀 Getting Started

### Prerequisites
* [Android Studio Jellyfish](https://developer.android.com/studio) or newer
* JDK 17+
* Android SDK 26 (Android 8.0 Oreo) or newer (Minimum API level)

### Installation & Execution
1. Clone or download this project to your local drive.
2. Open Android Studio, select **Open**, and navigate to the directory containing this project.
3. Allow Android Studio to complete Gradle sync and project build configuration tasks.
4. Build and run the app on an Android Emulator or a physical device.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
