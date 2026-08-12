import React from 'react';
import { useApp } from '../../context/AppContext';
import { Wallet, Plus, ArrowRightLeft, CreditCard, Landmark, PiggyBank, Banknote, Trash2, EyeOff, CheckCircle2 } from 'lucide-react';

export const AccountsScreen: React.FC = () => {
  const {
    accounts,
    deleteAccount,
    settings,
    setIsAddAccountOpen,
    setIsTransferOpen,
    requestDeleteConfirmation,
    setSelectedAccountForDetails
  } = useApp();

  const currency = settings.currency || '৳';
  const formatAmount = (num: number) => {
    if (settings.hideBalance) return '••••';
    return `${currency}${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const getAccountIcon = (type: string) => {
    switch (type) {
      case 'cash': return Banknote;
      case 'bank': return Landmark;
      case 'credit': return CreditCard;
      case 'savings': return PiggyBank;
      default: return Wallet;
    }
  };

  const includedAccounts = accounts.filter(a => !a.isExcluded);
  const totalNetBalance = includedAccounts.reduce((sum, a) => sum + a.currentBalance, 0);

  return (
    <div className="space-y-6 pb-12">
      {/* Header Banner & Net Worth Overview */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-bold text-secondary-var uppercase tracking-wider">Primary Net Cash Flow Balance</span>
          <div className="text-3xl font-extrabold text-primary-var tracking-tight mt-1">{formatAmount(totalNetBalance)}</div>
          <p className="text-xs text-secondary-var mt-1">
            Calculated from {includedAccounts.length} active wallet(s)
          </p>
        </div>

        <div className="flex items-center space-x-3 w-full md:w-auto">
          {accounts.length > 0 && (
            <button
              onClick={() => setIsTransferOpen(true)}
              className="flex-1 md:flex-none px-4 py-2.5 rounded-xl bg-sub-surface hover:bg-theme-main text-primary-var border border-theme text-xs font-bold flex items-center justify-center space-x-2 transition-colors cursor-pointer"
            >
              <ArrowRightLeft className="w-4 h-4 text-[#EA3B35]" />
              <span>Transfer Funds</span>
            </button>
          )}

          <button
            onClick={() => setIsAddAccountOpen(true)}
            className="flex-1 md:flex-none px-4 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-lg shadow-[#EA3B35]/25 flex items-center justify-center space-x-2 transition-all active:scale-95 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Add New Wallet</span>
          </button>
        </div>
      </div>

      {/* Wallets & Cards Grid */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-primary-var">Your Wallets & Cards ({accounts.length})</h3>
          <span className="text-xs text-secondary-var">Tap card to view details</span>
        </div>

        {accounts.length === 0 ? (
          <div className="py-16 text-center space-y-4 border-2 border-dashed border-theme rounded-2xl bg-card-surface/50">
            <div className="w-14 h-14 rounded-2xl bg-[#EA3B35]/10 border border-[#EA3B35]/20 text-[#EA3B35] mx-auto flex items-center justify-center">
              <Wallet className="w-7 h-7" />
            </div>
            <div>
              <div className="text-base font-bold text-primary-var">No Wallets Created Yet</div>
              <p className="text-xs text-secondary-var mt-1 max-w-sm mx-auto">
                Create custom wallets for your Physical Cash, Payroll Bank Account, Credit Cards, or Savings.
              </p>
            </div>
            <button
              onClick={() => setIsAddAccountOpen(true)}
              className="px-5 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-lg shadow-[#EA3B35]/25 transition-all inline-flex items-center space-x-2 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>Create First Wallet</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {accounts.map(acc => {
              const Icon = getAccountIcon(acc.cardType);

              return (
                <div
                  key={acc.id}
                  onClick={() => setSelectedAccountForDetails(acc)}
                  className="p-5 rounded-2xl bg-card-surface border border-theme hover:border-[#EA3B35]/40 transition-all space-y-4 relative group cursor-pointer"
                  style={{ borderTop: `4px solid ${acc.colorHex || '#EA3B35'}` }}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center space-x-3">
                      <div
                        className="w-10 h-10 rounded-xl flex items-center justify-center"
                        style={{ backgroundColor: `${acc.colorHex || '#EA3B35'}25`, color: acc.colorHex || '#EA3B35' }}
                      >
                        <Icon className="w-5 h-5" />
                      </div>
                      <div>
                        <h4 className="text-sm font-semibold text-primary-var">{acc.name}</h4>
                        <p className="text-xs text-secondary-var">{acc.bankName}</p>
                      </div>
                    </div>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        requestDeleteConfirmation({
                          title: 'Delete Wallet Card',
                          message: `Are you sure you want to delete "${acc.name}"? This action cannot be undone.`,
                          confirmText: 'Delete Wallet',
                          onConfirm: () => deleteAccount(acc.id)
                        });
                      }}
                      className="p-1.5 rounded-lg text-secondary-var hover:text-rose-500 hover:bg-rose-500/10 transition-colors opacity-0 group-hover:opacity-100 cursor-pointer"
                      title="Delete Wallet"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Balance Display */}
                  <div>
                    <span className="text-[11px] text-secondary-var uppercase tracking-wide">Current Balance</span>
                    <div className={`text-xl font-extrabold ${acc.currentBalance < 0 ? 'text-rose-500' : 'text-primary-var'}`}>
                      {formatAmount(acc.currentBalance)}
                    </div>
                  </div>

                  {/* Excluded Badge & Card Type */}
                  <div className="pt-2 border-t border-theme flex items-center justify-between text-xs">
                    <span className="capitalize px-2 py-0.5 rounded bg-sub-surface border border-theme text-secondary-var">
                      {acc.cardType}
                    </span>

                    {acc.isExcluded ? (
                      <span className="flex items-center space-x-1 text-amber-500 text-[11px] bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">
                        <EyeOff className="w-3 h-3" />
                        <span>Excluded from Net</span>
                      </span>
                    ) : (
                      <span className="flex items-center space-x-1 text-emerald-500 text-[11px] bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                        <CheckCircle2 className="w-3 h-3" />
                        <span>Included in Net</span>
                      </span>
                    )}
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
