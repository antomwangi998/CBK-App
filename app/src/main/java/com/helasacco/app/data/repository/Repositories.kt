package com.helasacco.app.data.repository

import com.helasacco.app.data.local.dao.*
import com.helasacco.app.data.local.entities.*
import com.helasacco.app.di.SessionData
import com.helasacco.app.di.SessionManager
import com.helasacco.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import javax.inject.Inject

// ── Auth Repository ───────────────────────────────────────────────────────────

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<SessionData>
    suspend fun logout()
    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit>
    suspend fun createUser(
        username: String, password: String, role: UserRole,
        fullName: String, branchId: String?,
    ): Result<User>
}

class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<SessionData> {
        val entity = userDao.getByUsername(username)
            ?: return Result.Error("Invalid username or password")

        if (entity.isLocked == 1) {
            val lockedUntil = entity.lockedUntil
            return Result.Error("Account locked${if (lockedUntil != null) " until $lockedUntil" else ""}")
        }

        if (entity.isActive == 0) {
            return Result.Error("Account is inactive. Contact your administrator.")
        }

        val passwordValid = verifyPassword(password, entity.passwordHash, entity.salt, entity.iterations)
        if (!passwordValid) {
            userDao.incrementFailedAttempts(username)
            if (entity.failedAttempts >= 4) { // 5th attempt triggers lock
                val lockUntil = now().plusMinutes(30).format(dtf)
                userDao.lockAccount(username, lockUntil)
            }
            return Result.Error("Invalid username or password")
        }

        val token = UUID.randomUUID().toString()
        val expires = now().plusHours(8).format(dtf)
        userDao.recordLogin(entity.id, now().format(dtf))
        userDao.updateSession(entity.id, token, expires)

        val session = SessionData(
            userId = entity.id,
            username = entity.username,
            fullName = entity.fullName ?: entity.username,
            role = UserRole.from(entity.role),
            branchId = entity.branchId,
            memberId = entity.memberId,
            sessionToken = token,
        )
        sessionManager.saveSession(session)
        return Result.Success(session)
    }

    override suspend fun logout() {
        val session = sessionManager.session
        sessionManager.clearSession()
    }

    override suspend fun changePassword(
        userId: String, oldPassword: String, newPassword: String,
    ): Result<Unit> {
        val entity = userDao.getById(userId) ?: return Result.Error("User not found")
        if (!verifyPassword(oldPassword, entity.passwordHash, entity.salt, entity.iterations)) {
            return Result.Error("Current password is incorrect")
        }
        val (hash, salt) = hashPassword(newPassword)
        userDao.update(entity.copy(passwordHash = hash, salt = salt, updatedAt = now().format(dtf)))
        return Result.Success(Unit)
    }

    override suspend fun createUser(
        username: String, password: String, role: UserRole,
        fullName: String, branchId: String?,
    ): Result<User> {
        if (userDao.getByUsername(username) != null) {
            return Result.Error("Username already exists")
        }
        val (hash, salt) = hashPassword(password)
        val entity = UserEntity(
            id = UUID.randomUUID().toString(),
            username = username,
            passwordHash = hash,
            salt = salt,
            role = role.value,
            fullName = fullName,
            branchId = branchId,
            createdAt = now().format(dtf),
        )
        userDao.insert(entity)
        return Result.Success(entity.toDomain())
    }

    // ── Crypto helpers (mirrors Python PBKDF2 logic) ──────────────────────────

    private fun hashPassword(password: String, iterations: Int = 600_000): Pair<String, String> {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hash = pbkdf2(password, salt, iterations)
        val hashB64 = Base64.getEncoder().encodeToString(hash)
        return hashB64 to saltB64
    }

    private fun verifyPassword(password: String, hashB64: String, saltB64: String, iterations: Int): Boolean {
        return try {
            val salt = Base64.getDecoder().decode(saltB64)
            val expected = Base64.getDecoder().decode(hashB64)
            val actual = pbkdf2(password, salt, iterations)
            MessageDigest.isEqual(actual, expected)
        } catch (e: Exception) { false }
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(), salt, iterations, 256
        )
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun now() = LocalDateTime.now()
    private val dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME
}

// ── Member Repository ─────────────────────────────────────────────────────────

interface MemberRepository {
    fun getAllActive(): Flow<List<Member>>
    fun getByBranch(branchId: String): Flow<List<Member>>
    fun getActiveCount(): Flow<Int>
    fun getPendingKycCount(): Flow<Int>
    suspend fun getById(id: String): Member?
    suspend fun getByMemberNo(memberNo: String): Member?
    suspend fun search(query: String): List<Member>
    suspend fun save(member: Member, createdBy: String? = null): Result<Member>
    suspend fun updateKycStatus(id: String, status: KycStatus): Result<Unit>
}

