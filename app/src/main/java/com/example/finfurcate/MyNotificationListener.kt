package com.example.finfurcate

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.regex.Pattern

class MyNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("FINFURGATE_TEST", "✅ LISTENER CONNECTED! Ready.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: "Unknown"
        val extras = sbn?.notification?.extras

        // Grab everything to ensure we don't miss Envelope names from GPay
        val title = extras?.getString("android.title") ?: ""
        val text = extras?.getString("android.text") ?: ""
        val subText = extras?.getString("android.subText") ?: ""
        val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
        val textLinesArray = extras?.getCharSequenceArray("android.textLines")
        val textLines = textLinesArray?.joinToString(" ") ?: ""

        val fullMessage = "$title $text $subText $bigText $textLines"
        val lowerText = fullMessage.lowercase()

        // Broadened App Filter
        val isBankingApp = packageName.contains("messaging") ||
                packageName.contains("mms") ||
                packageName.contains("sms") ||
                packageName.contains("phonepe") ||
                packageName.contains("paytm") ||
                packageName.contains("google.android.apps.nbu.paisa.user") ||
                packageName.contains("gpay") ||
                packageName.contains("bank") ||
                packageName.contains("upi")

        if (isBankingApp) {
            val hasCurrencyOrColon = lowerText.contains("rs") || lowerText.contains("inr") || lowerText.contains(":") || lowerText.contains("₹") || lowerText.contains("bal ")
            val isOfficialFormat = hasCurrencyOrColon && (
                    lowerText.contains("debited") || lowerText.contains("credited") ||
                            lowerText.contains("spent") || lowerText.contains("withdrawal") ||
                            lowerText.contains("txn") || lowerText.contains("a/c") ||
                            lowerText.contains("acct") || lowerText.contains("bal") ||
                            lowerText.contains("received") || lowerText.contains("paid")
                    )
            val isSpam = lowerText.contains("hello") || lowerText.contains("hi ") || lowerText.contains("request")

            if (isOfficialFormat && !isSpam) {
                val amount = extractAmount(lowerText)
                val bankBalance = extractBalance(lowerText)

                val currentUpi = UserPrefs.getUpi(this)
                if (currentUpi.isEmpty()) return

                // Force the background service to load the latest data
                if (EnvelopeRepo.envelopes.isEmpty()) EnvelopeRepo.load(this, currentUpi)

                var currentSpent = MoneyPrefs.loadSpent(this, currentUpi)
                var currentBalance = MoneyPrefs.loadBalance(this, currentUpi)

                // A. Handle Spending (Debits)
                val isSpend = lowerText.contains("debited") || lowerText.contains("spent") || lowerText.contains("sent") || lowerText.contains("withdrawal") || (lowerText.contains("paid") && !lowerText.contains("paid you"))

                if (amount > 0.0 && isSpend) {
                    currentSpent += amount
                    MoneyData.totalSpent = currentSpent // Update live UI
                    Log.d("FINFURGATE_TEST", "💰 SPENT: $amount")

                    val pendingEnvName = ActiveSession.getPending(this)

                    if (pendingEnvName != null) {
                        val env = EnvelopeRepo.envelopes.find { it.name == pendingEnvName }
                        if (env != null) {
                            EnvelopeRepo.updateAmount(this, currentUpi, env, -amount)
                            TransactionRepo.addTransaction(this, currentUpi, "Paid from $pendingEnvName (Auto)", amount, false)
                        }
                        ActiveSession.savePending(this, null)
                    } else {
                        TransactionRepo.addTransaction(this, currentUpi, "Bank Auto-Track", amount, false)
                    }
                }

                // B. Handle Income (Credits)
                val isIncome = lowerText.contains("credited") || lowerText.contains("received") || lowerText.contains("paid you")
                if (amount > 0.0 && isIncome && !isSpend) {
                    Log.d("FINFURGATE_TEST", "💵 INCOME: $amount")

                    var matchedEnvName: String? = null
                    for (env in EnvelopeRepo.envelopes) {
                        if (lowerText.contains(env.name.lowercase())) {
                            matchedEnvName = env.name
                            EnvelopeRepo.updateAmount(this, currentUpi, env, amount)
                            break
                        }
                    }

                    if (matchedEnvName != null) {
                        TransactionRepo.addTransaction(this, currentUpi, "Received for $matchedEnvName (Auto)", amount, true)
                    } else {
                        TransactionRepo.addTransaction(this, currentUpi, "Deposit / Received", amount, true)
                    }
                }

                // C. Handle Balance Update
                if (bankBalance > 0.0) {
                    currentBalance = bankBalance
                    MoneyData.currentBalance = currentBalance // Update live UI
                    Log.d("FINFURGATE_TEST", "🏦 BALANCE UPDATED: $bankBalance")
                }

                // 👇 NO MORE RED ERRORS: Uses your restored MoneyPrefs logic!
                MoneyPrefs.save(this, currentUpi, currentSpent, currentBalance)
            }
        }
    }

    private fun extractAmount(message: String): Double {
        val pattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        val matcher = pattern.matcher(message)
        if (matcher.find()) {
            return matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    // 👇 UPGRADED: Skips account numbers and looks for the actual money!
    private fun extractBalance(message: String): Double {
        // 1. Try to find a number that specifically has a decimal point (e.g., 15680.91)
        val decimalPattern = Pattern.compile("(?i)bal(?:ance|ances|\\.)?.*?([0-9,]+\\.[0-9]{1,2})")
        val decimalMatcher = decimalPattern.matcher(message)
        if (decimalMatcher.find()) {
            return decimalMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }

        // 2. Fallback: If there is no decimal, find the LAST number in the sentence (skipping the account number)
        val integerPattern = Pattern.compile("(?i)bal(?:ance|ances|\\.)?.*\\b([0-9,]+)\\b")
        val integerMatcher = integerPattern.matcher(message)
        if (integerMatcher.find()) {
            return integerMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }

        return 0.0
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}