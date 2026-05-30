package com.helasacco.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Users ────────────────────────────────────────────────────────────────────

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val salt: String,
    val iterations: Int = 600000,
    val role: String,
    @ColumnInfo(name = "full_name") val fullName: String? = null,
    val email: String? = null,
    @ColumnInfo(name = "email_encrypted") val emailEncrypted: String? = null,
    val phone: String? = null,
    @ColumnInfo(name = "phone_encrypted") val phoneEncrypted: String? = null,
    @ColumnInfo(name = "id_number") val idNumber: String? = null,
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Int = 1,
    @ColumnInfo(name = "is_locked") val isLocked: Int = 0,
    @ColumnInfo(name = "failed_attempts") val failedAttempts: Int = 0,
    @ColumnInfo(name = "locked_until") val lockedUntil: String? = null,
    @ColumnInfo(name = "last_login") val lastLogin: String? = null,
    @ColumnInfo(name = "last_activity") val lastActivity: String? = null,
    @ColumnInfo(name = "session_token") val sessionToken: String? = null,
    @ColumnInfo(name = "session_expires") val sessionExpires: String? = null,
    @ColumnInfo(name = "two_factor_enabled") val twoFactorEnabled: Int = 0,
    @ColumnInfo(name = "biometric_enabled") val biometricEnabled: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
    val version: Int = 1,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "synced",
    @ColumnInfo(name = "member_id") val memberId: String? = null,
)

// ── Branches ─────────────────────────────────────────────────────────────────

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val city: String? = null,
    val county: String? = null,
    @ColumnInfo(name = "postal_code") val postalCode: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @ColumnInfo(name = "manager_id") val managerId: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Int = 1,
    @ColumnInfo(name = "is_head_office") val isHeadOffice: Int = 0,
    @ColumnInfo(name = "parent_branch_id") val parentBranchId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "synced",
)

// ── Members ───────────────────────────────────────────────────────────────────

@Entity(
    tableName = "members",
    indices = [
        Index("member_no", unique = true),
        Index("phone"),
        Index("id_number"),
        Index("branch_id"),
        Index("kyc_status"),
        Index("is_active"),
    ]
)
data class MemberEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "member_no") val memberNo: String,
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "group_id") val groupId: String? = null,
    @ColumnInfo(name = "referrer_id") val referrerId: String? = null,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "other_names") val otherNames: String? = null,
    @ColumnInfo(name = "full_name_search") val fullNameSearch: String? = null,
    @ColumnInfo(name = "id_number") val idNumber: String? = null,
    @ColumnInfo(name = "id_number_encrypted") val idNumberEncrypted: String? = null,
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    @ColumnInfo(name = "marital_status") val maritalStatus: String? = null,
    val nationality: String? = "Kenyan",
    val phone: String? = null,
    @ColumnInfo(name = "phone_encrypted") val phoneEncrypted: String? = null,
    val email: String? = null,
    @ColumnInfo(name = "email_encrypted") val emailEncrypted: String? = null,
    val address: String? = null,
    val city: String? = null,
    val county: String? = null,
    @ColumnInfo(name = "postal_code") val postalCode: String? = null,
    val occupation: String? = null,
    val employer: String? = null,
    @ColumnInfo(name = "employment_type") val employmentType: String? = null,
    @ColumnInfo(name = "monthly_income") val monthlyIncome: Double? = null,
    @ColumnInfo(name = "bank_account_number") val bankAccountNumber: String? = null,
    @ColumnInfo(name = "bank_name") val bankName: String? = null,
    @ColumnInfo(name = "mpesa_number") val mpesaNumber: String? = null,
    @ColumnInfo(name = "kyc_status") val kycStatus: String = "pending",
    @ColumnInfo(name = "kyc_score") val kycScore: Int = 0,
    @ColumnInfo(name = "risk_score") val riskScore: Int = 0,
    @ColumnInfo(name = "risk_category") val riskCategory: String = "low",
    @ColumnInfo(name = "is_active") val isActive: Int = 1,
    @ColumnInfo(name = "is_dormant") val isDormant: Int = 0,
    @ColumnInfo(name = "membership_date") val membershipDate: String,
    @ColumnInfo(name = "membership_fee_paid") val membershipFeePaid: Int = 0,
    @ColumnInfo(name = "next_of_kin_name") val nextOfKinName: String? = null,
    @ColumnInfo(name = "next_of_kin_phone") val nextOfKinPhone: String? = null,
    @ColumnInfo(name = "profile_photo_path") val profilePhotoPath: String? = null,
    @ColumnInfo(name = "consent_signed") val consentSigned: Int = 0,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
    val version: Int = 1,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "pending",
)

// ── Accounts ──────────────────────────────────────────────────────────────────

