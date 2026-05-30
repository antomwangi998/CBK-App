package com.helasacco.app.data.local.dao

import androidx.room.*
import com.helasacco.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// ── User DAO ──────────────────────────────────────────────────────────────────

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND deleted_at IS NULL")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE deleted_at IS NULL ORDER BY full_name ASC")
    fun getAllActive(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE branch_id = :branchId AND deleted_at IS NULL")
    fun getByBranch(branchId: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET last_login = :timestamp, failed_attempts = 0 WHERE id = :id")
    suspend fun recordLogin(id: String, timestamp: String)

    @Query("UPDATE users SET failed_attempts = failed_attempts + 1 WHERE username = :username")
    suspend fun incrementFailedAttempts(username: String)

    @Query("UPDATE users SET is_locked = 1, locked_until = :until WHERE username = :username")
    suspend fun lockAccount(username: String, until: String)

    @Query("UPDATE users SET session_token = :token, session_expires = :expires WHERE id = :id")
    suspend fun updateSession(id: String, token: String, expires: String)

    @Query("UPDATE users SET session_token = NULL, session_expires = NULL WHERE id = :id")
    suspend fun clearSession(id: String)
}

// ── Branch DAO ────────────────────────────────────────────────────────────────

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE id = :id")
    suspend fun getById(id: String): BranchEntity?

    @Query("SELECT * FROM branches WHERE is_active = 1 ORDER BY name ASC")
    fun getAllActive(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE is_head_office = 1 LIMIT 1")
    suspend fun getHeadOffice(): BranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(branch: BranchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(branches: List<BranchEntity>)

    @Update
    suspend fun update(branch: BranchEntity)
}

// ── Member DAO ────────────────────────────────────────────────────────────────

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): MemberEntity?

    @Query("SELECT * FROM members WHERE member_no = :memberNo AND deleted_at IS NULL")
    suspend fun getByMemberNo(memberNo: String): MemberEntity?

    @Query("""
        SELECT * FROM members 
        WHERE deleted_at IS NULL AND is_active = 1
        ORDER BY first_name ASC, last_name ASC
    """)
    fun getAllActive(): Flow<List<MemberEntity>>

    @Query("""
        SELECT * FROM members
        WHERE deleted_at IS NULL
        AND (
            full_name_search LIKE '%' || :query || '%'
            OR member_no LIKE '%' || :query || '%'
            OR phone LIKE '%' || :query || '%'
            OR id_number LIKE '%' || :query || '%'
        )
        ORDER BY first_name ASC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<MemberEntity>

    @Query("""
        SELECT * FROM members 
        WHERE branch_id = :branchId AND deleted_at IS NULL AND is_active = 1
        ORDER BY first_name ASC
    """)
    fun getByBranch(branchId: String): Flow<List<MemberEntity>>

    @Query("SELECT COUNT(*) FROM members WHERE deleted_at IS NULL AND is_active = 1")
    fun getActiveCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM members 
        WHERE deleted_at IS NULL AND kyc_status = 'pending'
    """)
    fun getPendingKycCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>)

    @Update
    suspend fun update(member: MemberEntity)

    @Query("UPDATE members SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: String)

    @Query("UPDATE members SET kyc_status = :status, updated_at = :timestamp WHERE id = :id")
    suspend fun updateKycStatus(id: String, status: String, timestamp: String)

    @Query("SELECT * FROM members WHERE sync_status = 'pending' LIMIT 100")
    suspend fun getPendingSync(): List<MemberEntity>
}

// ── Account DAO ───────────────────────────────────────────────────────────────

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE member_id = :memberId AND deleted_at IS NULL ORDER BY account_type ASC")
    fun getByMember(memberId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE member_id = :memberId AND account_type = :type AND deleted_at IS NULL LIMIT 1")
    suspend fun getByMemberAndType(memberId: String, type: String): AccountEntity?

    @Query("SELECT SUM(balance_minor) FROM accounts WHERE deleted_at IS NULL AND status = 'active'")
    fun getTotalDeposits(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM accounts WHERE deleted_at IS NULL AND status = 'active'")
    fun getActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET balance_minor = :balance, available_balance_minor = :available, last_transaction_date = :timestamp WHERE id = :id")
    suspend fun updateBalance(id: String, balance: Long, available: Long, timestamp: String)
}

// ── Transaction DAO ───────────────────────────────────────────────────────────

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE account_id = :accountId 
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByAccount(accountId: String, limit: Int = 20, offset: Int = 0): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE member_id = :memberId 
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByMember(memberId: String, limit: Int = 20, offset: Int = 0): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE account_id = :accountId
        ORDER BY created_at DESC
        LIMIT 10
    """)
    fun getRecentByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE branch_id = :branchId
        AND created_at >= :fromDate
        ORDER BY created_at DESC
    """)
    fun getByBranchAndDate(branchId: String, fromDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount_minor) FROM transactions WHERE transaction_type = 'deposit' AND created_at >= :fromDate")
    suspend fun getTotalDepositsFrom(fromDate: String): Long?

    @Query("SELECT SUM(amount_minor) FROM transactions WHERE transaction_type = 'withdrawal' AND created_at >= :fromDate")
    suspend fun getTotalWithdrawalsFrom(fromDate: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
}

// ── Loan DAO ──────────────────────────────────────────────────────────────────

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getById(id: String): LoanEntity?

    @Query("SELECT * FROM loans WHERE member_id = :memberId ORDER BY created_at DESC")
    fun getByMember(memberId: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status IN ('active', 'disbursed') ORDER BY created_at DESC")
    fun getActiveLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = 'pending' ORDER BY created_at ASC")
    fun getPendingLoans(): Flow<List<LoanEntity>>

    @Query("SELECT SUM(outstanding_minor) FROM loans WHERE status IN ('active', 'disbursed')")
    fun getTotalOutstanding(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM loans WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    @Query("""
        SELECT * FROM loans 
        WHERE next_payment_date <= :date AND status = 'active'
        ORDER BY next_payment_date ASC
    """)
    suspend fun getOverdueLoans(date: String): List<LoanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: LoanEntity)

    @Update
    suspend fun update(loan: LoanEntity)

    @Query("UPDATE loans SET status = :status, updated_at = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: String)
}

// ── Notification DAO ──────────────────────────────────────────────────────────

@Dao
interface NotificationDao {
    @Query("""
        SELECT * FROM notifications 
        WHERE (user_id = :userId OR member_id = :memberId)
        ORDER BY created_at DESC
        LIMIT 50
    """)
    fun getForUser(userId: String, memberId: String?): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    fun getUnreadCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET is_read = 1, read_at = :timestamp WHERE id = :id")
    suspend fun markRead(id: String, timestamp: String)

    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId")
    suspend fun markAllRead(userId: String)
}

// ── Audit DAO ─────────────────────────────────────────────────────────────────

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = 50, offset: Int = 0): List<AuditLogEntity>

    @Query("SELECT * FROM audit_log WHERE user_id = :userId ORDER BY timestamp DESC LIMIT 50")
    fun getByUser(userId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEntry(): AuditLogEntity?
}
