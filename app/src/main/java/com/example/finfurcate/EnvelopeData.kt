package com.example.finfurcate

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class BudgetEnvelope(val id: Long, val name: String, var limit: Double, var currentAmount: Double, val colorHex: Long)

object EnvelopeRepo {
    var envelopes = mutableStateListOf<BudgetEnvelope>()
    private val colors = listOf(0xFFE57373, 0xFF81C784, 0xFF64B5F6, 0xFFFFB74D, 0xFFBA68C8)

    fun add(context: Context, upiId: String, name: String, limit: Double) {
        val color = colors[envelopes.size % colors.size]
        envelopes.add(BudgetEnvelope(System.currentTimeMillis(), name, limit, 0.0, color))
        save(context, upiId)
    }

    fun updateAmount(context: Context, upiId: String, envelope: BudgetEnvelope, amount: Double) {
        val index = envelopes.indexOfFirst { it.id == envelope.id }
        if (index != -1) {
            envelopes[index] = envelope.copy(currentAmount = envelope.currentAmount + amount)
            save(context, upiId)
        }
    }

    fun delete(context: Context, upiId: String, envelope: BudgetEnvelope) {
        envelopes.remove(envelope)
        save(context, upiId)
    }

    private fun save(context: Context, upiId: String) {
        val prefs = context.getSharedPreferences("envs_$upiId", Context.MODE_PRIVATE)
        prefs.edit().putString("list", Gson().toJson(envelopes)).apply()
    }

    fun load(context: Context, upiId: String) {
        val prefs = context.getSharedPreferences("envs_$upiId", Context.MODE_PRIVATE)
        val json = prefs.getString("list", null)
        envelopes.clear()
        if (json != null) {
            val type = object : TypeToken<List<BudgetEnvelope>>() {}.type
            val loaded: List<BudgetEnvelope> = Gson().fromJson(json, type)
            envelopes.addAll(loaded)
        }
    }
}