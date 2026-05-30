package com.helasacco.app.ui.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.helasacco.app.ui.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Investment(
    val id: String,
    val name: String,
    val type: String,
    val principalMinor: Long,
    val currentValueMinor: Long,
    val interestRate: Double,
    val startDate: String,
    val maturityDate: String,
    val status: String,
) {
    val returnMinor get() = currentValueMinor - principalMinor
    val returnPct get() = if (principalMinor > 0) returnMinor * 100.0 / principalMinor else 0.0
}

data class InvestmentsUiState(
    val investments: List<Investment> = emptyList(),
    val totalPrincipalMinor: Long = 0,
    val totalCurrentValueMinor: Long = 0,
    val isLoading: Boolean = false,
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState = _uiState.asStateFlow()

    init { loadSampleData() }

    private fun loadSampleData() {
        // Real implementation reads from Room investments table
        val sample = listOf(
            Investment("1", "Fixed Deposit - KCB", "fixed_deposit", 50000_00, 53500_00, 7.0, "2024-01-01", "2025-01-01", "active"),
            Investment("2", "Treasury Bills 91-day", "treasury_bill", 100000_00, 101800_00, 7.2, "2025-01-15", "2025-04-16", "active"),
            Investment("3", "Member Shares", "shares", 25000_00, 28750_00, 15.0, "2023-06-01", "2026-06-01", "active"),
        )
        _uiState.update {
            it.copy(
                investments = sample,
                totalPrincipalMinor = sample.sumOf { i -> i.principalMinor },
                totalCurrentValueMinor = sample.sumOf { i -> i.currentValueMinor },
                isLoading = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onBack: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalReturn = uiState.totalCurrentValueMinor - uiState.totalPrincipalMinor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investments") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Portfolio summary
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Portfolio Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Invested", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                AmountText(uiState.totalPrincipalMinor, style = MaterialTheme.typography.titleLarge)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Current Value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                AmountText(uiState.totalCurrentValueMinor, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Total Return", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(if (totalReturn >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown, null, tint = if (totalReturn >= 0) com.helasacco.app.ui.theme.HelaColors.Success else MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Text(totalReturn.minorToKes(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (totalReturn >= 0) com.helasacco.app.ui.theme.HelaColors.Success else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            // Individual investments
            item { SectionHeader("Holdings") }
            uiState.investments.forEach { inv ->
                item(key = inv.id) {
                    InvestmentCard(investment = inv)
                }
            }
        }
    }
}

@Composable
private fun InvestmentCard(investment: Investment) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(investment.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(investment.type.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(investment.status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = com.helasacco.app.ui.theme.HelaColors.Success.copy(alpha = 0.12f), labelColor = com.helasacco.app.ui.theme.HelaColors.Success),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(investment.principalMinor.minorToKes(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${investment.interestRate}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Return", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+${investment.returnMinor.minorToKes()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = com.helasacco.app.ui.theme.HelaColors.Success)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Matures: ${investment.maturityDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%.1f%% return".format(investment.returnPct), style = MaterialTheme.typography.labelSmall, color = com.helasacco.app.ui.theme.HelaColors.Success)
            }
        }
    }
}
