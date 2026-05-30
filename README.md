# Hela Smart SACCO — Android App

Kotlin/Jetpack Compose rewrite of the Hela Smart SACCO Python/Kivy app.

## Project Structure

```
app/src/main/java/com/helasacco/app/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── entities/     # Room entities + mappers
│   │   └── HelaSaccoDB.kt
│   └── repository/       # Repository implementations
├── di/                   # Hilt DI modules + SessionManager
├── domain/model/         # Domain models + enums
└── ui/
    ├── ai/               # AI Assistant (Claude API)
    ├── admin/            # KYC, Notifications, Settings
    ├── common/           # Shared composables + utilities
    ├── dashboard/        # Main dashboard
    ├── investments/      # Investment portfolio
    ├── loans/            # Loan list, detail, application
    ├── login/            # Login screen
    ├── members/          # Member list, detail, registration
    ├── navigation/       # Routes + bottom nav
    ├── reports/          # Reports dashboard
    ├── theme/            # Material 3 theme + colors
    └── transactions/     # Deposit, Withdrawal, Transfer
```

## Setup

### 1. Open in Android Studio
File → New → Import Project → select the `HelaSacco-Complete/` folder.

### 2. Add API key for AI Assistant
Create or edit `local.properties` in the project root:
```
ANTHROPIC_API_KEY=sk-ant-your-key-here
```

### 3. Seed initial admin user
On first launch the database is empty. Run this once in a test or seed script:
```kotlin
// In a coroutine, inject AuthRepository and call:
authRepository.createUser(
    username = "admin",
    password = "Admin@123",
    role = UserRole.SUPER_ADMIN,
    fullName = "System Administrator",
    branchId = null,
)
```

### 4. Build & Run
Select a device/emulator and press Run (Shift+F10).

## Features Implemented

| Phase | Features |
|-------|----------|
| 1 | Room DB (40+ tables), Hilt DI, Material 3 theme, repositories, session management |
| 2 | Login (PBKDF2 auth, lockout), Dashboard (role-aware stats, quick actions, recent transactions) |
| 3 | Member list/search/registration (4-step), Member profile, Deposit/Withdrawal/Transfer with receipts, Loan list/detail/application/schedule |
| 4 | KYC approval workflow, Notifications, Settings (theme, biometric, logout) |
| 5 | Reports (Overview/Members/Loans/Savings tabs), AI Assistant (Claude API), Investments portfolio |

## Notes

- **Currency**: All amounts stored as minor units (cents). `Long.minorToKes()` formats to KES.
- **AI Assistant**: Requires `ANTHROPIC_API_KEY` in `local.properties`. Uses `claude-sonnet-4-20250514`.
- **Offline-first**: All data lives in Room. Cloud sync can be added as a background worker.
- **M-Pesa**: Integration point is in `TransactionViewModel` — add Daraja STK Push calls before `transactionRepository.save()`.
