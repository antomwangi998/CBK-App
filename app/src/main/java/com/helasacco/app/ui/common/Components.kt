package com.helasacco.app.ui.common

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helasacco.app.domain.model.KycStatus
import com.helasacco.app.domain.model.LoanStatus
import com.helasacco.app.domain.model.TransactionType
import com.helasacco.app.ui.theme.HelaColors
import java.text.NumberFormat
import java.util.Locale

// ── Currency formatting ───────────────────────────────────────────────────────

private val kesFormat = NumberFormat.getCurrencyInstance(Locale("sw", "KE")).apply {
    currency = java.util.Currency.getInstance("KES")
    maximumFractionDigits = 2
}

fun Double.toKes(): String = kesFormat.format(this)
fun Long.minorToKes(): String = (this / 100.0).toKes()

// ── KES amount display ────────────────────────────────────────────────────────

@Composable
fun AmountText(
    amountMinor: Long,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showSign: Boolean = false,
) {
    val sign = if (showSign && amountMinor > 0) "+" else ""
    Text(
        text = "$sign${amountMinor.minorToKes()}",
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
    )
}

// ── Member avatar ─────────────────────────────────────────────────────────────

@Composable
fun MemberAvatar(
    initials: String,
    size: Dp = 40.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.take(2),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.35f).sp,
        )
    }
}

// ── Status chips ──────────────────────────────────────────────────────────────

@Composable
fun KycStatusChip(status: KycStatus) {
    val (label, containerColor, contentColor) = when (status) {
        KycStatus.APPROVED -> Triple("Verified", HelaColors.Success.copy(alpha = 0.15f), HelaColors.Success)
        KycStatus.PENDING -> Triple("Pending", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        KycStatus.UNDER_REVIEW -> Triple("In Review", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        KycStatus.REJECTED -> Triple("Rejected", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        KycStatus.SUBMITTED -> Triple("Submitted", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        KycStatus.EXPIRED -> Triple("Expired", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
        ),
    )
}

@Composable
fun LoanStatusChip(status: LoanStatus) {
    val (label, containerColor, contentColor) = when (status) {
        LoanStatus.ACTIVE -> Triple("Active", HelaColors.Success.copy(alpha = 0.15f), HelaColors.Success)
        LoanStatus.PENDING -> Triple("Pending", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        LoanStatus.APPROVED -> Triple("Approved", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        LoanStatus.REJECTED -> Triple("Rejected", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        LoanStatus.CLOSED -> Triple("Closed", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        LoanStatus.DISBURSED -> Triple("Disbursed", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        LoanStatus.WRITTEN_OFF -> Triple("Written Off", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        else -> Triple(status.value.replace("_", " ").replaceFirstChar { it.uppercase() }, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
        ),
    )
}

// ── Transaction type icon ─────────────────────────────────────────────────────

@Composable
fun TransactionIcon(type: TransactionType, size: Dp = 40.dp) {
    val (icon, bg, tint) = when (type) {
        TransactionType.DEPOSIT -> Triple(Icons.Filled.ArrowDownward, HelaColors.Success.copy(alpha = 0.15f), HelaColors.Success)
        TransactionType.WITHDRAWAL -> Triple(Icons.Filled.ArrowUpward, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        TransactionType.TRANSFER -> Triple(Icons.Filled.SwapHoriz, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary)
        TransactionType.LOAN_DISBURSEMENT -> Triple(Icons.Filled.AccountBalance, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary)
        TransactionType.LOAN_REPAYMENT -> Triple(Icons.Filled.Payments, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        TransactionType.INTEREST -> Triple(Icons.Filled.TrendingUp, HelaColors.Success.copy(alpha = 0.15f), HelaColors.Success)
        TransactionType.FEE, TransactionType.CHARGE, TransactionType.PENALTY ->
            Triple(Icons.Filled.Remove, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        else -> Triple(Icons.Filled.CompareArrows, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.8f))
                Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = contentColor, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Loading / error states ────────────────────────────────────────────────────

@Composable
fun LoadingScreen(message: String = "Loading...") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Filled.Inbox, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

// ── Info row (label + value) ──────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

// ── Confirmation dialog ───────────────────────────────────────────────────────

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}
