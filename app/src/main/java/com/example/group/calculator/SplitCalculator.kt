package com.example.group.calculator

import kotlin.math.roundToInt

object SplitCalculator {

    enum class SplitMethod {
        EQUAL, EXACT, PERCENTAGE, SHARES, CUSTOM
    }

    /**
     * Calculates the split shares for members.
     * Returns a Map of Member ID to their calculated Share Amount.
     */
    fun calculateSplit(
        method: SplitMethod,
        totalAmount: Double,
        participants: List<Int>, // list of member IDs participating
        inputs: Map<Int, Double> // inputs per member ID (weights, exact amounts, percentages, etc.)
    ): Map<Int, Double> {
        if (participants.isEmpty()) return emptyMap()
        if (totalAmount <= 0.0) return participants.associateWith { 0.0 }

        return when (method) {
            SplitMethod.EQUAL -> {
                val size = participants.size
                val baseShare = (totalAmount / size * 100.0).roundToInt() / 100.0
                val shares = participants.associateWith { baseShare }.toMutableMap()
                
                // Adjust for decimal rounding errors
                var diff = totalAmount - shares.values.sum()
                if (diff != 0.0) {
                    val first = participants.first()
                    shares[first] = ((shares[first] ?: 0.0) + diff * 100.0).roundToInt() / 100.0
                }
                shares
            }

            SplitMethod.EXACT -> {
                // Ensure exact inputs are defined for all participants
                val shares = participants.associateWith { inputs[it] ?: 0.0 }
                val sum = shares.values.sum()
                require(Math.abs(sum - totalAmount) < 0.02) { "Total of exact shares ($sum) must equal expense amount ($totalAmount)." }
                shares
            }

            SplitMethod.PERCENTAGE -> {
                // Percentage values are in inputs, e.g. Bob = 40.0, Alice = 60.0
                val totalPercent = participants.sumOf { inputs[it] ?: 0.0 }
                require(Math.abs(totalPercent - 100.0) < 0.02) { "Percentages must equal 100% (got $totalPercent%)." }

                val shares = participants.associateWith { id ->
                    val pct = inputs[id] ?: 0.0
                    (totalAmount * pct / 100.0 * 100.0).roundToInt() / 100.0
                }.toMutableMap()

                // Rounding adjustment
                val diff = totalAmount - shares.values.sum()
                if (diff != 0.0) {
                    val first = participants.first()
                    shares[first] = ((shares[first] ?: 0.0) + diff * 100.0).roundToInt() / 100.0
                }
                shares
            }

            SplitMethod.SHARES -> {
                // Weight values in inputs (e.g. Alice = 3.0, Bob = 2.0, Charlie = 1.0)
                val totalWeights = participants.sumOf { inputs[it] ?: 1.0 }
                if (totalWeights <= 0.0) {
                    return calculateSplit(SplitMethod.EQUAL, totalAmount, participants, emptyMap())
                }

                val shares = participants.associateWith { id ->
                    val weight = inputs[id] ?: 1.0
                    (totalAmount * weight / totalWeights * 100.0).roundToInt() / 100.0
                }.toMutableMap()

                // Rounding adjustment
                val diff = totalAmount - shares.values.sum()
                if (diff != 0.0) {
                    val first = participants.first()
                    shares[first] = ((shares[first] ?: 0.0) + diff * 100.0).roundToInt() / 100.0
                }
                shares
            }

            SplitMethod.CUSTOM -> {
                val shares = participants.associateWith { inputs[it] ?: 0.0 }
                val sum = shares.values.sum()
                require(Math.abs(sum - totalAmount) < 0.02) { "Total of custom shares ($sum) must equal expense amount ($totalAmount)." }
                shares
            }
        }
    }
}
