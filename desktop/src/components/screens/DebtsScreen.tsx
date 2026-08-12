import React from 'react';
import { useApp } from '../../context/AppContext';
import { Scale, Plus, ArrowUpRight, ArrowDownLeft, ShieldCheck, Trash2 } from 'lucide-react';

export const DebtsScreen: React.FC = () => {
  const {
    debtsDues,
    deleteDebtDue,
    openSettleDebtModal,
    settings,
    setIsAddDebtOpen,
    requestDeleteConfirmation
  } = useApp();

  const currency = settings.currency || '৳';
  const formatAmount = (num: number) => {
    if (settings.hideBalance) return '••••';
    return `${currency}${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const totalLent = debtsDues
    .filter(d => d.type === 'LENT' && !d.isSettled)
    .reduce((sum, d) => sum + (d.amount - d.paidAmount), 0);

  const totalBorrowed = debtsDues
    .filter(d => d.type === 'BORROWED' && !d.isSettled)
    .reduce((sum, d) => sum + (d.amount - d.paidAmount), 0);

  return (
    <div className="space-y-6 pb-12">
      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="p-5 rounded-2xl bg-card-surface border border-theme flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
            <ArrowUpRight className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-secondary-var uppercase tracking-wide">You Are Owed (Lent)</span>
            <div className="text-2xl font-extrabold text-emerald-500">{formatAmount(totalLent)}</div>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-card-surface border border-theme flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-rose-500/10 text-rose-500 border border-rose-500/20">
            <ArrowDownLeft className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-secondary-var uppercase tracking-wide">You Owe (Borrowed)</span>
            <div className="text-2xl font-extrabold text-rose-500">{formatAmount(totalBorrowed)}</div>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-card-surface border border-theme flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-secondary-var uppercase tracking-wide">Debt Ledger</span>
            <div className="text-xs text-secondary-var mt-0.5">{debtsDues.filter(d => !d.isSettled).length} pending item(s)</div>
          </div>

          <button
            onClick={() => setIsAddDebtOpen(true)}
            className="px-4 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-lg shadow-[#EA3B35]/25 flex items-center space-x-1.5 transition-all active:scale-95 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Add Record</span>
          </button>
        </div>
      </div>

      {/* Debts & Receivables List */}
      {debtsDues.length === 0 ? (
        <div className="py-16 text-center space-y-4 border-2 border-dashed border-theme rounded-2xl bg-card-surface/50">
          <div className="w-14 h-14 rounded-2xl bg-[#EA3B35]/10 border border-[#EA3B35]/20 text-[#EA3B35] mx-auto flex items-center justify-center">
            <Scale className="w-7 h-7" />
          </div>
          <div>
            <div className="text-base font-bold text-primary-var">No Debts or Receivables Logged</div>
            <p className="text-xs text-secondary-var mt-1 max-w-sm mx-auto">
              Track money lent to friends or loans borrowed from family with partial settlement support.
            </p>
          </div>
          <button
            onClick={() => setIsAddDebtOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-lg shadow-[#EA3B35]/25 transition-all inline-flex items-center space-x-2 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Add Debt Record</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {debtsDues.map(debt => {
            const isLent = debt.type === 'LENT';
            const remaining = debt.amount - debt.paidAmount;
            const progress = Math.min(100, Math.round((debt.paidAmount / debt.amount) * 100));

            return (
              <div
                key={debt.id}
                className={`p-5 rounded-2xl bg-card-surface border transition-all space-y-4 ${
                  debt.isSettled ? 'border-emerald-500/30 opacity-70' : 'border-theme'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                        isLent ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-500 border border-rose-500/20'
                      }`}
                    >
                      {isLent ? <ArrowUpRight className="w-5 h-5" /> : <ArrowDownLeft className="w-5 h-5" />}
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-primary-var">{debt.personName}</h4>
                      <p className="text-xs text-secondary-var">{isLent ? 'Lent to person' : 'Borrowed from person'}</p>
                    </div>
                  </div>

                  <button
                    onClick={() => {
                      requestDeleteConfirmation({
                        title: 'Delete Debt Record',
                        message: `Are you sure you want to delete the debt record for "${debt.personName}"?`,
                        confirmText: 'Delete Record',
                        onConfirm: () => deleteDebtDue(debt.id)
                      });
                    }}
                    className="p-1 rounded-lg text-secondary-var hover:text-rose-500 hover:bg-rose-500/10 transition-colors cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-[11px] text-secondary-var uppercase tracking-wide">Remaining Due</span>
                    <div className={`text-lg font-extrabold ${isLent ? 'text-emerald-500' : 'text-rose-500'}`}>
                      {formatAmount(remaining)}
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="text-[11px] text-secondary-var uppercase tracking-wide">Original</span>
                    <div className="text-xs font-bold text-primary-var">{formatAmount(debt.amount)}</div>
                  </div>
                </div>

                {/* Progress bar for partial payments */}
                <div className="space-y-1">
                  <div className="w-full h-2 bg-sub-surface rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${isLent ? 'bg-emerald-500' : 'bg-[#EA3B35]'}`}
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-[10px] text-secondary-var font-medium">
                    <span>Paid: {formatAmount(debt.paidAmount)}</span>
                    <span>{progress}% Settled</span>
                  </div>
                </div>

                {!debt.isSettled ? (
                  <button
                    onClick={() => openSettleDebtModal(debt)}
                    className="w-full py-2.5 rounded-xl bg-sub-surface hover:bg-emerald-500 hover:text-white border border-theme text-primary-var text-xs font-bold flex items-center justify-center space-x-2 transition-all cursor-pointer"
                  >
                    <ShieldCheck className="w-4 h-4 text-emerald-500" />
                    <span>Settle / Make Payment</span>
                  </button>
                ) : (
                  <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 text-xs font-bold text-center">
                    ✓ Fully Settled
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
