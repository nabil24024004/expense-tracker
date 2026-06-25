package com.example.group.data.repository

import com.example.group.data.dao.GroupDao
import com.example.group.data.entity.*
import kotlinx.coroutines.flow.Flow

class GroupRepository(private val groupDao: GroupDao) {

    // --- Groups ---
    fun getAllGroups(): Flow<List<GroupEntity>> = groupDao.getAllGroups()
    
    fun getGroupById(id: Int): Flow<GroupEntity?> = groupDao.getGroupById(id)

    suspend fun getGroupByIdDirect(id: Int): GroupEntity? = groupDao.getGroupByIdDirect(id)

    suspend fun insertGroup(group: GroupEntity): Long = groupDao.insertGroup(group)

    suspend fun updateGroup(group: GroupEntity) = groupDao.updateGroup(group)

    suspend fun deleteGroup(group: GroupEntity) = groupDao.deleteGroup(group)

    suspend fun createGroupWithMembers(group: GroupEntity, members: List<String>): Long {
        return groupDao.createGroupWithMembers(group, members)
    }

    // --- Members ---
    fun getMembersByGroupId(groupId: Int): Flow<List<MemberEntity>> = groupDao.getMembersByGroupId(groupId)

    suspend fun getMembersByGroupIdDirect(groupId: Int): List<MemberEntity> = groupDao.getMembersByGroupIdDirect(groupId)

    suspend fun insertMember(member: MemberEntity): Long = groupDao.insertMember(member)

    suspend fun updateMember(member: MemberEntity) = groupDao.updateMember(member)

    // --- Expenses ---
    fun getExpensesByGroupId(groupId: Int): Flow<List<GroupExpenseEntity>> = groupDao.getExpensesByGroupId(groupId)

    suspend fun getExpensesByGroupIdDirect(groupId: Int): List<GroupExpenseEntity> = groupDao.getExpensesByGroupIdDirect(groupId)

    suspend fun getExpenseByIdDirect(expenseId: Int): GroupExpenseEntity? = groupDao.getExpenseByIdDirect(expenseId)

    suspend fun insertExpenseWithDetails(
        expense: GroupExpenseEntity,
        payers: List<ExpensePayerEntity>,
        participants: List<ExpenseParticipantEntity>
    ): Long {
        return groupDao.insertExpenseWithDetails(expense, payers, participants)
    }

    suspend fun updateExpenseWithDetails(
        expense: GroupExpenseEntity,
        payers: List<ExpensePayerEntity>,
        participants: List<ExpenseParticipantEntity>
    ) {
        groupDao.updateExpenseWithDetails(expense, payers, participants)
    }

    suspend fun deleteExpenseWithDetails(expense: GroupExpenseEntity) {
        groupDao.deleteExpenseWithDetails(expense)
    }

    // --- Participants ---
    fun getParticipantsForExpense(expenseId: Int): Flow<List<ExpenseParticipantEntity>> =
        groupDao.getParticipantsForExpense(expenseId)

    suspend fun getParticipantsForExpenseDirect(expenseId: Int): List<ExpenseParticipantEntity> =
        groupDao.getParticipantsForExpenseDirect(expenseId)

    suspend fun getParticipantsForGroupDirect(groupId: Int): List<ExpenseParticipantEntity> =
        groupDao.getParticipantsForGroupDirect(groupId)

    fun getParticipantsForGroup(groupId: Int): Flow<List<ExpenseParticipantEntity>> =
        groupDao.getParticipantsForGroup(groupId)

    // --- Payers ---
    fun getPayersForExpense(expenseId: Int): Flow<List<ExpensePayerEntity>> =
        groupDao.getPayersForExpense(expenseId)

    suspend fun getPayersForExpenseDirect(expenseId: Int): List<ExpensePayerEntity> =
        groupDao.getPayersForExpenseDirect(expenseId)

    suspend fun getPayersForGroupDirect(groupId: Int): List<ExpensePayerEntity> =
        groupDao.getPayersForGroupDirect(groupId)

    fun getPayersForGroup(groupId: Int): Flow<List<ExpensePayerEntity>> =
        groupDao.getPayersForGroup(groupId)

    // --- Settlements ---
    fun getSettlementsByGroupId(groupId: Int): Flow<List<SettlementEntity>> = groupDao.getSettlementsByGroupId(groupId)

    suspend fun getSettlementsByGroupIdDirect(groupId: Int): List<SettlementEntity> = groupDao.getSettlementsByGroupIdDirect(groupId)

    suspend fun insertSettlement(settlement: SettlementEntity): Long = groupDao.insertSettlement(settlement)

    suspend fun deleteSettlement(settlement: SettlementEntity) = groupDao.deleteSettlement(settlement)

    // --- Custom Categories ---
    fun getGroupCategories(groupId: Int): Flow<List<GroupCategoryEntity>> = groupDao.getGroupCategories(groupId)

    suspend fun getGroupCategoriesDirect(groupId: Int): List<GroupCategoryEntity> = groupDao.getGroupCategoriesDirect(groupId)

    suspend fun insertGroupCategory(category: GroupCategoryEntity): Long = groupDao.insertGroupCategory(category)
}