class MemberRepositoryImpl @Inject constructor(
    private val memberDao: MemberDao,
) : MemberRepository {
    override fun getAllActive() = memberDao.getAllActive().map { it.map { e -> e.toDomain() } }
    override fun getByBranch(branchId: String) = memberDao.getByBranch(branchId).map { it.map { e -> e.toDomain() } }
    override fun getActiveCount() = memberDao.getActiveCount()
    override fun getPendingKycCount() = memberDao.getPendingKycCount()
    override suspend fun getById(id: String) = memberDao.getById(id)?.toDomain()
    override suspend fun getByMemberNo(no: String) = memberDao.getByMemberNo(no)?.toDomain()
    override suspend fun search(query: String) = memberDao.search(query).map { it.toDomain() }

    override suspend fun save(member: Member, createdBy: String?): Result<Member> {
        return try {
            memberDao.insert(member.toEntity(createdBy = createdBy))
            Result.Success(member)
        } catch (e: Exception) {
            Result.Error("Failed to save member: ${e.message}", e)
        }
    }

    override suspend fun updateKycStatus(id: String, status: KycStatus): Result<Unit> {
        return try {
            memberDao.updateKycStatus(id, status.value, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update KYC: ${e.message}", e)
        }
    }
}

// ── Account Repository ────────────────────────────────────────────────────────

interface AccountRepository {
    fun getByMember(memberId: String): Flow<List<Account>>
    fun getTotalDeposits(): Flow<Long?>
    fun getActiveCount(): Flow<Int>
    suspend fun getById(id: String): Account?
    suspend fun save(account: AccountEntity): Result<Unit>
    suspend fun updateBalance(id: String, balance: Long, available: Long): Result<Unit>
}

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
) : AccountRepository {
    override fun getByMember(memberId: String) = accountDao.getByMember(memberId).map { it.map { e -> e.toDomain() } }
    override fun getTotalDeposits() = accountDao.getTotalDeposits()
    override fun getActiveCount() = accountDao.getActiveCount()
    override suspend fun getById(id: String) = accountDao.getById(id)?.toDomain()

    override suspend fun save(account: AccountEntity): Result<Unit> = try {
        accountDao.insert(account)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to save account", e)
    }

    override suspend fun updateBalance(id: String, balance: Long, available: Long): Result<Unit> = try {
        accountDao.updateBalance(id, balance, available, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update balance", e)
    }
}

// ── Transaction Repository ────────────────────────────────────────────────────

interface TransactionRepository {
    fun getRecentByAccount(accountId: String): Flow<List<Transaction>>
    suspend fun getByAccount(accountId: String, limit: Int, offset: Int): List<Transaction>
    suspend fun getByMember(memberId: String, limit: Int, offset: Int): List<Transaction>
    suspend fun save(transaction: TransactionEntity): Result<Unit>
    suspend fun getTotalDepositsFrom(fromDate: String): Long
    suspend fun getTotalWithdrawalsFrom(fromDate: String): Long
}

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {
    override fun getRecentByAccount(accountId: String) =
        transactionDao.getRecentByAccount(accountId).map { it.map { e -> e.toDomain() } }

    override suspend fun getByAccount(accountId: String, limit: Int, offset: Int) =
        transactionDao.getByAccount(accountId, limit, offset).map { it.toDomain() }

    override suspend fun getByMember(memberId: String, limit: Int, offset: Int) =
        transactionDao.getByMember(memberId, limit, offset).map { it.toDomain() }

    override suspend fun save(transaction: TransactionEntity): Result<Unit> = try {
        transactionDao.insert(transaction)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to save transaction", e)
    }

    override suspend fun getTotalDepositsFrom(fromDate: String) =
        transactionDao.getTotalDepositsFrom(fromDate) ?: 0L

    override suspend fun getTotalWithdrawalsFrom(fromDate: String) =
        transactionDao.getTotalWithdrawalsFrom(fromDate) ?: 0L
}

// ── Loan Repository ───────────────────────────────────────────────────────────

interface LoanRepository {
    fun getByMember(memberId: String): Flow<List<Loan>>
    fun getActiveLoans(): Flow<List<Loan>>
    fun getPendingLoans(): Flow<List<Loan>>
    fun getTotalOutstanding(): Flow<Long?>
    fun getPendingCount(): Flow<Int>
    suspend fun getById(id: String): Loan?
    suspend fun save(loan: LoanEntity): Result<Unit>
    suspend fun updateStatus(id: String, status: LoanStatus): Result<Unit>
}

class LoanRepositoryImpl @Inject constructor(
    private val loanDao: LoanDao,
) : LoanRepository {
    override fun getByMember(memberId: String) = loanDao.getByMember(memberId).map { it.map { e -> e.toDomain() } }
    override fun getActiveLoans() = loanDao.getActiveLoans().map { it.map { e -> e.toDomain() } }
    override fun getPendingLoans() = loanDao.getPendingLoans().map { it.map { e -> e.toDomain() } }
    override fun getTotalOutstanding() = loanDao.getTotalOutstanding()
    override fun getPendingCount() = loanDao.getPendingCount()
    override suspend fun getById(id: String) = loanDao.getById(id)?.toDomain()

    override suspend fun save(loan: LoanEntity): Result<Unit> = try {
        loanDao.insert(loan)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to save loan", e)
    }

    override suspend fun updateStatus(id: String, status: LoanStatus): Result<Unit> = try {
        loanDao.updateStatus(id, status.value, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update loan status", e)
    }
}
