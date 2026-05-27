package com.example.finfurcate

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionLog(val id: Long, val title: String, val amount: Double, val isCredit: Boolean, val formattedDate: String)

object TransactionRepo {
    var transactions = mutableStateListOf<TransactionLog>()

    // Added upiId parameter
    fun addTransaction(context: Context, upiId: String, title: String, amount: Double, isCredit: Boolean) {
        val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        transactions.add(0, TransactionLog(System.currentTimeMillis(), title, amount, isCredit, date))
        save(context, upiId)
    }

    // Isolated save file using upiId
    private fun save(context: Context, upiId: String) {
        val prefs = context.getSharedPreferences("txns_$upiId", Context.MODE_PRIVATE)
        prefs.edit().putString("history", Gson().toJson(transactions)).apply()
    }

    // Isolated load file using upiId
    fun load(context: Context, upiId: String) {
        val prefs = context.getSharedPreferences("txns_$upiId", Context.MODE_PRIVATE)
        val json = prefs.getString("history", null)
        transactions.clear()
        if (json != null) {
            val type = object : TypeToken<List<TransactionLog>>() {}.type
            val loaded: List<TransactionLog> = Gson().fromJson(json, type)
            transactions.addAll(loaded)
        }
    }
}