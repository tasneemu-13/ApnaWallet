package com.example.finfurcate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.* // 👈 Added for animation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan // 👈 Added for grid spanning
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale // 👈 Added for heart scaling
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.finfurcate.ui.theme.FinFurcateTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

// Uses SharedPreferences so Android can't kill the memory!
object ActiveSession {
    fun savePending(context: Context, name: String?) {
        context.getSharedPreferences("session", Context.MODE_PRIVATE).edit().putString("pending", name).apply()
    }
    fun getPending(context: Context): String? {
        return context.getSharedPreferences("session", Context.MODE_PRIVATE).getString("pending", null)
    }
    var isAwaitingReturn by mutableStateOf(false)
}

data class Note(val id: Long, val text: String, val date: String)

object NoteRepo {
    var notes = mutableStateListOf<Note>()
    fun addNote(context: Context, text: String, upiId: String) {
        val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        notes.add(0, Note(System.currentTimeMillis(), text, date))
        save(context, upiId)
    }
    fun deleteNote(context: Context, note: Note, upiId: String) {
        notes.remove(note)
        save(context, upiId)
    }
    private fun save(context: Context, upiId: String) {
        val prefs = context.getSharedPreferences("notes_$upiId", Context.MODE_PRIVATE)
        prefs.edit().putString("notes_list", com.google.gson.Gson().toJson(notes)).apply()
    }
    fun load(context: Context, upiId: String) {
        val json = context.getSharedPreferences("notes_$upiId", Context.MODE_PRIVATE).getString("notes_list", null)
        notes.clear()
        if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
            notes.addAll(com.google.gson.Gson().fromJson(json, type))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var isDarkMode by remember { mutableStateOf(UserPrefs.isDarkMode(context)) }

            FinFurcateTheme(darkTheme = isDarkMode) {
                AppRoot(
                    isDarkMode = isDarkMode,
                    onThemeToggle = {
                        isDarkMode = !isDarkMode
                        UserPrefs.setDarkMode(context, isDarkMode)
                    }
                )
            }
        }
    }
}

@Composable
fun AppRoot(isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    val context = LocalContext.current
    var upiId by remember { mutableStateOf(UserPrefs.getUpi(context)) }

    LaunchedEffect(upiId) {
        if (upiId.isNotEmpty()) {
            MoneyData.totalSpent = MoneyPrefs.loadSpent(context, upiId)
            MoneyData.currentBalance = MoneyPrefs.loadBalance(context, upiId)
            EnvelopeRepo.load(context, upiId)
            TransactionRepo.load(context, upiId)
            NoteRepo.load(context, upiId)
        }
    }

    if (upiId.isEmpty()) {
        OnboardingScreen(onComplete = { name, upi ->
            UserPrefs.save(context, name, upi)
            upiId = upi
        }, isDarkMode = isDarkMode)
    } else {
        MainScreenManager(
            upiId = upiId,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle,
            onProfileDeleted = {
                UserPrefs.clear(context)
                MoneyData.totalSpent = 0.0
                MoneyData.currentBalance = 0.0
                EnvelopeRepo.envelopes.clear()
                TransactionRepo.transactions.clear()
                NoteRepo.notes.clear()
                ActiveSession.savePending(context, null)
                upiId = ""
            }
        )
    }
}

@Composable
fun OnboardingScreen(onComplete: (String, String) -> Unit, isDarkMode: Boolean) {
    var name by remember { mutableStateOf("") }
    var upi by remember { mutableStateOf("") }
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF6200EE))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Welcome to FinFurcate", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor)
        Text("Let's set up your receiving account", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = upi, onValueChange = { upi = it }, label = { Text("Your UPI ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { if (name.isNotBlank() && upi.isNotBlank()) onComplete(name, upi) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) {
            Text("Get Started", fontSize = 16.sp)
        }
    }
}

