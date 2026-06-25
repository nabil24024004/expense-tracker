package com.example.group.data.dao

import androidx.room.*
import com.example.group.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    // --- Groups ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroupById(id: Int): Flow<GroupEntity?>

    @Query("SELECT * FROM groups ORDER BY createdDate DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupByIdDirect(groupId: Int): GroupEntity?

    @Query("SELECT * FROM groups WHERE id IN (:groupIds)")
    suspend fun getGroupsByIdsDirect(groupIds: List<Int>): List<GroupEntity>

    // --- Members ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY createdDate ASC")
    fun getMembersByGroupId(groupId: Int): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY createdDate ASC")
    suspend fun getMembersByGroupIdDirect(groupId: Int): List<MemberEntity>

    // --- Group Expenses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupExpense(expense: GroupExpenseEntity): Long

    @Update
    suspend fun updateGroupExpense(expense: GroupExpenseEntity)

    @Delete
    suspend fun deleteGroupExpense(expense: GroupExpenseEntity)

    @Query("SELECT * FROM group_expenses WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    fun getExpensesByGroupId(groupId: Int): Flow<List<GroupExpenseEntity>>

    @Query("SELECT * FROM group_expenses WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    suspend fun getExpensesByGroupIdDirect(groupId: Int): List<GroupExpenseEntity>

    @Query("SELECT * FROM group_expenses WHERE id = :expenseId")
    suspend fun getExpenseByIdDirect(expenseId: Int): GroupExpenseEntity?

    // --- Expense Participants ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseParticipants(participants: List<ExpenseParticipantEntity>)

    @Query("DELETE FROM expense_participants WHERE expenseId = :expenseId")
    suspend fun deleteParticipantsForExpense(expenseId: Int)

    @Query("SELECT * FROM expense_participants WHERE expenseId = :expenseId")
    fun getParticipantsForExpense(expenseId: Int): Flow<List<ExpenseParticipantEntity>>

    @Query("SELECT * FROM expense_participants WHERE expenseId = :expenseId")
    suspend fun getParticipantsForExpenseDirect(expenseId: Int): List<ExpenseParticipantEntity>

    @Query("SELECT ep.* FROM expense_participants ep INNER JOIN group_expenses ge ON ep.expenseId = ge.id WHERE ge.groupId = :groupId")
    suspend fun getParticipantsForGroupDirect(groupId: Int): List<ExpenseParticipantEntity>

    @Query("SELECT ep.* FROM expense_participants ep INNER JOIN group_expenses ge ON ep.expenseId = ge.id WHERE ge.groupId = :groupId")
    fun getParticipantsForGroup(groupId: Int): Flow<List<ExpenseParticipantEntity>>

    // --- Expense Payers ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpensePayers(payers: List<ExpensePayerEntity>)

    @Query("DELETE FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun deletePayersForExpense(expenseId: Int)

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    fun getPayersForExpense(expenseId: Int): Flow<List<ExpensePayerEntity>>

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun getPayersForExpenseDirect(expenseId: Int): List<ExpensePayerEntity>

    @Query("SELECT epa.* FROM expense_payers epa INNER JOIN group_expenses ge ON epa.expenseId = ge.id WHERE ge.groupId = :groupId")
    suspend fun getPayersForGroupDirect(groupId: Int): List<ExpensePayerEntity>

    @Query("SELECT epa.* FROM expense_payers epa INNER JOIN group_expenses ge ON epa.expenseId = ge.id WHERE ge.groupId = :groupId")
    fun getPayersForGroup(groupId: Int): Flow<List<ExpensePayerEntity>>

    // --- Settlements ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Delete
    suspend fun deleteSettlement(settlement: SettlementEntity)

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    fun getSettlementsByGroupId(groupId: Int): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    suspend fun getSettlementsByGroupIdDirect(groupId: Int): List<SettlementEntity>

    // --- Group Categories ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupCategory(category: GroupCategoryEntity): Long

    @Query("SELECT * FROM group_categories WHERE groupId IS NULL OR groupId = :groupId ORDER BY name ASC")
    fun getGroupCategories(groupId: Int): Flow<List<GroupCategoryEntity>>

    @Query("SELECT * FROM group_categories WHERE groupId IS NULL OR groupId = :groupId ORDER BY name ASC")
    suspend fun getGroupCategoriesDirect(groupId: Int): List<GroupCategoryEntity>

    // --- High-level Transaction Helpers ---
    @Transaction
    suspend fun insertExpenseWithDetails(
        expense: GroupExpenseEntity,
        payers: List<ExpensePayerEntity>,
        participants: List<ExpenseParticipantEntity>
    ): Long {
        val expenseId = insertGroupExpense(expense).toInt()
        val updatedPayers = payers.map { it.copy(expenseId = expenseId) }
        val updatedParticipants = participants.map { it.copy(expenseId = expenseId) }
        insertExpensePayers(updatedPayers)
        insertExpenseParticipants(updatedParticipants)
        return expenseId.toLong()
    }

    @Transaction
    suspend fun updateExpenseWithDetails(
        expense: GroupExpenseEntity,
        payers: List<ExpensePayerEntity>,
        participants: List<ExpenseParticipantEntity>
    ) {
        updateGroupExpense(expense)
        deletePayersForExpense(expense.id)
        deleteParticipantsForExpense(expense.id)
        val updatedPayers = payers.map { it.copy(expenseId = expense.id) }
        val updatedParticipants = participants.map { it.copy(expenseId = expense.id) }
        insertExpensePayers(updatedPayers)
        insertExpenseParticipants(updatedParticipants)
    }

    @Transaction
    suspend fun deleteExpenseWithDetails(expense: GroupExpenseEntity) {
        deleteParticipantsForExpense(expense.id)
        deletePayersForExpense(expense.id)
        deleteGroupExpense(expense)
    }

    @Transaction
    suspend fun createGroupWithMembers(group: GroupEntity, members: List<String>): Long {
        val groupId = insertGroup(group).toInt()
        val memberEntities = members.map { name ->
            MemberEntity(
                groupId = groupId,
                name = name,
                avatarUri = null,
                defaultWeight = 1.0,
                notes = null,
                isEnabled = true,
                createdDate = System.currentTimeMillis()
            )
        }
        insertMembers(memberEntities)
        return groupId.toLong()
    }
}
