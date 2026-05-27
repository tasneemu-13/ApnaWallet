package com.example.finfurcate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    currentName: String,
    currentUpi: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F4F0)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1E1E1E)
    val subTextColor = if (isDarkMode) Color.LightGray else Color(0xFF757575)

    var showEditDialog by remember { mutableStateOf(false) }
    // 👇 NEW: State for the 3-dots dropdown menu
    var showMenu by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var appLockEnabled by remember { mutableStateOf(true) }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(currentName) }
        var editUpi by remember { mutableStateOf(currentUpi) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Update Information", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editUpi, onValueChange = { editUpi = it }, label = { Text("UPI ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveProfile(editName, editUpi)
                    showEditDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(bgColor).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- HEADER ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.background(cardBg, CircleShape)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)

                // 👇 UPDATED: The dynamic 3-dots menu
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.background(cardBg, CircleShape)) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = textColor)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit Profile") },
                            onClick = { showEditDialog = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                    }
                }
            }
        }

        // --- GRADIENT PROFILE CARD ---
        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF064E3B)))).padding(vertical = 32.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(80.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF8B5CF6), CircleShape), contentAlignment = Alignment.Center) {
                            val initials = currentName.split(" ").take(2).joinToString("") { it.take(1) }.uppercase()
                            Text(text = initials, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.size(20.dp).align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp).background(Color.White, CircleShape).padding(3.dp).background(Color.White, CircleShape))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = currentName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$currentUpi · UPI verified", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BadgeChip(icon = Icons.Default.Bolt, text = "Pro user", bgColor = Color(0xFF312E81), textColor = Color(0xFFA5B4FC))
                        BadgeChip(icon = Icons.Default.Check, text = "KYC done", bgColor = Color(0xFF064E3B), textColor = Color(0xFF34D399))
                    }
                }
            }
        }

        // --- STATS ROW ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(value = TransactionRepo.transactions.size.toString(), label = "Total\nTransactions", cardBg, textColor, subTextColor, modifier = Modifier.weight(1f))
                StatCard(value = EnvelopeRepo.envelopes.size.toString(), label = "Active\nenvelopes", cardBg, textColor, subTextColor, modifier = Modifier.weight(1f))
                StatCard(value = "87%", label = "Budget\naccuracy", cardBg, Color(0xFF059669), subTextColor, modifier = Modifier.weight(1f))
            }
        }

        // --- SETTINGS GROUPS ---
        item {
            Text("ACCOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = subTextColor, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(icon = Icons.Default.Person, iconBg = Color(0xFFEDE9FE), iconColor = Color(0xFF7C3AED), title = "Personal info", subtitle = "Name, phone, email", textColor = textColor, subTextColor = subTextColor, onClick = { showEditDialog = true })
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(icon = Icons.Default.CreditCard, iconBg = Color(0xFFE0F2FE), iconColor = Color(0xFF0284C7), title = "UPI IDs", subtitle = "$currentUpi · 1 linked", textColor = textColor, subTextColor = subTextColor, onClick = { showEditDialog = true })
                }
            }
        }

        item {
            Text("PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = subTextColor, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsToggleRow(icon = Icons.Default.Notifications, iconBg = Color(0xFFFFEDD5), iconColor = Color(0xFFEA580C), title = "Notifications", subtitle = "Payment alerts, reminders", textColor = textColor, subTextColor = subTextColor, isChecked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }
            }
        }

        // --- LOGOUT BUTTON ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log out securely", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "FinFurcate v1.0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Developed with ❤️ by Tasneem", fontSize = 12.sp, color = subTextColor.copy(alpha = 0.7f))
            }
        }
    }
}

// --- SUB-COMPONENTS ---
@Composable
fun BadgeChip(icon: ImageVector, text: String, bgColor: Color, textColor: Color) {
    Row(modifier = Modifier.background(bgColor, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatCard(value: String, label: String, cardBg: Color, valueColor: Color, labelColor: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardBg), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, color = labelColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, iconBg: Color, iconColor: Color, title: String, subtitle: String, textColor: Color, subTextColor: Color, onClick: () -> Unit, trailingText: String? = null, trailingTextColor: Color = Color.Gray) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 16.sp, color = textColor, fontWeight = FontWeight.SemiBold); Text(text = subtitle, fontSize = 13.sp, color = subTextColor) }
        if (trailingText != null) { Text(text = trailingText, color = trailingTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = subTextColor)
    }
}

@Composable
fun SettingsToggleRow(icon: ImageVector, iconBg: Color, iconColor: Color, title: String, subtitle: String, textColor: Color, subTextColor: Color, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 16.sp, color = textColor, fontWeight = FontWeight.SemiBold); Text(text = subtitle, fontSize = 13.sp, color = subTextColor) }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6200EE)))
    }
}