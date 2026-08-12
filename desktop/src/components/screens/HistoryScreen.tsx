import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { History, TrendingUp, TrendingDown, Trash2, Filter, Search, Download } from 'lucide-react';

export const HistoryScreen: React.FC = () => {
  const {
    expenses,
    accounts,
    deleteExpense,
    searchQuery,
    setSearchQuery,
    activeCategoryFilter,
    setActiveCategoryFilter,
    activeTypeFilter,
    setActiveTypeFilter,
    settings,
    setIsExportImportOpen,
    requestDeleteConfirmation,
    customCategories
  } = useApp();

  const [dateRange, setDateRange] = useState<'ALL' | 'THIS_MONTH' | 'LAST_MONTH'>('ALL');
  const [selectedWalletFilter, setSelectedWalletFilter] = useState<string>('ALL');

  const currency = settings.currency || '৳';
  const formatAmount = (num: number) => {
    if (settings.hideBalance) return '••••';
    return `${currency}${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const filteredExpenses = expenses.filter(exp => {
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = exp.title.toLowerCase().includes(q);
      const matchCategory = exp.category.toLowerCase().includes(q);
      const matchNote = exp.note?.toLowerCase().includes(q) || false;
      if (!matchTitle && !matchCategory && !matchNote) return false;
    }

    if (activeCategoryFilter !== 'ALL' && exp.category !== activeCategoryFilter) {
      return false;
    }

    if (activeTypeFilter !== 'ALL' && exp.type !== activeTypeFilter) {
      return false;
    }

    if (selectedWalletFilter !== 'ALL' && exp.walletId !== selectedWalletFilter) {
      return false;
    }

    if (dateRange === 'THIS_MONTH') {
      const eDate = new Date(exp.date);
      const now = new Date();
      if (eDate.getMonth() !== now.getMonth() || eDate.getFullYear() !== now.getFullYear()) return false;
    } else if (dateRange === 'LAST_MONTH') {
      const eDate = new Date(exp.date);
      const now = new Date();
      const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      if (eDate.getMonth() !== lastMonth.getMonth() || eDate.getFullYear() !== lastMonth.getFullYear()) return false;
    }

    return true;
  });

  const defaultCategories = ['ALL', 'Food', 'Shopping', 'Travel', 'Bills', 'Health', 'Utilities', 'Salary', 'Investment'];
  const categories = Array.from(new Set([...defaultCategories, ...customCategories.map(c => c.name)]));

  return (
    <div className="space-y-6 pb-12">
      {/* Header Banner */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-[#EA3B35]/10 border border-[#EA3B35]/20 text-[#EA3B35]">
            <History className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-primary-var">Transaction History Logs</h2>
            <p className="text-xs text-secondary-var">Complete audit log of income, expenses, and transfers</p>
          </div>
        </div>

        <button
          onClick={() => setIsExportImportOpen(true)}
          className="px-4 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-lg shadow-[#EA3B35]/25 flex items-center space-x-2 transition-all cursor-pointer"
        >
          <Download className="w-4 h-4" />
          <span>Export CSV / PDF Report</span>
        </button>
      </div>

      {/* Filter Controls Bar */}
      <div className="p-5 rounded-2xl bg-card-surface border border-theme space-y-4">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          {/* Search Input */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-secondary-var" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Filter by keyword, note, or tag..."
              className="w-full bg-sub-surface border border-theme rounded-xl pl-10 pr-4 py-2 text-xs text-primary-var placeholder:text-secondary-var focus:outline-none focus:border-[#EA3B35]"
            />
          </div>

          {/* Type Toggle Pills */}
          <div className="flex items-center space-x-1.5 p-1 rounded-xl bg-sub-surface border border-theme">
            {(['ALL', 'EXPENSE', 'INCOME'] as const).map(t => (
              <button
                key={t}
                onClick={() => setActiveTypeFilter(t)}
                className={`px-3 py-1 rounded-xl border text-xs font-bold transition-colors cursor-pointer ${
                  activeTypeFilter === t
                    ? 'bg-[#EA3B35] text-white border-[#EA3B35]'
                    : 'text-secondary-var border-transparent hover:text-primary-var'
                }`}
              >
                {t}
              </button>
            ))}
          </div>

          {/* Wallet Filter */}
          <select
            value={selectedWalletFilter}
            onChange={(e) => setSelectedWalletFilter(e.target.value)}
            className="bg-sub-surface border border-theme rounded-xl px-3 py-2 text-xs text-primary-var focus:outline-none"
          >
            <option value="ALL">All Wallets</option>
            {accounts.map(acc => (
              <option key={acc.id} value={acc.id}>{acc.name}</option>
            ))}
          </select>

          {/* Date Filter */}
          <select
            value={dateRange}
            onChange={(e: any) => setDateRange(e.target.value)}
            className="bg-sub-surface border border-theme rounded-xl px-3 py-2 text-xs text-primary-var focus:outline-none"
          >
            <option value="ALL">All Time</option>
            <option value="THIS_MONTH">This Month</option>
            <option value="LAST_MONTH">Last Month</option>
          </select>
        </div>

        {/* Category Chips */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-none">
          <Filter className="w-3.5 h-3.5 text-secondary-var flex-shrink-0" />
          {categories.map(cat => (
            <button
              key={cat}
              onClick={() => setActiveCategoryFilter(cat)}
              className={`px-3 py-1 rounded-xl border font-semibold text-xs transition-colors cursor-pointer flex-shrink-0 ${
                activeCategoryFilter === cat
                  ? 'bg-[#EA3B35] border-[#EA3B35] text-white'
                  : 'bg-sub-surface border-theme text-secondary-var hover:text-primary-var'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* History Log Table */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <div className="flex items-center justify-between text-xs font-bold text-secondary-var">
          <span>Showing {filteredExpenses.length} transaction(s)</span>
          <span>
            Total Net:{' '}
            <span className="text-primary-var">
              {formatAmount(
                filteredExpenses.reduce((s, e) => s + (e.type === 'INCOME' ? e.amount : -e.amount), 0)
              )}
            </span>
          </span>
        </div>

        {filteredExpenses.length === 0 ? (
          <div className="py-16 text-center space-y-3 border-2 border-dashed border-theme rounded-2xl">
            <div className="w-12 h-12 rounded-full bg-sub-surface text-secondary-var mx-auto flex items-center justify-center">
              <History className="w-6 h-6" />
            </div>
            <div className="text-sm font-bold text-primary-var">No transactions match your search</div>
            <p className="text-xs text-secondary-var">Try resetting your search query or category filters.</p>
          </div>
        ) : (
          <div className="divide-y divide-theme">
            {filteredExpenses.map(exp => {
              const acc = accounts.find(a => a.id === exp.walletId);
              const isIncome = exp.type === 'INCOME';

              return (
                <div key={exp.id} className="py-3.5 flex items-center justify-between hover:bg-sub-surface/50 px-2 rounded-xl transition-colors group">
                  <div className="flex items-center space-x-3.5">
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                        isIncome ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20' : 'bg-[#EA3B35]/10 text-[#EA3B35] border border-[#EA3B35]/20'
                      }`}
                    >
                      {isIncome ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-primary-var">{exp.title}</div>
                      <div className="flex items-center space-x-2 text-xs text-secondary-var mt-0.5">
                        <span className="font-semibold text-primary-var/80">{exp.category}</span>
                        <span>•</span>
                        <span className="px-1.5 py-0.5 rounded bg-sub-surface border border-theme">
                          {acc?.name || 'Cash Wallet'}
                        </span>
                        <span>•</span>
                        <span>{new Date(exp.date).toLocaleString()}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center space-x-4">
                    <div className={`text-sm font-bold ${isIncome ? 'text-emerald-500' : 'text-[#EA3B35]'}`}>
                      {isIncome ? '+' : '-'} {formatAmount(exp.amount)}
                    </div>

                    <button
                      onClick={() => {
                        requestDeleteConfirmation({
                          title: 'Delete Transaction Log',
                          message: `Are you sure you want to delete "${exp.title}" (${formatAmount(exp.amount)})? This will adjust your wallet balance.`,
                          confirmText: 'Delete Log',
                          onConfirm: () => deleteExpense(exp.id)
                        });
                      }}
                      className="p-1.5 rounded-lg text-secondary-var hover:text-rose-500 hover:bg-rose-500/10 transition-colors opacity-0 group-hover:opacity-100 cursor-pointer"
                      title="Delete Transaction"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