@Composable
fun MainScreenManager(upiId: String, isDarkMode: Boolean, onThemeToggle: () -> Unit, onProfileDeleted: () -> Unit) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf(UserPrefs.getName(context)) }

    var showEnvelopeDialog by remember { mutableStateOf(false) }
    var selectedEnvelope by remember { mutableStateOf<BudgetEnvelope?>(null) }

    var currentScreen by remember { mutableStateOf("Home") }
    val navColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    if (showEnvelopeDialog) {
        AddEnvelopeDialog(
            onDismiss = { showEnvelopeDialog = false },
            onConfirm = { name, limit ->
                EnvelopeRepo.add(context, upiId, name, limit.toDoubleOrNull() ?: 0.0)
                showEnvelopeDialog = false
            }
        )
    }

    selectedEnvelope?.let { envelope ->
        EnvelopeManagerDialog(
            envelope = envelope, context = context, upiId = upiId,
            onDismiss = { selectedEnvelope = null },
            onUpdate = { amount ->
                EnvelopeRepo.updateAmount(context, upiId, envelope, amount)
                val title = if (amount > 0) "Added to ${envelope.name}" else "Spent from ${envelope.name} (Manual)"
                TransactionRepo.addTransaction(context, upiId, title, kotlin.math.abs(amount), isCredit = amount > 0)
                selectedEnvelope = null
            },
            onDelete = { EnvelopeRepo.delete(context, upiId, envelope); selectedEnvelope = null }
        )
    }

    Scaffold(
        bottomBar = {
            FinBottomNav(
                currentScreen = currentScreen,
                onNavigate = { screen -> currentScreen = screen },
                navColor = navColor
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                // 👇 FAB IS NOW INTERACTIVE! It instantly opens the Create Envelope dialog.
                onClick = { showEnvelopeDialog = true },
                containerColor = Color(0xFF2A2A2A),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).offset(y = 45.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Envelope", modifier = Modifier.size(32.dp))
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (currentScreen) {
                    "Notes" -> NotepadScreen(upiId = upiId, onBack = { currentScreen = "Home" }, isDarkMode = isDarkMode)
                    "Support" -> ChatbotScreen(onBack = { currentScreen = "Home" }, isDarkMode = isDarkMode)
                    "History" -> TransactionHistoryScreen(onBack = { currentScreen = "Home" }, isDarkMode = isDarkMode)

                    "Wallets" -> WalletsScreen(
                        isDarkMode = isDarkMode,
                        onEnvelopeClick = { env -> selectedEnvelope = env },
                        onCreateClick = { showEnvelopeDialog = true }
                    )

                    "Stats" -> StatsScreen(
                        isDarkMode = isDarkMode,
                        onBack = { currentScreen = "Home" }
                    )

                    "Profile" -> ProfileScreen(
                        currentName = userName,
                        currentUpi = upiId,
                        isDarkMode = isDarkMode,
                        onBack = { currentScreen = "Home" },
                        onSaveProfile = { newName, newUpi ->
                            UserPrefs.save(context, newName, newUpi)
                            userName = newName
                        },
                        onLogout = onProfileDeleted
                    )

                    else -> DashboardScreen( // Home
                        upiId = upiId,
                        isDarkMode = isDarkMode,
                        onMenuClick = { currentScreen = "Profile" },
                        onNotesClick = { currentScreen = "Notes" },
                        onSupportClick = { currentScreen = "Support" },
                        onThemeToggle = onThemeToggle,
                        onEnvelopeClick = { selectedEnvelope = it },
                        onCreateEnvelopeClick = { showEnvelopeDialog = true }
                    )
                }
            }
        }
    )
}

