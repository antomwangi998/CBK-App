package com.helasacco.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helasacco.app.domain.model.Account
import com.helasacco.app.domain.model.Member
import com.helasacco.app.ui.common.*
import com.helasacco.app.ui.theme.HelaColors

private val QUICK_AMOUNTS = listOf(500, 1000, 2000, 5000, 10000)
private val CHANNELS = listOf("branch", "mobile", "agent", "online")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(onBack: () -> Unit, viewModel: TransactionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionScaffold(
        title = "Deposit",
        accentColor = HelaColors.Success,
        onBack = { viewModel.reset(); onBack() },
        state = state,
        onProcess = viewModel::processDeposit,
        onDismissReceipt = { viewModel.dismissReceipt(); viewModel.reset(); onBack() },
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(onBack: () -> Unit, viewModel: TransactionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionScaffold(
        title = "Withdrawal",
        accentColor = MaterialTheme.colorScheme.error,
        onBack = { viewModel.reset(); onBack() },
        state = state,
        onProcess = viewModel::processWithdrawal,
        onDismissReceipt = { viewModel.dismissReceipt(); viewModel.reset(); onBack() },
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(onBack: () -> Unit, viewModel: TransactionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionScaffold(
        title = "Transfer",
        accentColor = MaterialTheme.colorScheme.secondary,
        onBack = { viewModel.reset(); onBack() },
        state = state,
        onProcess = viewModel::processTransfer,
        onDismissReceipt = { viewModel.dismissReceipt(); viewModel.reset(); onBack() },
        viewModel = viewModel,
    )
}

// ── Shared scaffold ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionScaffold(
    title: String,
    accentColor: Color,
    onBack: () -> Unit,
    state: TransactionFormState,
    onProcess: () -> Unit,
    onDismissReceipt: () -> Unit,
    viewModel: TransactionViewModel,
) {
    // Receipt dialog
    state.receiptData?.let { receipt ->
        ReceiptDialog(receipt = receipt, onDone = onDismissReceipt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = accentColor, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = onProcess,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    enabled = !state.isLoading && state.selectedMember != null && state.selectedAccountId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("CONFIRM $title", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Member search
            MemberSearchSection(state = state, accentColor = accentColor, viewModel = viewModel)

            // Account picker
            if (state.accounts.isNotEmpty()) {
                AccountPickerSection(accounts = state.accounts, selectedId = state.selectedAccountId, onSelect = viewModel::selectAccount)
            }

            // Amount
            AmountSection(state = state, accentColor = accentColor, viewModel = viewModel)

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Reference / Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )

            // Channel
            ChannelSelector(selected = state.channel, onSelect = viewModel::onChannelChange, accentColor = accentColor)

            // Error
            state.error?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun MemberSearchSection(state: TransactionFormState, accentColor: Color, viewModel: TransactionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Member", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Name, phone or ID number") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )
            Button(onClick = viewModel::searchMember, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                if (state.isMemberLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Find")
            }
        }
        state.selectedMember?.let { member ->
            Card(colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)), shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MemberAvatar(initials = member.initials, backgroundColor = accentColor.copy(alpha = 0.2f), textColor = accentColor)
                    Column {
                        Text(member.fullName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("${member.memberNo} • ${member.phone ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountPickerSection(accounts: List<Account>, selectedId: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Account", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        accounts.forEach { account ->
            val selected = account.id == selectedId
            Card(
                onClick = { onSelect(account.id) },
                colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(account.accountNo, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        Text(account.accountType.value.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(account.balanceMinor.minorToKes(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Available: ${account.availableBalanceMinor.minorToKes()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountSection(state: TransactionFormState, accentColor: Color, viewModel: TransactionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Amount (KES)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = state.amountText,
            onValueChange = viewModel::onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Text("KES", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp)) },
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_AMOUNTS.forEach { amt ->
                SuggestionChip(
                    onClick = { viewModel.setQuickAmount(amt) },
                    label = { Text("%,d".format(amt)) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = accentColor.copy(alpha = 0.1f), labelColor = accentColor),
                )
            }
        }
    }
}

@Composable
private fun ChannelSelector(selected: String, onSelect: (String) -> Unit, accentColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Channel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CHANNELS.forEach { ch ->
                FilterChip(
                    selected = selected == ch,
                    onClick = { onSelect(ch) },
                    label = { Text(ch.replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(alpha = 0.15f), selectedLabelColor = accentColor),
                )
            }
        }
    }
}

// ── Receipt dialog ────────────────────────────────────────────────────────────

@Composable
private fun ReceiptDialog(receipt: ReceiptData, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Filled.CheckCircle, null, tint = HelaColors.Success, modifier = Modifier.size(40.dp)) },
        title = { Text("${receipt.type} Successful", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("Member", receipt.memberName)
                InfoRow("Account", receipt.accountNo)
                InfoRow("Amount", receipt.amount.minorToKes())
                InfoRow("Balance After", receipt.balanceAfter.minorToKes())
                InfoRow("Reference", receipt.reference)
                InfoRow("Time", receipt.timestamp)
            }
        },
        confirmButton = { Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = HelaColors.Success)) { Text("DONE") } },
        shape = RoundedCornerShape(16.dp),
    )
}

// Extension for StateFlow access in screens
val TransactionViewModel.uiState get() = state
