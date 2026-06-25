package com.example.group.calculator

import com.example.group.data.entity.MemberEntity
import com.example.group.data.entity.ExpenseParticipantEntity
import com.example.group.data.entity.ExpensePayerEntity
import com.example.group.data.entity.SettlementEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupCalculatorsTest {

    @Test
    fun testEqualSplitCalculator() {
        val participants = listOf(1, 2, 3)
        val shares = SplitCalculator.calculateSplit(
            method = SplitCalculator.SplitMethod.EQUAL,
            totalAmount = 100.00,
            participants = participants,
            inputs = emptyMap()
        )

        // 100 / 3 = 33.33 with 0.01 rounding error allocated to the first participant (1)
        assertEquals(33.34, shares[1] ?: 0.0, 0.001)
        assertEquals(33.33, shares[2] ?: 0.0, 0.001)
        assertEquals(33.33, shares[3] ?: 0.0, 0.001)
        assertEquals(100.0, shares.values.sum(), 0.001)
    }

    @Test
    fun testSharesSplitCalculator() {
        val participants = listOf(1, 2)
        val inputs = mapOf(1 to 2.0, 2 to 1.0) // 2:1 ratio
        val shares = SplitCalculator.calculateSplit(
            method = SplitCalculator.SplitMethod.SHARES,
            totalAmount = 90.0,
            participants = participants,
            inputs = inputs
        )

        assertEquals(60.0, shares[1] ?: 0.0, 0.001)
        assertEquals(30.0, shares[2] ?: 0.0, 0.001)
    }

    @Test
    fun testBalanceCalculator() {
        val members = listOf(
            MemberEntity(id = 1, groupId = 1, name = "Alice", avatarUri = null, defaultWeight = 1.0, notes = null, createdDate = 0L),
            MemberEntity(id = 2, groupId = 1, name = "Bob", avatarUri = null, defaultWeight = 1.0, notes = null, createdDate = 0L)
        )

        val payers = listOf(
            ExpensePayerEntity(id = 1, expenseId = 1, memberId = 1, paidAmount = 100.0)
        )

        val participants = listOf(
            ExpenseParticipantEntity(id = 1, expenseId = 1, memberId = 1, shareAmount = 50.0, splitMethod = "EQUAL"),
            ExpenseParticipantEntity(id = 2, expenseId = 1, memberId = 2, shareAmount = 50.0, splitMethod = "EQUAL")
        )

        val settlements = listOf(
            SettlementEntity(id = 1, groupId = 1, payerId = 2, payeeId = 1, amount = 20.0, date = 0L, notes = null)
        )

        val balances = BalanceCalculator.calculateBalances(
            members = members,
            payers = payers,
            participants = participants,
            settlements = settlements
        )

        // Alice paid 100, share 50, received 20 settlement. Balance = 100 - 50 - 20 = +30
        assertEquals(30.0, balances[1] ?: 0.0, 0.001)
        // Bob paid 0, share 50, sent 20 settlement. Balance = 0 - 50 + 20 = -30
        assertEquals(-30.0, balances[2] ?: 0.0, 0.001)
    }

    @Test
    fun testSettlementGenerator() {
        // Alice is owed 30, Bob owes 30
        val balances = mapOf(1 to 30.0, 2 to -30.0)
        val suggested = SettlementGenerator.generateSuggestedSettlements(balances)

        assertEquals(1, suggested.size)
        val sug = suggested.first()
        assertEquals(2, sug.debtorId)  // Bob pays
        assertEquals(1, sug.creditorId) // Alice receives
        assertEquals(30.0, sug.amount, 0.001)
    }
}
