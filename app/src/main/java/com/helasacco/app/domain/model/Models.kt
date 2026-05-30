package com.helasacco.app.domain.model

// ── Enumerations (mirrors models.py) ────────────────────────────────────────

enum class UserRole(val value: String) {
    ADMIN("admin"),
    SUPER_ADMIN("super_admin"),
    TELLER("teller"),
    SENIOR_TELLER("senior_teller"),
    LOANS_OFFICER("loans_officer"),
    SENIOR_LOANS_OFFICER("senior_loans_officer"),
    MANAGER("manager"),
    BRANCH_MANAGER("branch_manager"),
    AUDITOR("auditor"),
    FIELD_OFFICER("field_officer"),
    CREDIT_ANALYST("credit_analyst"),
    ACCOUNTANT("accountant"),
    MEMBER("member"),
    AGENT("agent");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: MEMBER
    }
}

enum class TransactionType(val value: String) {
    DEPOSIT("deposit"),
    WITHDRAWAL("withdrawal"),
    TRANSFER("transfer"),
    LOAN_DISBURSEMENT("loan_disbursement"),
    LOAN_REPAYMENT("loan_repayment"),
    SHARE_PURCHASE("share_purchase"),
    SHARE_SALE("share_sale"),
    DIVIDEND("dividend"),
    INTEREST("interest"),
    FEE("fee"),
    PENALTY("penalty"),
    CHARGE("charge"),
    ADJUSTMENT("adjustment"),
    REVERSAL("reversal"),
    STANDING_ORDER("standing_order"),
    BULK_PAYMENT("bulk_payment"),
    MOBILE_MONEY("mobile_money"),
    BANK_TRANSFER("bank_transfer"),
    CHEQUE_DEPOSIT("cheque_deposit"),
    CHEQUE_WITHDRAWAL("cheque_withdrawal");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: DEPOSIT
    }
}

enum class AccountType(val value: String) {
    SAVINGS("savings"),
    CURRENT("current"),
    FIXED_DEPOSIT("fixed_deposit"),
    SHARE_CAPITAL("share_capital"),
    LOAN("loan"),
    SUSPENSE("suspense"),
    JOINT("joint"),
    CHILDREN_SAVINGS("children_savings"),
    RETIREMENT("retirement"),
    EDUCATION("education"),
    HOLIDAY("holiday"),
    EMERGENCY("emergency");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: SAVINGS
    }
}

enum class LoanStatus(val value: String) {
    PENDING("pending"),
    APPRAISAL("appraisal"),
    COMMITTEE("committee"),
    APPROVED("approved"),
    REJECTED("rejected"),
    DISBURSED("disbursed"),
    ACTIVE("active"),
    CLOSED("closed"),
    WRITTEN_OFF("written_off"),
    RESCHEDULED("rescheduled"),
    RESTRUCTURED("restructured"),
    SUSPENDED("suspended");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

enum class KycStatus(val value: String) {
    PENDING("pending"),
    SUBMITTED("submitted"),
    UNDER_REVIEW("under_review"),
    APPROVED("approved"),
    REJECTED("rejected"),
    EXPIRED("expired");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

enum class SyncStatus(val value: String) {
    PENDING("pending"),
    SYNCED("synced"),
    FAILED("failed"),
    CONFLICT("conflict");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

// ── Domain models ────────────────────────────────────────────────────────────

data class User(
    val id: String,
    val username: String,
    val role: UserRole,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val branchId: String?,
    val isActive: Boolean,
    val isLocked: Boolean,
    val lastLogin: String?,
    val memberId: String?,
)

data class Member(
    val id: String,
    val memberNo: String,
    val branchId: String?,
    val firstName: String,
    val lastName: String,
    val otherNames: String?,
    val idNumber: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val city: String?,
    val county: String?,
    val occupation: String?,
    val employer: String?,
    val mpesaNumber: String?,
    val kycStatus: KycStatus,
    val isActive: Boolean,
    val membershipDate: String,
    val profilePhotoPath: String?,
    val createdAt: String,
) {
    val fullName: String get() = listOfNotNull(firstName, otherNames, lastName).joinToString(" ")
    val initials: String get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
}

data class Account(
    val id: String,
    val accountNo: String,
    val memberId: String,
    val branchId: String?,
    val accountType: AccountType,
    val currency: String,
    val status: String,
    val balanceMinor: Long,         // stored in minor units (cents)
    val availableBalanceMinor: Long,
    val interestRate: Double,
    val openingDate: String,
    val lastTransactionDate: String?,
) {
    /** Balance in major currency units (KES) */
    val balance: Double get() = balanceMinor / 100.0
    val availableBalance: Double get() = availableBalanceMinor / 100.0
}

data class Transaction(
    val id: String,
    val transactionNo: String,
    val accountId: String,
    val memberId: String?,
    val branchId: String?,
    val transactionType: TransactionType,
    val amountMinor: Long,
    val balanceAfterMinor: Long,
    val currency: String,
    val description: String?,
    val reference: String?,
    val status: String,
    val processedBy: String?,
    val createdAt: String,
    val valueDate: String?,
) {
    val amount: Double get() = amountMinor / 100.0
    val balanceAfter: Double get() = balanceAfterMinor / 100.0
}

data class Loan(
    val id: String,
    val loanNo: String,
    val memberId: String,
    val accountId: String?,
    val branchId: String?,
    val productId: String?,
    val principalMinor: Long,
    val outstandingMinor: Long,
    val interestRate: Double,
    val termMonths: Int,
    val status: LoanStatus,
    val disbursementDate: String?,
    val maturityDate: String?,
    val nextPaymentDate: String?,
    val nextPaymentMinor: Long,
    val createdAt: String,
) {
    val principal: Double get() = principalMinor / 100.0
    val outstanding: Double get() = outstandingMinor / 100.0
    val nextPayment: Double get() = nextPaymentMinor / 100.0
}

data class Branch(
    val id: String,
    val code: String,
    val name: String,
    val location: String?,
    val city: String?,
    val county: String?,
    val phone: String?,
    val isActive: Boolean,
    val isHeadOffice: Boolean,
)

data class Notification(
    val id: String,
    val userId: String?,
    val memberId: String?,
    val title: String,
    val message: String,
    val notificationType: String,
    val isRead: Boolean,
    val priority: Int,
    val createdAt: String,
)

// ── Result wrapper ────────────────────────────────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    fun getOrNull(): T? = if (this is Success) data else null
    fun errorMessage(): String? = if (this is Error) message else null
}
