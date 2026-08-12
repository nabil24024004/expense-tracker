import React from 'react';
import { useApp } from '../../context/AppContext';
import { X, Wallet, TrendingUp, TrendingDown, History, Plus, ArrowRightLeft } from 'lucide-react';

export const AccountDetailsModal: React.FC = () => {
  const {
    selectedAccountForDetails,
    setSelectedAccountForDetails,
    expenses,
    settings,
    setIsAddExpenseOpen,
    setIsTransferOpen
  } = useApp();

  if (!selectedAccountForDetails) return null;

  const account = selectedAccountForDetails;
  const currency = settings.currency || '৳';

  const formatAmount = (num: number) => {
    if (settings.hideBalance) return '••••';
    return `${currency}${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const walletExpenses = expenses.filter(e => e.walletId === account.id);
  const totalInflow = walletExpenses.filter(e => e.type === 'INCOME').reduce((sum, e) => sum + e.amount, 0);
  const totalOutflow = walletExpenses.filter(e => e.type === 'EXPENSE').reduce((sum, e) => sum + e.amount, 0);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 select-none">
      <div className="w-full max-w-xl rounded-2xl bg-card-surface border border-theme p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-theme">
          <div className="flex items-center space-x-3">
            <div
              className="w-10 h-10 rounded-xl flex items-center justify-center"
              style={{ backgroundColor: `${account.colorHex || '#EA3B35'}25`, color: account.colorHex || '#EA3B35' }}
            >
              <Wallet className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-primary-var tracking-tight">{account.name}</h3>
              <p className="text-xs text-secondary-var">{account.bankName} • <span className="capitalize">{account.cardType}</span></p>
            </div>
          </div>

          <button
            onClick={() => setSelectedAccountForDetails(null)}
            className="p-1 rounded-lg text-secondary-var hover:text-primary-var hover:bg-sub-surface transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Stats Summary Row */}
        <div className="grid grid-cols-3 gap-3">
          <div className="p-3.5 rounded-xl bg-sub-surface border border-theme">
            <span className="text-[10px] font-bold text-secondary-var uppercase tracking-wide">Current Balance</span>
            <div className="text-lg font-extrabold text-primary-var mt-0.5">{formatAmount(account.currentBalance)}</div>
          </div>

          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20">
            <span className="text-[10px] font-bold text-emerald-500 uppercase tracking-wide flex items-center space-x-1">
              <TrendingUp className="w-3 h-3" />
              <span>Total Inflow</span>
            </span>
            <div className="text-lg font-extrabold text-emerald-500 mt-0.5">{formatAmount(totalInflow)}</div>
          </div>

          <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20">
            <span className="text-[10px] font-bold text-rose-500 uppercase tracking-wide flex items-center space-x-1">
              <TrendingDown className="w-3 h-3" />
              <span>Total Outflow</span>
            </span>
            <div className="text-lg font-extrabold text-rose-500 mt-0.5">{formatAmount(totalOutflow)}</div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="flex items-center space-x-3">
          <button
            onClick={() => {
              setSelectedAccountForDetails(null);
              setIsAddExpenseOpen(true);
            }}
            className="flex-1 py-2.5 px-3 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold flex items-center justify-center space-x-1.5 transition-all shadow-md shadow-[#EA3B35]/20 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Log Transaction to Wallet</span>
          </button>

          <button
            onClick={() => {
              setSelectedAccountForDetails(null);
              setIsTransferOpen(true);
            }}
            className="flex-1 py-2.5 px-3 rounded-xl bg-sub-surface hover:bg-theme-main text-primary-var border border-theme text-xs font-bold flex items-center justify-center space-x-1.5 transition-colors cursor-pointer"
          >
            <ArrowRightLeft className="w-4 h-4 text-[#EA3B35]" />
            <span>Transfer Funds</span>
          </button>
        </div>

        {/* Wallet Transactions Breakdown */}
        <div className="space-y-2 pt-2">
          <div className="flex items-center justify-between text-xs font-bold">
            <span className="text-primary-var flex items-center space-x-1.5">
              <History className="w-4 h-4 text-[#EA3B35]" />
              <span>Wallet Transaction Log ({walletExpenses.length})</span>
            </span>
          </div>

          {walletExpenses.length === 0 ? (
            <div className="py-8 text-center border border-dashed border-theme rounded-xl text-xs text-secondary-var">
              No transactions recorded for this wallet yet.
            </div>
          ) : (
            <div className="max-h-48 overflow-y-auto divide-y divide-theme pr-1">
              {walletExpenses.map(exp => {
                const isIncome = exp.type === 'INCOME';
                return (
                  <div key={exp.id} className="py-2.5 flex items-center justify-between px-2 hover:bg-sub-surface rounded-lg transition-colors">
                    <div className="flex items-center space-x-3">
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                        isIncome ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
                      }`}>
                        {isIncome ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                      </div>
                      <div>
                        <div className="text-xs font-bold text-primary-var">{exp.title}</div>
                        <div className="text-[11px] text-secondary-var">{exp.category} • {new Date(exp.date).toLocaleDateString()}</div>
                      </div>
                    </div>

                    <div className={`text-xs font-bold ${isIncome ? 'text-emerald-500' : 'text-rose-500'}`}>
                      {isIncome ? '+' : '-'} {formatAmount(exp.amount)}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
