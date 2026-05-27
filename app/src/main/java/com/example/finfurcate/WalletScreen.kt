package com.example.finfurcate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    isDarkMode: Boolean,
    onEnvelopeClick: (BudgetEnvelope) -> Unit,
    onCreateClick: () -> Unit
) {
    val context = LocalContext.current
    val upiId = UserPrefs.getUpi(context)

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F4F0)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1E1E1E)
    val subTextColor = if (isDarkMode) Color.LightGray else Color(0xFF757575)

    val envelopes = EnvelopeRepo.envelopes
    val totalInWallets = envelopes.sumOf { it.currentAmount }
    val overspentTotal = envelopes.filter { it.limit > 0 && it.currentAmount > it.limit }.sumOf { it.currentAmount - it.limit }
    val unallocated = maxOf(0.0, MoneyData.currentBalance - totalInWallets)

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Active", "Over budget", "Empty")

    // 👇 NEW: State for the Transfer Logic
    var showTransferDialog by remember { mutableStateOf(false) }

    if (showTransferDialog) {
        var fromEnv by remember { mutableStateOf<BudgetEnvelope?>(null) }
        var toEnv by remember { mutableStateOf<BudgetEnvelope?>(null) }
        var transferAmount by remember { mutableStateOf("") }
        var expandedFrom by remember { mutableStateOf(false) }
        var expandedTo by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transfer Funds", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Move money without affecting your Total Spent.", fontSize = 13.sp, color = Color.Gray)

                    // FROM Dropdown
                    ExposedDropdownMenuBox(expanded = expandedFrom, onExpandedChange = { expandedFrom = !expandedFrom }) {
                        OutlinedTextField(
                            value = fromEnv?.name ?: "Select 'From' Envelope", onValueChange = {}, readOnly = true,
                            label = { Text("From") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                            envelopes.filter { it.currentAmount > 0 }.forEach { env ->
                                DropdownMenuItem(text = { Text("${env.name} (₹${env.currentAmount})") }, onClick = { fromEnv = env; expandedFrom = false })
                            }
                        }
                    }

                    // TO Dropdown
                    ExposedDropdownMenuBox(expanded = expandedTo, onExpandedChange = { expandedTo = !expandedTo }) {
                        OutlinedTextField(
                            value = toEnv?.name ?: "Select 'To' Envelope", onValueChange = {}, readOnly = true,
                            label = { Text("To") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                            envelopes.filter { it != fromEnv }.forEach { env ->
                                DropdownMenuItem(text = { Text(env.name) }, onClick = { toEnv = env; expandedTo = false })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = transferAmount, onValueChange = { transferAmount = it }, label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = transferAmount.toDoubleOrNull() ?: 0.0
                    if (fromEnv != null && toEnv != null && amt > 0 && amt <= fromEnv!!.currentAmount) {
                        EnvelopeRepo.updateAmount(context, upiId, fromEnv!!, -amt)
                        EnvelopeRepo.updateAmount(context, upiId, toEnv!!, amt)
                        showTransferDialog = false
                    }
                }) { Text("Transfer") }
            },
            dismissButton = { TextButton(onClick = { showTransferDialog = false }) { Text("Cancel") } }
        )
    }

    val displayEnvelopes = when (selectedFilter) {
        "Over budget" -> envelopes.filter { it.limit > 0 && it.currentAmount > it.limit }
        "Empty" -> envelopes.filter { it.currentAmount <= 0 }
        "Active" -> envelopes.filter { it.currentAmount > 0 }
        else -> envelopes
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("My Wallets", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)))).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TOTAL ACROSS WALLETS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("₹${totalInWallets.toInt()}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Spread across ${envelopes.size} envelopes", color = Color.Gray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End) { Text("₹${unallocated.toInt()}", color = Color(0xFF34D399), fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("Unallocated", color = Color.Gray, fontSize = 11.sp) }
                    if (overspentTotal > 0) { Column(horizontalAlignment = Alignment.End) { Text("₹${overspentTotal.toInt()}", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("Overspent", color = Color.Gray, fontSize = 11.sp) } }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) Color(0xFF1E1E1E) else cardBg).clickable { selectedFilter = filter }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(text = filter, color = if (isSelected) Color.White else subTextColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
            items(displayEnvelopes) { envelope -> DetailedEnvelopeCard(envelope = envelope, cardBg = cardBg, textColor = textColor, onClick = { onEnvelopeClick(envelope) }) }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // 👇 UPDATED: Attached the transfer dialog to this button!
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)), modifier = Modifier.fillMaxWidth().clickable { showTransferDialog = true }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(Color.White, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color(0xFF4F46E5)) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) { Text("Transfer between envelopes", fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5)); Text("Move funds without a real transaction", fontSize = 12.sp, color = Color(0xFF6366F1)) }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4F46E5))
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth().clickable { onCreateClick() }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF6366F1)) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column { Text("Create new envelope", fontWeight = FontWeight.Bold, color = textColor); Text("Pick name, color, set budget limit", fontSize = 12.sp, color = subTextColor) }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedEnvelopeCard(envelope: BudgetEnvelope, cardBg: Color, textColor: Color, onClick: () -> Unit) {
    val isBudgetSet = envelope.limit > 0
    val progress = if (isBudgetSet) (envelope.currentAmount / envelope.limit).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = if (isBudgetSet) envelope.limit - envelope.currentAmount else 0.0
    val isOverBudget = isBudgetSet && envelope.currentAmount > envelope.limit
    val isNearLimit = isBudgetSet && progress >= 0.8f && !isOverBudget

    val statusColor = when { !isBudgetSet -> Color(0xFF9CA3AF); isOverBudget -> Color(0xFFEF4444); isNearLimit -> Color(0xFFF59E0B); else -> Color(0xFF10B981) }
    val envIconBg = Color(envelope.colorHex).copy(alpha = 0.2f)
    val envIconTint = Color(envelope.colorHex)
    val txnHistory = TransactionRepo.transactions.filter { it.title.contains(envelope.name, ignoreCase = true) }

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).background(envIconBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = envIconTint, modifier = Modifier.size(24.dp)) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column { Text(envelope.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor); Text(if (isBudgetSet) "Budget tracking active" else "No budget set", fontSize = 12.sp, color = Color.Gray) }
                }
                if (isBudgetSet) { when { isOverBudget -> BadgeChip(Icons.Default.Warning, "Over budget", Color(0xFFFEE2E2), Color(0xFFEF4444)); isNearLimit -> BadgeChip(Icons.Default.WarningAmber, "Near limit", Color(0xFFFEF3C7), Color(0xFFD97706)); else -> BadgeChip(Icons.Default.CheckCircle, "On track", Color(0xFFD1FAE5), Color(0xFF059669)) } }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("₹${envelope.currentAmount.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = if (isOverBudget) Color(0xFFEF4444) else textColor)
                Text(if (isBudgetSet) "of ₹${envelope.limit.toInt()}" else "No limit", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isBudgetSet) { LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = statusColor, trackColor = Color(0xFFF3F4F6)) } else { Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF3F4F6))) }
            Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f)); Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("₹${envelope.currentAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor); Text("Spent", fontSize = 12.sp, color = Color.Gray) }
                if (isBudgetSet) { Column { Text("₹${kotlin.math.abs(remaining.toInt())}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (remaining < 0) Color(0xFFEF4444) else Color(0xFF10B981)); Text(if (remaining < 0) "Over limit" else "Remaining", fontSize = 12.sp, color = Color.Gray) } }
                Column { Text("${txnHistory.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor); Text("Transactions", fontSize = 12.sp, color = Color.Gray) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onClick() }, modifier = Modifier.size(36.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}