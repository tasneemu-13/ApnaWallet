package com.example.finfurcate
import androidx.compose.ui.graphics.Color

data class Bank (
    val name: String,
    val number: String,
    val color: Color
)
object BankData {

    val list = listOf(
        // Top Indian Banks for Missed Call Balance
            Bank("SBI", "9223766666", Color(0xFF283593)),       // State Bank of India
            Bank("HDFC", "18002703333", Color(0xFF004D40)),     // HDFC Bank
            Bank("ICICI", "9594612612", Color(0xFFE65100)),     // ICICI Bank
            Bank("Axis", "18004195959", Color(0xFF880E4F)),     // Axis Bank
            Bank("Kotak", "18002740110", Color(0xFFB71C1C)),    // Kotak Mahindra
            Bank("PNB", "18001802223", Color(0xFFFBC02D)),      // Punjab National Bank
            Bank("BOI", "9811255430", Color(0xFF1A237E)),       // Bank of India
            Bank("Union", "09223008586", Color(0xFFD32F2F))     // Union Bank

    )
}