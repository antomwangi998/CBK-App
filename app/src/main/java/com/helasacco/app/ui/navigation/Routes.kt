package com.helasacco.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// ── Route constants ───────────────────────────────────────────────────────────

object Routes {
    // Auth
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val CHANGE_PASSWORD = "change_password"

    // Main
    const val DASHBOARD = "dashboard"

    // Members
    const val MEMBER_LIST = "members"
    const val MEMBER_DETAIL = "members/{memberId}"
    const val MEMBER_NEW = "members/new"
    const val MEMBER_EDIT = "members/{memberId}/edit"

    // Accounts
    const val ACCOUNT_DETAIL = "accounts/{accountId}"

    // Transactions
    const val TRANSACTION_LIST = "transactions"
    const val TRANSACTION_DEPOSIT = "transactions/deposit"
    const val TRANSACTION_WITHDRAWAL = "transactions/withdrawal"
    const val TRANSACTION_TRANSFER = "transactions/transfer"
    const val TRANSACTION_DETAIL = "transactions/{transactionId}"

    // Loans
    const val LOAN_LIST = "loans"
    const val LOAN_DETAIL = "loans/{loanId}"
    const val LOAN_NEW = "loans/new"
    const val LOAN_REPAYMENT = "loans/{loanId}/repayment"
    const val LOAN_SCHEDULE = "loans/{loanId}/schedule"

    // Admin
    const val KYC_APPROVAL = "admin/kyc"
    const val ADMIN_MEMBERS = "admin/members"
    const val BRANCHES = "admin/branches"
    const val AUDIT_LOG = "admin/audit"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    // Reports
    const val REPORTS = "reports"

    // AI
    const val AI_ASSISTANT = "ai"

    // Investments
    const val INVESTMENTS = "investments"
    const val CUSTOMER_REGISTER = "register"

    // Helper builders
    fun memberDetail(id: String) = "members/$id"
    fun memberEdit(id: String) = "members/$id/edit"
    fun accountDetail(id: String) = "accounts/$id"
    fun transactionDetail(id: String) = "transactions/$id"
    fun loanDetail(id: String) = "loans/$id"
    fun loanRepayment(id: String) = "loans/$id/repayment"
    fun loanSchedule(id: String) = "loans/$id/schedule"
}
