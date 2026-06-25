package com.example.group.calculator

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

object SettlementGenerator {

    data class SuggestedSettlement(
        val debtorId: Int,
        val creditorId: Int,
        val amount: Double
    )

    /**
     * Minimizes the number of payments required to settle all debts.
     * Returns a List of SuggestedSettlements.
     */
    fun generateSuggestedSettlements(
        balances: Map<Int, Double>
    ): List<SuggestedSettlement> {
        // Collect debtors (balance < 0) and creditors (balance > 0)
        val debtors = mutableListOf<Pair<Int, Double>>()
        val creditors = mutableListOf<Pair<Int, Double>>()

        for ((id, bal) in balances) {
            val rounded = round(bal * 100.0) / 100.0
            if (rounded < -0.01) {
                debtors.add(id to rounded)
            } else if (rounded > 0.01) {
                creditors.add(id to rounded)
            }
        }

        // Sort: most negative first (debtors), most positive first (creditors)
        debtors.sortBy { it.second }
        creditors.sortByDescending { it.second }

        val suggestions = mutableListOf<SuggestedSettlement>()
        var debtorIdx = 0
        var creditorIdx = 0

        val activeDebtors = debtors.toMutableList()
        val activeCreditors = creditors.toMutableList()

        while (debtorIdx < activeDebtors.size && creditorIdx < activeCreditors.size) {
            val debtor = activeDebtors[debtorIdx]
            val creditor = activeCreditors[creditorIdx]

            val debtVal = abs(debtor.second)
            val creditVal = creditor.second

            val payment = round(min(debtVal, creditVal) * 100.0) / 100.0
            if (payment > 0.0) {
                suggestions.add(
                    SuggestedSettlement(
                        debtorId = debtor.first,
                        creditorId = creditor.first,
                        amount = payment
                    )
                )
            }

            // Update remaining balances
            val nextDebtorBal = round((debtor.second + payment) * 100.0) / 100.0
            val nextCreditorBal = round((creditor.second - payment) * 100.0) / 100.0

            activeDebtors[debtorIdx] = debtor.first to nextDebtorBal
            activeCreditors[creditorIdx] = creditor.first to nextCreditorBal

            if (abs(nextDebtorBal) < 0.01) {
                debtorIdx++
            }
            if (abs(nextCreditorBal) < 0.01) {
                creditorIdx++
            }
        }

        return suggestions
    }
}
