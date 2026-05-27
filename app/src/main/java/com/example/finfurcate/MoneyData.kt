package com.example.finfurcate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
object MoneyData {
    // variable automatically notifies the screen when it changes
    var totalSpent by mutableStateOf(0.0)
    var currentBalance by mutableStateOf(0.0)
}