// 👇 THE NEW ANIMATED SIGNATURE
@Composable
fun AnimatedSignature() {
    val infiniteTransition = rememberInfiniteTransition(label = "heartBeat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f, // How big the heart gets
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Developed with ", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text("❤️", modifier = Modifier.scale(scale))
            Text(" by Tasneem", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatbotScreen(onBack: () -> Unit, isDarkMode: Boolean) {
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val botBubbleColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    var messages by remember {
        mutableStateOf(listOf(ChatMessage("Hi there! I'm FinBot. Ask me anything about tracking expenses, auto-pay, or envelopes!", false)))
    }
    var inputText by remember { mutableStateOf("") }

    fun getBotResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("auto") || q.contains("gpay") || q.contains("paytm") || q.contains("phonepe") -> "When you tap 'Auto-Pay' in an envelope and choose your app, I wait for your bank SMS and automatically deduct the exact amount from that envelope."
            q.contains("balance") -> "I update your balance automatically when your bank sends an SMS! You can also click the Check Balance button."
            q.contains("envelope") || q.contains("fund") || q.contains("budget") -> "Envelopes help you budget! You can use them for savings goals (start at 0 and add money) or spending limits (load them up and spend down)."
            q.contains("qr") || q.contains("receive") -> "Tap the 'Receive' tool to generate a custom QR code. When someone scans it, the money goes straight to your UPI ID."
            q.contains("hi") || q.contains("hello") -> "Hello! How can I help you manage your finances today?"
            q.contains("not updating") || q.contains("zero") -> "Please make sure 'Notification Access' is turned ON in your phone settings. I need it to read bank messages!"
            else -> "I'm still learning! Try asking me about Auto-Pay, Envelopes, Checking Balance, or QR codes."
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor) }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.SmartToy, contentDescription = "Bot", tint = Color(0xFF6200EE), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("FinBot Assistant", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = true) {
            items(messages.reversed()) { msg ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                    Box(modifier = Modifier.background(color = if (msg.isUser) Color(0xFF6200EE) else botBubbleColor, shape = RoundedCornerShape(16.dp)).padding(12.dp).widthIn(max = 280.dp)) {
                        Text(text = msg.text, color = if (msg.isUser) Color.White else textColor, fontSize = 15.sp)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(if (isDarkMode) Color(0xFF1E1E1E) else Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it },
                placeholder = { Text("Message FinBot...") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp), singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                if (inputText.isNotBlank()) {
                    val userText = inputText
                    messages = messages + ChatMessage(userText, true)
                    inputText = ""
                    messages = messages + ChatMessage(getBotResponse(userText), false)
                }
            }, modifier = Modifier.background(Color(0xFF6200EE), CircleShape)) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun DashboardScreen(
    upiId: String,
    isDarkMode: Boolean,
    onMenuClick: () -> Unit,
    onNotesClick: () -> Unit,
    onSupportClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onEnvelopeClick: (BudgetEnvelope) -> Unit,
    onCreateEnvelopeClick: () -> Unit
) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf(UserPrefs.getName(context)) }
    var showBankDialog by remember { mutableStateOf(false) }
    var showQrSelector by remember { mutableStateOf(false) }
    var envelopeForQr by remember { mutableStateOf<BudgetEnvelope?>(null) }

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    val lifecycleOwner = LocalLifecycleOwner.current
    var showMissingSmsDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                MoneyData.totalSpent = MoneyPrefs.loadSpent(context, upiId)
                MoneyData.currentBalance = MoneyPrefs.loadBalance(context, upiId)
                EnvelopeRepo.load(context, upiId)
                TransactionRepo.load(context, upiId)

                val pendingName = ActiveSession.getPending(context)
                if (ActiveSession.isAwaitingReturn && pendingName != null) {
                    showMissingSmsDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showMissingSmsDialog) {
        var manualAmount by remember { mutableStateOf("") }
        val envName = ActiveSession.getPending(context) ?: ""
        AlertDialog(
            onDismissRequest = { showMissingSmsDialog = false; ActiveSession.isAwaitingReturn = false; ActiveSession.savePending(context, null) },
            title = { Text("Did you pay $envName?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("We haven't received a bank SMS yet. If you completed the payment, log it manually.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = manualAmount, onValueChange = { manualAmount = it }, label = { Text("Amount Spent") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), onClick = {
                    val amt = manualAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val env = EnvelopeRepo.envelopes.find { it.name == envName }
                        if (env != null) {
                            EnvelopeRepo.updateAmount(context, upiId, env, -amt)
                            TransactionRepo.addTransaction(context, upiId, "Paid $envName (Manual)", amt, false)
                            MoneyData.totalSpent += amt
                            MoneyData.currentBalance -= amt
                            MoneyPrefs.save(context, upiId, MoneyData.totalSpent, MoneyData.currentBalance)
                        }
                    }
                    showMissingSmsDialog = false; ActiveSession.isAwaitingReturn = false; ActiveSession.savePending(context, null)
                }) { Text("Save Manually") }
            },
            dismissButton = { TextButton(onClick = { showMissingSmsDialog = false; ActiveSession.isAwaitingReturn = false; ActiveSession.savePending(context, null) }) { Text("Wait for SMS") } }
        )
    }

    if (showBankDialog) { Dialog(onDismissRequest = { showBankDialog = false }) { Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(400.dp)) { BankSelectionScreen(onBack = { showBankDialog = false }) } } }

    if (showQrSelector) {
        AlertDialog(
            onDismissRequest = { showQrSelector = false }, title = { Text("Generate QR For:") },
            text = {
                if (EnvelopeRepo.envelopes.isEmpty()) { Text("Create an envelope first!", color = Color.Gray) } else {
                    LazyColumn { items(EnvelopeRepo.envelopes) { env -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { envelopeForQr = env; showQrSelector = false }, colors = CardDefaults.cardColors(containerColor = surfaceColor)) { Text(text = env.name, color=textColor, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) } } }
                }
            },
            confirmButton = { TextButton(onClick = { showQrSelector = false }) { Text("Cancel") } }
        )
    }

    envelopeForQr?.let { env ->
        val myUpiId = UserPrefs.getUpi(context)
        val myName = UserPrefs.getName(context)
        val upiString = "upi://pay?pa=$myUpiId&pn=$myName&tn=For ${env.name} Envelope&cu=INR"
        val qrBitmap = remember { generateQrBitmap(upiString) }

        AlertDialog(
            onDismissRequest = { envelopeForQr = null },
            title = { Text(text = "Receive: ${env.name}", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    qrBitmap?.let { bmp -> Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(220.dp)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scan to add money directly here.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                }
            },
            confirmButton = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { OutlinedButton(onClick = { qrBitmap?.let { bmp -> shareQrCode(context, bmp, env.name) } }) { Text("Share") }; Button(onClick = { envelopeForQr = null }) { Text("Done") } } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = textColor) }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Welcome Back,", fontSize = 14.sp, color = Color.Gray)
                    Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
            IconButton(onClick = onThemeToggle, modifier = Modifier.size(48.dp).background(surfaceColor, CircleShape)) {
                Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = textColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), modifier = Modifier.fillMaxWidth().height(160.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Total Spent Today", color = Color.Gray, fontSize = 14.sp)
                Text(text = "Rs. ${MoneyData.totalSpent}", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(text = "Available: Rs. ${MoneyData.currentBalance}", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showBankDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check Balance (Missed Call)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Quick Tools", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickToolCard("Receive", Icons.Default.QrCode, surfaceColor, textColor) { showQrSelector = true }
            QuickToolCard("Notepad", Icons.Default.Edit, surfaceColor, textColor, onNotesClick)
            QuickToolCard("FinBot", Icons.Default.SmartToy, surfaceColor, textColor, onSupportClick)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "My Envelopes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(bottom = 12.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            item { CreateEnvelopeButton(onClick = onCreateEnvelopeClick, surfaceColor = surfaceColor, textColor = textColor) }
            items(EnvelopeRepo.envelopes) { envelope -> EnvelopeCard(envelope = envelope, onClick = { onEnvelopeClick(envelope) }) }

            // 👇 ATTACHED THE SIGNATURE HERE
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnimatedSignature()
            }
        }
    }
}

