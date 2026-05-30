package com.helasacco.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helasacco.app.data.local.entities.TransactionEntity
import com.helasacco.app.data.repository.*
import com.helasacco.app.di.SessionManager
import com.helasacco.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class TransactionFormState(
    val searchQuery: String = "",
    val selectedMember: Member? = null,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String? = null,
    val amountText: String = "",
    val description: String = "",
    val channel: String = "branch",
    val isLoading: Boolean = false,
    val isMemberLoading: Boolean = false,
    val error: String? = null,
    val receiptData: ReceiptData? = null,
)

data class ReceiptData(
    val type: String,
    val memberName: String,
    val accountNo: String,
    val amount: Long,
    val balanceAfter: Long,
    val reference: String,
    val timestamp: String,
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionFormState())
    val state = _state.asStateFlow()

    fun onSearchChange(q: String) { _state.update { it.copy(searchQuery = q, error = null) } }

    fun searchMember() {
        val q = _state.value.searchQuery.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isMemberLoading = true, selectedMember = null, accounts = emptyList(), selectedAccountId = null) }
            val results = memberRepository.search(q)
            val member = results.firstOrNull()
            if (member != null) {
                val accounts = accountRepository.getByMember(member.id).first()
                    .filter { it.status == "active" }
                _state.update { it.copy(isMemberLoading = false, selectedMember = member, accounts = accounts, selectedAccountId = accounts.firstOrNull()?.id) }
            } else {
                _state.update { it.copy(isMemberLoading = false, error = "Member not found") }
            }
        }
    }

    fun selectAccount(id: String) { _state.update { it.copy(selectedAccountId = id) } }
    fun onAmountChange(v: String) { _state.update { it.copy(amountText = v, error = null) } }
    fun onDescriptionChange(v: String) { _state.update { it.copy(description = v) } }
    fun onChannelChange(v: String) { _state.update { it.copy(channel = v) } }
    fun setQuickAmount(amount: Int) { _state.update { it.copy(amountText = amount.toString()) } }
    fun dismissReceipt() { _state.update { it.copy(receiptData = null) } }
    fun reset() { _state.value = TransactionFormState() }

    fun processDeposit() = processTransaction(TransactionType.DEPOSIT)
    fun processWithdrawal() = processTransaction(TransactionType.WITHDRAWAL)

    private fun processTransaction(type: TransactionType) {
        val s = _state.value
        val amountMinor = s.amountText.replace(",", "").toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        val accountId = s.selectedAccountId

        if (s.selectedMember == null) { _state.update { it.copy(error = "Select a member first") }; return }
        if (accountId == null) { _state.update { it.copy(error = "Select an account") }; return }
        if (amountMinor <= 0) { _state.update { it.copy(error = "Enter a valid amount") }; return }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val session = sessionManager.session.first()
                val account = accountRepository.getById(accountId)!!
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                if (type == TransactionType.WITHDRAWAL && amountMinor > account.availableBalanceMinor) {
                    _state.update { it.copy(isLoading = false, error = "Insufficient funds. Available: ${account.availableBalance.toKesString()}") }
                    return@launch
                }

                val newBalance = if (type == TransactionType.DEPOSIT)
                    account.balanceMinor + amountMinor
                else
                    account.balanceMinor - amountMinor

                val reference = "TXN${System.currentTimeMillis()}"
                val entity = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    transactionNo = reference,
                    accountId = accountId,
                    memberId = s.selectedMember.id,
                    branchId = session?.branchId,
                    transactionType = type.value,
                    amountMinor = amountMinor,
                    balanceAfterMinor = newBalance,
                    description = s.description.ifBlank { "${type.value.replace("_", " ").replaceFirstChar { it.uppercase() }} - ${s.channel}" },
                    reference = reference,
                    status = "completed",
                    processedBy = session?.userId,
                    createdAt = now,
                )
                transactionRepository.save(entity)
                accountRepository.updateBalance(accountId, newBalance, newBalance)

                _state.update {
                    it.copy(
                        isLoading = false,
                        receiptData = ReceiptData(
                            type = type.value.replace("_", " ").replaceFirstChar { it.uppercase() },
                            memberName = s.selectedMember.fullName,
                            accountNo = account.accountNo,
                            amount = amountMinor,
                            balanceAfter = newBalance,
                            reference = reference,
                            timestamp = now.take(19).replace("T", " "),
                        ),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Transaction failed") }
            }
        }
    }

    fun processTransfer() {
        val s = _state.value
        val amountMinor = s.amountText.replace(",", "").toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        if (s.selectedMember == null || s.selectedAccountId == null || amountMinor <= 0) {
            _state.update { it.copy(error = "Fill all fields correctly") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val session = sessionManager.session.first()
                val fromAccount = accountRepository.getById(s.selectedAccountId)!!
                if (amountMinor > fromAccount.availableBalanceMinor) {
                    _state.update { it.copy(isLoading = false, error = "Insufficient funds") }
                    return@launch
                }
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val ref = "TRF${System.currentTimeMillis()}"
                val newBalance = fromAccount.balanceMinor - amountMinor
                val entity = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    transactionNo = ref,
                    accountId = s.selectedAccountId,
                    memberId = s.selectedMember.id,
                    branchId = session?.branchId,
                    transactionType = TransactionType.TRANSFER.value,
                    amountMinor = amountMinor,
                    balanceAfterMinor = newBalance,
                    description = s.description.ifBlank { "Transfer" },
                    reference = ref,
                    status = "completed",
                    processedBy = session?.userId,
                    createdAt = now,
                )
                transactionRepository.save(entity)
                accountRepository.updateBalance(s.selectedAccountId, newBalance, newBalance)
                _state.update {
                    it.copy(
                        isLoading = false,
                        receiptData = ReceiptData(
                            type = "Transfer",
                            memberName = s.selectedMember.fullName,
                            accountNo = fromAccount.accountNo,
                            amount = amountMinor,
                            balanceAfter = newBalance,
                            reference = ref,
                            timestamp = now.take(19).replace("T", " "),
                        ),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Transfer failed") }
            }
        }
    }
}

private fun Double.toKesString(): String = "KES %,.2f".format(this)
