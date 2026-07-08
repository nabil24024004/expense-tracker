# Expense Tracker

**Sub Heading:** A premium offline-first personal finance and expense tracking Android application.

**Role:** Native Android Developer, UI/UX Designer, Database Architect  
**Tech Stack:** Kotlin, Jetpack Compose, SQLite, Room DB, Kotlin Coroutines, StateFlow, Android Biometric SDK, Apache POI, Android PdfDocument, MVVM Architecture  
**Platform:** Mobile Application (Android App)  

## Overview
[Expense Tracker (v5.0.0)](file:///d:/expense-tracker/README.md) is a premium, offline-first personal finance and wealth management application developed natively for the Android platform. By running 100% locally and device-bound, it guarantees absolute privacy for the user's financial ledger. The application allows users to orchestrate their budgets, manage multiple accounts/wallets (like Cash, Credit Cards, Bank Accounts, and Savings), log transactions with an integrated arithmetic calculator, track loans and borrowing, and schedule recurring expenses or income. 

The application is built on modern Android development standards, utilizing Jetpack Compose for reactive, declarative layouts, Kotlin Coroutines & Flow for asynchronous stream handling, Room Database for persistent storage, and local biometric sensors for hardware-level app lock security.

## The Problems and Solutions
**Problem:**  
Modern personal finance applications heavily depend on cloud servers, requiring constant internet connectivity, exposing sensitive financial logs to data breaches, and forcing users to link their private bank credentials. Additionally, manual entry interfaces introduce high friction: standard numeric keypads force users to bounce back and forth to external calculator apps to compute shared bills or split costs, and fixed calendar budgets fail to align with real-world, non-monthly payroll cycles.

**Solution:**  
Developed a native Android app that runs entirely offline, storing data locally inside a secured SQLite database. The project introduces an integrated arithmetic compiler directly inside the transaction input keyboard (enabling operations like `250 + 120 * 1.05`), allowing users to split and compute expenses inline. The application supports custom budget periods (Weekly, Monthly, or Custom Date Ranges) that sync with the user's real payroll cycles, and protects all sensitive data behind local biometric authentication and a fast screen-masking privacy mode.

## Key Features
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
- **Local Persistence with Room DB Schema Migrations:** Leverages [AppDatabase.kt](file:///d:/expense-tracker/app/src/main/java/com/example/data/AppDatabase.kt) to manage a relational SQLite schema consisting of `Expense`, `DebtDue`, `Account`, and `PlannedTransaction` entities. Features robust migration paths (supporting up to database version 6) to guard user data when modifying schemas.
- **Asynchronous Flow Collections:** Collects database records as live flows using coroutine channels (`SharingStarted.WhileSubscribed`), running automated events (such as background salary deposits on planned dates) without blocking UI rendering.
- **On-Device Biometric Lock:** Connects natively to the Android Biometric prompt library, linking local cryptographic keys to the device's hardware-backed keymaster (Keystore).
- **Client-Side Document Compilers:** Embeds Apache POI directly inside the compilation dependencies to generate Excel files locally, and utilizes Android's native `PdfDocument` API to compile visual transaction grids to PDF files without utilizing external servers.

## Project Outcome
The application succeeds in delivering a fully offline personal finance management system. By eliminating the necessity of web servers, transaction logging is instantaneous. Users gain absolute visibility over their actual net worth, track outstanding liabilities, and avoid overspending via alerts. The project proves that high-performance, complex utility apps can be written entirely client-side, reducing security risks to zero.

## What Makes it Different
Unlike typical commercial money trackers that sell user data, track locations, or display disruptive third-party advertisements, Expense Tracker is built strictly offline-first. Features like the mathematical syntax parser inside the amount input field, customizable budget timelines tied to real income cycles instead of calendar months, inter-account transfers, and native Excel/PDF compilation make it a secure power-user tool.
