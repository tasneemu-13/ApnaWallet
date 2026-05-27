import android.content.Context



object MoneyPrefs {

    private const val KEY_TOTAL = "spent"

    private const val KEY_BALANCE = "balance"



// 1. SAVE BOTH NUMBERS (Tied to the specific UPI ID)

    fun save(context: Context, upiId: String, spent: Double, balance: Double) {

        val prefs = context.getSharedPreferences("money_$upiId", Context.MODE_PRIVATE)

        prefs.edit()

            .putString(KEY_TOTAL, spent.toString())

            .putString(KEY_BALANCE, balance.toString())

            .apply()

    }



// 2. LOAD SPENT (For the Black Card)

    fun loadSpent(context: Context, upiId: String): Double {

        val prefs = context.getSharedPreferences("money_$upiId", Context.MODE_PRIVATE)

        val spentString = prefs.getString(KEY_TOTAL, "0.0")

        return spentString?.toDoubleOrNull() ?: 0.0

    }



// 3. LOAD BALANCE (For the Green Pill)

    fun loadBalance(context: Context, upiId: String): Double {

        val prefs = context.getSharedPreferences("money_$upiId", Context.MODE_PRIVATE)

        val balanceString = prefs.getString(KEY_BALANCE, "0.0")

        return balanceString?.toDoubleOrNull() ?: 0.0

    }
}