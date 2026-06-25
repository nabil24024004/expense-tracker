package com.example.group.calculator

import com.example.group.data.entity.MemberEntity
import com.example.group.data.entity.ExpenseParticipantEntity
import com.example.group.data.entity.ExpensePayerEntity
import com.example.group.data.entity.SettlementEntity
import kotlin.math.round

object BalanceCalculator {

    /**
     * Calculates derived balances of all members in a group.
     * Returns a Map of Member ID to net Balance (positive = member should receive money, negative = member owes money).
     */
    fun calculateBalances(
        members: List<MemberEntity>,
        payers: List<ExpensePayerEntity>,
        participants: List<ExpenseParticipantEntity>,
        settlements: List<SettlementEntity>
    ): Map<Int, Double> {
        val balances = members.associate { it.id to 0.0 }.toMutableMap()

        // 1. Add Paid amounts from Expenses
        for (payer in payers) {
            val memberId = payer.memberId
            if (balances.containsKey(memberId)) {
                balances[memberId] = (balances[memberId] ?: 0.0) + payer.paidAmount
            }
        }

        // 2. Subtract Share amounts from Expenses
        for (participant in participants) {
            val memberId = participant.memberId
            if (balances.containsKey(memberId)) {
                balances[memberId] = (balances[memberId] ?: 0.0) - participant.shareAmount
            }
        }

        // 3. Adjust for Settlements
        for (settlement in settlements) {
            val sender = settlement.payerId
            val receiver = settlement.payeeId
            
            // Sender gets credit for sending money (increases balance)
            if (balances.containsKey(sender)) {
                balances[sender] = (balances[sender] ?: 0.0) + settlement.amount
            }
            
            // Receiver gets debited for receiving money (decreases balance)
            if (balances.containsKey(receiver)) {
                balances[receiver] = (balances[receiver] ?: 0.0) - settlement.amount
            }
        }

        // Round to 2 decimal places to clear floating point errors
        for (key in balances.keys) {
            val currentVal = balances[key] ?: 0.0
            balances[key] = round(currentVal * 100.0) / 100.0
        }

        return balances
    }
}