@Entity(
    tableName = "accounts",
    indices = [Index("account_no", unique = true), Index("member_id"), Index("account_type")]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_no") val accountNo: String,
    @ColumnInfo(name = "member_id") val memberId: String,
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "product_id") val productId: String? = null,
    @ColumnInfo(name = "account_type") val accountType: String,
    @ColumnInfo(name = "account_subtype") val accountSubtype: String? = null,
    val currency: String = "KES",
    val status: String = "active",
    @ColumnInfo(name = "balance_minor") val balanceMinor: Long = 0,
    @ColumnInfo(name = "available_balance_minor") val availableBalanceMinor: Long = 0,
    @ColumnInfo(name = "blocked_amount_minor") val blockedAmountMinor: Long = 0,
    @ColumnInfo(name = "overdraft_limit_minor") val overdraftLimitMinor: Long = 0,
    @ColumnInfo(name = "interest_rate") val interestRate: Double = 0.0,
    @ColumnInfo(name = "interest_accrued_minor") val interestAccruedMinor: Long = 0,
    @ColumnInfo(name = "opening_date") val openingDate: String,
    @ColumnInfo(name = "closing_date") val closingDate: String? = null,
    @ColumnInfo(name = "last_transaction_date") val lastTransactionDate: String? = null,
    @ColumnInfo(name = "sms_alert_enabled") val smsAlertEnabled: Int = 1,
    @ColumnInfo(name = "email_alert_enabled") val emailAlertEnabled: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
    val version: Int = 1,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "pending",
)

// ── Transactions ──────────────────────────────────────────────────────────────

@Entity(
    tableName = "transactions",
    indices = [
        Index("transaction_no", unique = true),
        Index("account_id"),
        Index("member_id"),
        Index("transaction_type"),
        Index("created_at"),
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "transaction_no") val transactionNo: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "member_id") val memberId: String? = null,
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "transaction_type") val transactionType: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "balance_after_minor") val balanceAfterMinor: Long = 0,
    val currency: String = "KES",
    val description: String? = null,
    val reference: String? = null,
    val status: String = "completed",
    @ColumnInfo(name = "processed_by") val processedBy: String? = null,
    @ColumnInfo(name = "reversal_of") val reversalOf: String? = null,
    @ColumnInfo(name = "value_date") val valueDate: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "pending",
)

// ── Loans ─────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "loans",
    indices = [Index("loan_no", unique = true), Index("member_id"), Index("status")]
)
data class LoanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "loan_no") val loanNo: String,
    @ColumnInfo(name = "member_id") val memberId: String,
    @ColumnInfo(name = "account_id") val accountId: String? = null,
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "product_id") val productId: String? = null,
    @ColumnInfo(name = "principal_minor") val principalMinor: Long,
    @ColumnInfo(name = "outstanding_minor") val outstandingMinor: Long,
    @ColumnInfo(name = "interest_rate") val interestRate: Double,
    @ColumnInfo(name = "term_months") val termMonths: Int,
    val status: String = "pending",
    val purpose: String? = null,
    @ColumnInfo(name = "disbursement_date") val disbursementDate: String? = null,
    @ColumnInfo(name = "maturity_date") val maturityDate: String? = null,
    @ColumnInfo(name = "next_payment_date") val nextPaymentDate: String? = null,
    @ColumnInfo(name = "next_payment_minor") val nextPaymentMinor: Long = 0,
    @ColumnInfo(name = "approved_by") val approvedBy: String? = null,
    @ColumnInfo(name = "approved_at") val approvedAt: String? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "pending",
)

// ── Notifications ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "notifications",
    indices = [Index("user_id"), Index("member_id"), Index("is_read")]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "member_id") val memberId: String? = null,
    val title: String,
    val message: String,
    @ColumnInfo(name = "notification_type") val notificationType: String = "info",
    @ColumnInfo(name = "is_read") val isRead: Int = 0,
    val priority: Int = 5,
    @ColumnInfo(name = "action_url") val actionUrl: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "read_at") val readAt: String? = null,
)

// ── Audit Log ─────────────────────────────────────────────────────────────────

@Entity(tableName = "audit_log", indices = [Index("user_id"), Index("timestamp")])
data class AuditLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    val action: String,
    @ColumnInfo(name = "entity_type") val entityType: String? = null,
    @ColumnInfo(name = "entity_id") val entityId: String? = null,
    val operation: String? = null,
    @ColumnInfo(name = "old_values") val oldValues: String? = null,
    @ColumnInfo(name = "new_values") val newValues: String? = null,
    @ColumnInfo(name = "ip_address") val ipAddress: String? = null,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    val hash: String? = null,
    @ColumnInfo(name = "blockchain_index") val blockchainIndex: Int? = null,
    val timestamp: String,
)
