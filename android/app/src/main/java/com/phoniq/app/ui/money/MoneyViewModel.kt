package com.phoniq.app.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import com.phoniq.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoneyViewModel(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val monthRange = transactionRepository.currentMonthEpochRange()

    val currentMonthTransactions: StateFlow<List<TransactionEntity>> =
        transactionRepository.transactionsForPeriod(monthRange.first, monthRange.second)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalSpentThisMonth: StateFlow<Double?> =
        transactionRepository.totalSpentInPeriod(monthRange.first, monthRange.second)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentMonthBudgets: StateFlow<List<BudgetEntity>> =
        transactionRepository.budgetsForCurrentMonth()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addManualTransaction(txn: TransactionEntity) {
        viewModelScope.launch { transactionRepository.addTransaction(txn) }
    }

    fun setBudget(category: String, limitRupees: Double) {
        viewModelScope.launch { transactionRepository.setBudget(category, limitRupees) }
    }

    class Factory(private val repo: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MoneyViewModel(repo) as T
    }
}
