package com.example.group.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.group.calculator.BalanceCalculator
import com.example.group.calculator.SettlementGenerator
import com.example.group.data.entity.*
import com.example.group.data.repository.GroupRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GroupRepository

    init {
        val groupDao = AppDatabase.getDatabase(application).groupDao()
        repository = GroupRepository(groupDao)
    }

    // --- State Streams ---
    val allGroups: StateFlow<List<GroupEntity>> = repository.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedGroupId = MutableStateFlow<Int?>(null)
    val selectedGroupId: StateFlow<Int?> = _selectedGroupId.asStateFlow()

    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

    fun setShowCreateGroupDialog(show: Boolean) {
        _showCreateGroupDialog.value = show
    }

    private val _showAddExpenseDialog = MutableStateFlow(false)
    val showAddExpenseDialog: StateFlow<Boolean> = _showAddExpenseDialog.asStateFlow()

    fun setShowAddExpenseDialog(show: Boolean) {
        _showAddExpenseDialog.value = show
    }

    fun selectGroup(groupId: Int?) {
        _selectedGroupId.value = groupId
    }

    val selectedGroup: StateFlow<GroupEntity?> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getGroupById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedGroupMembers: StateFlow<List<MemberEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMembersByGroupId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroupExpenses: StateFlow<List<GroupExpenseEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getExpensesByGroupId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroupSettlements: StateFlow<List<SettlementEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSettlementsByGroupId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroupCategories: StateFlow<List<GroupCategoryEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getGroupCategories(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedGroupParticipants: StateFlow<List<ExpenseParticipantEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getParticipantsForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedGroupPayers: StateFlow<List<ExpensePayerEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getPayersForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Derived Calculators ---
    val selectedGroupBalances: StateFlow<Map<Int, Double>> = combine(
        selectedGroupMembers,
        selectedGroupPayers,
        selectedGroupParticipants,
        selectedGroupSettlements
    ) { members, payers, participants, settlements ->
        BalanceCalculator.calculateBalances(members, payers, participants, settlements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selectedGroupSuggestedSettlements: StateFlow<List<SettlementGenerator.SuggestedSettlement>> =
        selectedGroupBalances.map { balances ->
            SettlementGenerator.generateSuggestedSettlements(balances)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Action Methods ---
    fun createGroup(
        name: String,
        currency: String,
        color: Int,
        description: String?,
        members: List<String>
    ) {
        viewModelScope.launch {
            val group = GroupEntity(
                name = name,
                currency = currency,
                color = color,
                description = description,
                createdDate = System.currentTimeMillis()
            )
            repository.createGroupWithMembers(group, members)
        }
    }

    fun addMember(groupId: Int, name: String, notes: String?, defaultWeight: Double) {
        viewModelScope.launch {
            val member = MemberEntity(
                groupId = groupId,
                name = name,
                avatarUri = null,
                defaultWeight = defaultWeight,
                notes = notes,
                isEnabled = true,
                createdDate = System.currentTimeMillis()
            )
            repository.insertMember(member)
        }
    }

    fun updateMember(member: MemberEntity) {
        viewModelScope.launch {
            repository.updateMember(member)
        }
    }

    fun addExpense(
        groupId: Int,
        title: String,
        amount: Double,
        currency: String,
        category: String,
        date: Long,
        notes: String?,
        receiptBytes: ByteArray?,
        payers: List<ExpensePayerEntity>,
        participants: List<ExpenseParticipantEntity>
    ) {
        viewModelScope.launch {
            val expense = GroupExpenseEntity(
                groupId = groupId,
                title = title,
                amount = amount,
                currency = currency,
                category = category,
                date = date,
                notes = notes,
                receiptBytes = receiptBytes,
                createdDate = System.currentTimeMillis(),
                updatedDate = System.currentTimeMillis()
            )
            repository.insertExpenseWithDetails(expense, payers, participants)
        }
    }

    fun updateExpense(
        expense: GroupExpenseEntity,
        payers: List<ExpensePayerEntity>,
        participants: List<ExpenseParticipantEntity>
    ) {
        viewModelScope.launch {
            val updated = expense.copy(updatedDate = System.currentTimeMillis())
            repository.updateExpenseWithDetails(updated, payers, participants)
        }
    }

    fun deleteExpense(expense: GroupExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpenseWithDetails(expense)
        }
    }

    fun addSettlement(
        groupId: Int,
        payerId: Int,
        payeeId: Int,
        amount: Double,
        date: Long,
        notes: String?
    ) {
        viewModelScope.launch {
            val settlement = SettlementEntity(
                groupId = groupId,
                payerId = payerId,
                payeeId = payeeId,
                amount = amount,
                date = date,
                notes = notes
            )
            repository.insertSettlement(settlement)
        }
    }

    fun deleteSettlement(settlement: SettlementEntity) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
        }
    }

    fun toggleGroupArchive(group: GroupEntity) {
        viewModelScope.launch {
            repository.updateGroup(group.copy(isArchived = !group.isArchived))
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }

    fun addCustomCategory(groupId: Int, name: String) {
        viewModelScope.launch {
            val category = GroupCategoryEntity(
                groupId = groupId,
                name = name,
                iconName = "Others"
            )
            repository.insertGroupCategory(category)
        }
    }

    // Direct access helper for single expense details (used when editing)
    suspend fun getExpensePayersDirect(expenseId: Int): List<ExpensePayerEntity> {
        return repository.getPayersForExpenseDirect(expenseId)
    }

    suspend fun getExpenseParticipantsDirect(expenseId: Int): List<ExpenseParticipantEntity> {
        return repository.getParticipantsForExpenseDirect(expenseId)
    }
}
