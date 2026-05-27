package com.example.finfurcate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatsScreen(isDarkMode: Boolean, onBack: () -> Unit) {
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F4F0)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1E1E1E)
    val subTextColor = if (isDarkMode) Color.LightGray else Color(0xFF757575)

    var selectedTab by remember { mutableStateOf("Month") }
    val tabs = listOf("Week", "Month", "3 Months", "Year")

    // DYNAMIC MATH LOGIC
    val totalSpent = MoneyData.totalSpent
    val currentSaved = MoneyData.currentBalance
    val allTransactions = TransactionRepo.transactions
    val expenses = allTransactions.filter { !it.isCredit }

    // Sort transactions by highest amount for the Top Spending list
    val topSpends = expenses.sortedByDescending { it.amount }.take(4)

    // Calculate Donut Chart percentages based on Envelope balances
    val envelopes = EnvelopeRepo.envelopes
    val totalInEnvelopes = envelopes.sumOf { it.currentAmount }
    val topEnvelopes = envelopes.sortedByDescending { it.currentAmount }.take(3)
    val othersAmount = envelopes.sortedByDescending { it.currentAmount }.drop(3).sumOf { it.currentAmount }

    Column(
        modifier = Modifier.fillMaxSize().background(bgColor).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- HEADER ---
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("STATS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = subTextColor, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-10).dp))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.background(cardBg, CircleShape)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor) }
                Text("Analytics", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                IconButton(onClick = { /* Export */ }, modifier = Modifier.background(cardBg, CircleShape)) { Icon(Icons.Default.Download, contentDescription = "Export", tint = textColor) }
            }
        }

        // --- TIME SELECTOR ---
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent, RoundedCornerShape(20.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tab, color = if (isSelected) Color.White else subTextColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                    }
                }
            }
        }

        // --- SUMMARY CARDS (DYNAMIC) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)), modifier = Modifier.weight(1f).height(120.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL SPENT", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("₹${totalSpent.toInt()}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)), modifier = Modifier.weight(1f).height(120.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("AVAILABLE", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("₹${currentSaved.toInt()}", color = Color(0xFF059669), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // --- BY CATEGORY DONUT CHART (DYNAMIC) ---
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Envelope Distribution", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 24.dp.toPx()
                            if (totalInEnvelopes <= 0) {
                                drawArc(Color.LightGray, 0f, 360f, false, style = Stroke(strokeWidth))
                            } else {
                                var currentStartAngle = -90f
                                topEnvelopes.forEach { env ->
                                    val sweepAngle = ((env.currentAmount / totalInEnvelopes) * 360).toFloat()
                                    drawArc(Color(env.colorHex), currentStartAngle, sweepAngle, false, style = Stroke(strokeWidth))
                                    currentStartAngle += sweepAngle
                                }
                                if (othersAmount > 0) {
                                    val sweepAngle = ((othersAmount / totalInEnvelopes) * 360).toFloat()
                                    drawArc(Color.Gray, currentStartAngle, sweepAngle, false, style = Stroke(strokeWidth))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        if (totalInEnvelopes <= 0) {
                            Text("No active envelopes", color = subTextColor, fontSize = 14.sp)
                        } else {
                            topEnvelopes.forEach { env ->
                                val pct = ((env.currentAmount / totalInEnvelopes) * 100).toInt()
                                DonutLegendItem(Color(env.colorHex), env.name, "$pct%", textColor, subTextColor)
                            }
                            if (othersAmount > 0) {
                                val pct = ((othersAmount / totalInEnvelopes) * 100).toInt()
                                DonutLegendItem(Color.Gray, "Others", "$pct%", textColor, subTextColor)
                            }
                        }
                    }
                }
            }
        }

        // --- LOGGING STREAK ---
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Logging streak", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Keep it up — you're on a roll!", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
                Text("14d", color = Color(0xFFFBBF24), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // --- TOP SPENDING LIST (DYNAMIC WITH SMART COLORS) ---
        Text("TOP SPENDING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = subTextColor, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 8.dp))

        if (topSpends.isEmpty()) {
            Text("No expenses recorded yet.", color = subTextColor, modifier = Modifier.padding(bottom = 32.dp))
        } else {
            topSpends.forEachIndexed { index, txn ->
                // 👇 SMART CATEGORIZER: Assigns colors based on the transaction title!
                val lowerTitle = txn.title.lowercase()
                val iconData = when {
                    lowerTitle.contains("rent") || lowerTitle.contains("home") ->
                        Triple(Icons.Default.Home, Color(0xFFEDE9FE), Color(0xFF7C3AED)) // Purple House
                    lowerTitle.contains("recharge") || lowerTitle.contains("bill") ->
                        Triple(Icons.Default.Bolt, Color(0xFFD1FAE5), Color(0xFF059669)) // Green Bolt
                    lowerTitle.contains("veg") || lowerTitle.contains("food") || lowerTitle.contains("grocer") ->
                        Triple(Icons.Default.Eco, Color(0xFFFEF3C7), Color(0xFFD97706)) // Yellow Leaf
                    lowerTitle.contains("celebration") || lowerTitle.contains("party") || lowerTitle.contains("birthday") ->
                        Triple(Icons.Default.Cake, Color(0xFFFCE7F3), Color(0xFFDB2777)) // Pink Cake
                    else ->
                        Triple(Icons.Default.ReceiptLong, Color(0xFFF3F4F6), Color.DarkGray) // Default Gray Receipt
                }

                TopSpendCard(
                    rank = (index + 1).toString(),
                    title = txn.title,
                    amount = "₹${txn.amount.toInt()}",
                    icon = iconData.first,
                    iconBg = iconData.second,
                    iconTint = iconData.third,
                    cardBg = cardBg,
                    textColor = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom nav
    }
}

// --- SUB-COMPONENTS ---
@Composable
fun DonutLegendItem(color: Color, label: String, percentage: String, textColor: Color, subTextColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, color = subTextColor, maxLines = 1)
        }
        Text(percentage, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
fun TopSpendCard(rank: String, title: String, amount: String, icon: ImageVector, iconBg: Color, iconTint: Color, cardBg: Color, textColor: Color) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(rank, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp))
            Box(modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 16.sp, color = textColor, modifier = Modifier.weight(1f), maxLines = 1)
            Text(amount, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
        }
    }
}