# Expense Tracker

**Sub Heading:** A premium offline-first personal finance and expense tracking Android application.

**Role:** Native Android Developer, UI/UX Designer, Database Architect  
**Tech Stack:** Kotlin, Jetpack Compose, SQLite, Room DB, Kotlin Coroutines, StateFlow, Android Biometric SDK, Apache POI, Android PdfDocument, MVVM Architecture  
**Platform:** Mobile Application (Android App)  

## Overview
[Expense Tracker (v2.0.0)](file:///d:/expense-tracker/README.md) is a premium, offline-first personal finance and wealth management application developed natively for the Android platform. By running 100% locally and device-bound, it guarantees absolute privacy for the user's financial ledger. The application allows users to orchestrate their budgets, manage multiple accounts/wallets (like Cash, Credit Cards, Bank Accounts, and Savings), log transactions with an integrated arithmetic calculator, track loans and borrowing, and schedule recurring expenses or income. 

The application is built on modern Android development standards, utilizing Jetpack Compose for reactive, declarative layouts, Kotlin Coroutines & Flow for asynchronous stream handling, Room Database for persistent storage, and local biometric sensors for hardware-level app lock security.

## Inspiration
Modern personal finance applications heavily depend on cloud servers, requiring constant internet connectivity, exposing sensitive financial logs to data breaches, and forcing users to link their private bank credentials. Additionally, manual entry interfaces introduce high friction: standard numeric keypads force users to bounce back and forth to external calculator apps to compute shared bills or split costs, and fixed calendar budgets fail to align with real-world, non-monthly payroll cycles.

We were inspired to build a premium, offline-first personal finance and wealth management application that runs 100% locally and device-bound, guaranteeing absolute privacy for the user's financial ledger while eliminating standard UI frictions with smart offline utilities.

## What it does

[Expense Tracker (v2.0.0)](file:///d:/expense-tracker/README.md) is a feature-rich, local-first finance manager for Android. Key features include:
- **Dynamic Dashboard & Budget Controller:** High-visibility spending indicators displaying remaining funds relative to budget intervals (Weekly, Monthly, Custom) along with smooth, interactive 7-day spending line charts.
- **Multi-Wallet Account System:** Creation and customization of distinct financial pools (Cash, Credit Cards, Savings) with custom color hexes, bank names, starting balances, and icons. Balances automatically adjust during inter-account transfers.
- **Arithmetic Input Parser:** An inline math evaluation keyboard that parses arithmetic strings directly in the transaction amount field, displaying a live preview prior to confirmation.
- **Planned Transactions & Automation:** Cycle-based planners (Daily, Weekly, Monthly, Yearly) for fixed bills and recurring subscriptions. Automatically triggers income deposits in the background and highlights overdue bills in red.
- **Lending & Borrowing Ledger:** Track debts (what you owe) and dues (what others owe you) linked to contacts, with due dates and a toggle to log payments directly to the main transaction history.
- **Advanced Filtering & Tag Search:** Multi-criteria transaction history lookup enabling searches by description, category, tags, and date ranges.
- **Privacy Masking Mode:** One-tap toggle that masks balances and income (displaying them as `••••`) to prevent shoulder-surfing in public.
- **Hardware Biometric Lock:** Security integration leveraging fingerprint/facial recognition to lock financial logs.
- **Data Portability & Local Backups:** JSON and CSV export/import systems, Excel workbook creation using Apache POI, and native PDF invoice printing.

## Design Philosophy
The user interface is designed on top of modern Material Design 3 patterns, adopting a sleek design system tailored for readability, focus, and rapid input. It employs horizontal category scroll hubs and custom color codes to minimize cognitive overload when scanning financial categories. Modern glassmorphism card properties, dynamic list expansions, and responsive button states provide immediate tactile feedback. 

To maximize accessibility, the app adopts clean typography, high-contrast text fields, and soft-colored chips for tag organization. A key UX detail is the **Privacy Masking Toggle** which replaces numerical balances with blurred masking blocks, allowing users to log transactions discreetly in crowded public spaces.

## Technical Highlights
- **Declarative Compose UI:** Uses declarative Jetpack Compose UI structures, utilizing state lifting, reactive theme adapters, and state flows within screens like [HomeScreen.kt](file:///d:/expense-tracker/app/src/main/java/com/example/ui/screens/HomeScreen.kt) and components like [AddDebtDueDialog.kt](file:///d:/expense-tracker/app/src/main/java/com/example/ui/screens/AddDebtDueDialog.kt).
- **Architecture & ViewModel State Management:** Implements the MVVM architecture pattern. The [MainViewModel](file:///d:/expense-tracker/app/src/main/java/com/example/MainViewModel.kt) coordinates data flows from repositories to UI views using Kotlin `StateFlow` and structured coroutines, preventing state mismatch or memory leaks.
- **Local Persistence with Room DB:** Leverages [AppDatabase.kt](file:///d:/expense-tracker/app/src/main/java/com/example/data/AppDatabase.kt) to manage a relational SQLite schema consisting of `Expense`, `DebtDue`, `Account`, and `PlannedTransaction` entities.
- **Asynchronous Flow Collections:** Collects database records as live flows using coroutine channels (`SharingStarted.WhileSubscribed`), running automated events (such as background salary deposits on planned dates) without blocking UI rendering.
- **On-Device Biometric Lock:** Connects natively to the Android Biometric prompt library, linking local cryptographic keys to the device's hardware-backed keymaster (Keystore).
- **Client-Side Document Compilers:** Embeds Apache POI directly inside the compilation dependencies to generate Excel files locally, and utilizes Android's native `PdfDocument` API to compile visual transaction grids to PDF files without utilizing external servers.

## Challenges we ran into

- **Room Database Schema Migrations:** Implementing versioned SQLite migrations (supporting up to database version 6) without causing data loss or app crashes. Ensuring database updates perfectly transitioned existing tables to support multi-wallet relations and planned transactions.
- **Inline Arithmetic Input Parsing:** Designing a robust mathematical parser to run inside the Compose text field in real-time, handling edge cases such as incomplete syntax (e.g., `250 + `) and division by zero.
- **UI Performance and Flow Collection:** Optimizing asynchronous database collections with StateFlow to prevent UI jank during large transaction filters and date-range updates.
- **On-Device PDF and Excel Generation:** Compiling visual data into standard documents directly on the Android runtime without external servers, which required managing device storage permissions and memory limits.

## Accomplishments that we're proud of

- **100% Offline Integrity:** Creating a fully offline financial ledger that logs transactions instantaneously and provides absolute data privacy.
- **Integrated Arithmetic Keyboard Parser:** Delivering a frictionless transaction entry UI where users can split or calculate bills inline (e.g. typing `120 * 1.05 + 50` directly in the amount input).
- **Robust Schema Architecture:** Building a highly resilient local database schema with robust Room migrations.
- **Privacy Masking Mode:** Seamlessly integrating a UI masking toggle to hide numerical values with a single tap, protecting user privacy in public.

## What we learned

- **State Hoisting and Custom Layouts in Jetpack Compose:** Gained a deep understanding of reactive styling and component modularity.
- **Database Architecture and Migrations:** Mastered writing, testing, and debugging complex database migrations using Room DB and SQLite.
- **Local Document Printing:** Learned to utilize native Android printing classes and Apache POI to compile documents client-side.
- **Hardware-Level Security:** Integrated the biometric API and encryption keys safely via Android Keystore.

## What's next for Expense tracker

- **On-Device OCR Receipt Scanning:** Integrate Google ML Kit to extract transaction details from photo receipts locally.
- **Local P2P Backups:** Implement encrypted file backup/restore options via private cloud drives (e.g., user-managed Google Drive) or local network sync.
- **Predictive Spending Analytics:** Build lightweight on-device statistical models to predict next month's recurring expenses and forecast budget warnings based on historical spending.
