import React from 'react';
import { useApp } from '../../context/AppContext';
import { CalendarClock, Plus, CheckCircle2, TrendingUp, TrendingDown, Clock, AlertTriangle, Trash2 } from 'lucide-react';

export const PlannedScreen: React.FC = () => {
  const {
    plannedTransactions,
    payPlannedTransaction,
    deletePlannedTransaction,
    settings,
    setIsAddPlannedOpen,
    requestDeleteConfirmation
  } = useApp();

  const currency = settings.currency || '৳';
  const formatAmount = (num: number) => {
    if (settings.hideBalance) return '••••';
    return `${currency}${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header Banner */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-[#EA3B35]/10 border border-[#EA3B35]/20 text-[#EA3B35]">
            <CalendarClock className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-primary-var">Planned Payments & Subscriptions</h2>
            <p className="text-xs text-secondary-var">Recurring subscriptions, monthly bills, and scheduled paychecks</p>
          </div>
        </div>

        <button
          onClick={() => setIsAddPlannedOpen(true)}
          className="px-4 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-lg shadow-[#EA3B35]/25 flex items-center space-x-2 transition-all active:scale-95 cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Schedule Payment</span>
        </button>
      </div>

      {/* Planned Payments List */}
      {plannedTransactions.length === 0 ? (
        <div className="py-16 text-center space-y-4 border-2 border-dashed border-theme rounded-2xl bg-card-surface/50">
          <div className="w-14 h-14 rounded-2xl bg-[#EA3B35]/10 border border-[#EA3B35]/20 text-[#EA3B35] mx-auto flex items-center justify-center">
            <Clock className="w-7 h-7" />
          </div>
          <div>
            <div className="text-base font-bold text-primary-var">No Scheduled Payments</div>
            <p className="text-xs text-secondary-var mt-1 max-w-sm mx-auto">
              Keep track of Netflix, Spotify, Internet bills, Rent, or salary paychecks before they hit your account.
            </p>
          </div>
          <button
            onClick={() => setIsAddPlannedOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-lg shadow-[#EA3B35]/25 transition-all inline-flex items-center space-x-2 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Schedule First Payment</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {plannedTransactions.map(plan => {
            const isIncome = plan.type === 'INCOME';
            const dueDate = new Date(plan.nextDueDate);
            const isOverdue = dueDate < new Date();

            return (
              <div
                key={plan.id}
                className={`p-5 rounded-2xl bg-card-surface border transition-all space-y-4 relative ${
                  isOverdue ? 'border-rose-500/40 bg-rose-500/5' : 'border-theme'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                        isIncome ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20' : 'bg-[#EA3B35]/10 text-[#EA3B35] border border-[#EA3B35]/20'
                      }`}
                    >
                      {isIncome ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-primary-var">{plan.title}</h4>
                      <p className="text-xs text-secondary-var capitalize">{plan.category} • {plan.frequency}</p>
                    </div>
                  </div>

                  <button
                    onClick={() => {
                      requestDeleteConfirmation({
                        title: 'Delete Scheduled Payment',
                        message: `Are you sure you want to cancel the recurring schedule for "${plan.title}"?`,
                        confirmText: 'Remove Schedule',
                        onConfirm: () => deletePlannedTransaction(plan.id)
                      });
                    }}
                    className="p-1 rounded-lg text-secondary-var hover:text-rose-500 hover:bg-rose-500/10 transition-colors cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-[11px] text-secondary-var uppercase tracking-wide">Amount</span>
                    <div className={`text-lg font-extrabold ${isIncome ? 'text-emerald-500' : 'text-primary-var'}`}>
                      {formatAmount(plan.amount)}
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="text-[11px] text-secondary-var uppercase tracking-wide">Due Date</span>
                    <div className={`text-xs font-bold ${isOverdue ? 'text-rose-500' : 'text-primary-var'}`}>
                      {dueDate.toLocaleDateString()}
                    </div>
                  </div>
                </div>

                {isOverdue && (
                  <div className="p-2 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-500 text-xs flex items-center space-x-1.5 font-medium">
                    <AlertTriangle className="w-3.5 h-3.5" />
                    <span>Overdue payment</span>
                  </div>
                )}

                <button
                  onClick={() => payPlannedTransaction(plan)}
                  className="w-full py-2.5 rounded-xl bg-sub-surface hover:bg-[#EA3B35] hover:text-white border border-theme text-primary-var text-xs font-bold flex items-center justify-center space-x-2 transition-all cursor-pointer"
                >
                  <CheckCircle2 className="w-4 h-4 text-[#EA3B35] group-hover:text-white" />
                  <span>Mark as Logged & Paid</span>
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