@Composable
fun QuickToolCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, textColor: Color, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bgColor), modifier = Modifier.size(105.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = Color(0xFF6200EE), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun NotepadScreen(upiId: String, onBack: () -> Unit, isDarkMode: Boolean) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    if (showAddDialog) {
        var noteText by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("New Note") }, text = { OutlinedTextField(value = noteText, onValueChange = { noteText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Write something...") }) }, confirmButton = { Button(onClick = { if (noteText.isNotBlank()) NoteRepo.addNote(context, noteText, upiId); showAddDialog = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } })
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("My Notepad", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        if (NoteRepo.notes.isEmpty()) { Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No notes yet.", color = Color.Gray) } } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NoteRepo.notes) { note ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                        Column(modifier = Modifier.padding(16.dp)) { Text(text = note.date, fontSize = 12.sp, color = Color.Gray); Spacer(modifier = Modifier.height(4.dp)); Text(text = note.text, fontSize = 16.sp, color = textColor); Spacer(modifier = Modifier.height(8.dp)); TextButton(onClick = { NoteRepo.deleteNote(context, note, upiId) }, contentPadding = PaddingValues(0.dp)) { Text("Delete", color = Color.Red, fontSize = 12.sp) } }
                    }
                }
            }
        }
        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) { Icon(Icons.Default.Add, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Add Note") }
    }
}

@Composable
fun TransactionHistoryScreen(onBack: () -> Unit, isDarkMode: Boolean) {
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Transaction History", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        if (TransactionRepo.transactions.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No transactions yet.", color = Color.Gray) } } else {
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TransactionRepo.transactions) { txn -> TransactionCard(txn, cardColor, textColor) }
            }
        }
    }
}

@Composable
fun TransactionCard(txn: TransactionLog, cardColor: Color, textColor: Color) {
    val amountColor = if (txn.isCredit) Color(0xFF4CAF50) else Color(0xFFE57373)
    val prefix = if (txn.isCredit) "+" else "-"
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(text = txn.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor); Spacer(modifier = Modifier.height(4.dp)); Text(text = txn.formattedDate, fontSize = 12.sp, color = Color.Gray) }
            Text(text = "$prefix Rs. ${txn.amount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = amountColor)
        }
    }
}

@Composable
fun ProfileDialog(currentName: String, currentUpi: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    var upi by remember { mutableStateOf(currentUpi) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("My Profile", fontWeight = FontWeight.Bold) }, text = { Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = upi, onValueChange = { upi = it }, label = { Text("UPI ID") }, singleLine = true) } }, confirmButton = { Button(onClick = { onSave(name, upi) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) { Text("Save") } }, dismissButton = { TextButton(onClick = onDelete) { Text("Delete Profile", color = Color.Red) } })
}

@Composable
fun EnvelopeManagerDialog(envelope: BudgetEnvelope, context: Context, upiId: String, onDismiss: () -> Unit, onUpdate: (Double) -> Unit, onDelete: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var showAppChooser by remember { mutableStateOf(false) }
    val color = Color(envelope.colorHex)

    fun launchPaymentApp(packageName: String, appName: String) {
        ActiveSession.savePending(context, envelope.name)
        ActiveSession.isAwaitingReturn = true
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "$appName is not installed!", Toast.LENGTH_SHORT).show()
            ActiveSession.isAwaitingReturn = false
            ActiveSession.savePending(context, null)
        }
        showAppChooser = false
        onDismiss()
    }

    if (showAppChooser) {
        AlertDialog(
            onDismissRequest = { showAppChooser = false },
            title = { Text("Select Payment App", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButton(onClick = { launchPaymentApp("com.google.android.apps.nbu.paisa.user", "GPay") }, modifier = Modifier.fillMaxWidth()) { Text("Google Pay (GPay)", fontSize = 16.sp, color = Color(0xFF4285F4)) }
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    TextButton(onClick = { launchPaymentApp("com.phonepe.app", "PhonePe") }, modifier = Modifier.fillMaxWidth()) { Text("PhonePe", fontSize = 16.sp, color = Color(0xFF5E35B1)) }
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    TextButton(onClick = { launchPaymentApp("net.one97.paytm", "Paytm") }, modifier = Modifier.fillMaxWidth()) { Text("Paytm", fontSize = 16.sp, color = Color(0xFF03A9F4)) }
                }
            },
            confirmButton = { TextButton(onClick = { showAppChooser = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = envelope.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Capacity / Goal: Rs. ${envelope.limit.toInt()}", color = Color.Gray, fontSize = 14.sp)
                Text(text = "Inside Envelope: Rs. ${envelope.currentAmount.toInt()}", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAppChooser = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212))) { Text("Auto-Pay via...") }
                Spacer(modifier = Modifier.height(16.dp))

                Text("OR Add/Spend Manually", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { val a = amountText.toDoubleOrNull() ?: 0.0; if (a > 0) onUpdate(-a) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))) { Text("Spend") }
                    Button(onClick = { val a = amountText.toDoubleOrNull() ?: 0.0; if (a > 0) onUpdate(a) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784))) { Text("Add") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDelete) { Text("Delete Envelope", color = Color.Gray, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun CreateEnvelopeButton(onClick: () -> Unit, surfaceColor: Color, textColor: Color) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = surfaceColor), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth().height(140.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.Gray, modifier = Modifier.size(40.dp)); Spacer(modifier = Modifier.height(8.dp)); Text(text = "Create New", color = textColor, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun EnvelopeCard(envelope: BudgetEnvelope, onClick: () -> Unit) {
    val color = Color(envelope.colorHex)
    val progress = if (envelope.limit > 0) (envelope.currentAmount / envelope.limit).toFloat().coerceIn(0f, 1f) else 0f

    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth().height(140.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
            Column {
                Text(text = envelope.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Rs. ${envelope.currentAmount.toInt()} / ${envelope.limit.toInt()}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AddEnvelopeDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Envelope", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Set a Capacity. You can fill this up for a Goal, or pre-load it to limit your spending.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Food, Paint)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = limit, onValueChange = { limit = it }, label = { Text("Capacity / Goal (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, limit) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun generateQrBitmap(text: String): Bitmap? {
    return try { val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512); val w = bitMatrix.width; val h = bitMatrix.height; val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565); for (x in 0 until w) { for (y in 0 until h) { bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE) } }; bmp } catch (e: Exception) { null }
}

fun shareQrCode(context: Context, bitmap: Bitmap, envName: String) {
    try { val file = File(File(context.cacheDir, "images").apply { mkdirs() }, "qr_$envName.png"); val stream = FileOutputStream(file); bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); stream.close(); val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file); val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, "Scan to pay for $envName!"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(Intent.createChooser(shareIntent, "Share QR")) } catch (e: Exception) { e.printStackTrace() }
}

@Composable
fun FinBottomNav(currentScreen: String, onNavigate: (String) -> Unit, navColor: Color) {
    NavigationBar(
        containerColor = navColor,
        tonalElevation = 8.dp,
        modifier = Modifier.height(80.dp)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            selected = currentScreen == "Home",
            onClick = { onNavigate("Home") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF6200EE), selectedTextColor = Color(0xFF6200EE), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallets") },
            label = { Text("Wallets", fontSize = 10.sp) },
            selected = currentScreen == "Wallets",
            onClick = { onNavigate("Wallets") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF6200EE), selectedTextColor = Color(0xFF6200EE), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
            label = { Text("Stats", fontSize = 10.sp) },
            selected = currentScreen == "Stats",
            onClick = { onNavigate("Stats") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF6200EE), selectedTextColor = Color(0xFF6200EE), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 10.sp) },
            selected = currentScreen == "Profile",
            onClick = { onNavigate("Profile") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF6200EE), selectedTextColor = Color(0xFF6200EE), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color.Transparent)
        )
    }
}