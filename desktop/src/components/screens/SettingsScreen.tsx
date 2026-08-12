import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { Settings, Lock, Database, CheckCircle2, User, Trash2, Coins, Sparkles } from 'lucide-react';

export const SettingsScreen: React.FC = () => {
  const {
    settings,
    userName,
    setUserName,
    setCurrency,
    setBudgetPeriod,
    setPinLock,
    clearAllData,
    setIsExportImportOpen,
    requestDeleteConfirmation,
    resetOnboarding
  } = useApp();

  const [nameInput, setNameInput] = useState<string>(userName);
  const [limitInput, setLimitInput] = useState<string>(settings.budget.limitAmount.toString());
  const [pinInput, setPinInput] = useState<string>('');
  const [savedSuccess, setSavedSuccess] = useState<boolean>(false);

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    if (nameInput.trim()) {
      setUserName(nameInput.trim());
      setSavedSuccess(true);
      setTimeout(() => setSavedSuccess(false), 2000);
    }
  };

  const handleSaveBudget = (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseFloat(limitInput);
    if (!isNaN(val) && val > 0) {
      setBudgetPeriod(settings.budget.periodType, val);
      setSavedSuccess(true);
      setTimeout(() => setSavedSuccess(false), 2000);
    }
  };

  const handleSetPin = () => {
    if (pinInput.length >= 4) {
      setPinLock(pinInput);
      setPinInput('');
      setSavedSuccess(true);
      setTimeout(() => setSavedSuccess(false), 2000);
    }
  };

  const handleRemovePin = () => {
    setPinLock(null);
  };

  const currencyOptions = [
    { label: '৳ (BDT - Bangladeshi Taka)', symbol: '৳' },
    { label: '$ (USD - US Dollar)', symbol: '$' },
    { label: '€ (EUR - Euro)', symbol: '€' },
    { label: '£ (GBP - British Pound)', symbol: '£' },
    { label: '₹ (INR - Indian Rupee)', symbol: '₹' }
  ];

  return (
    <div className="space-y-6 pb-12 max-w-3xl">
      {/* Header Banner */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme flex items-center space-x-3">
        <Settings className="w-6 h-6 text-[#EA3B35]" />
        <div>
          <h2 className="text-xl font-bold text-primary-var">Application Settings</h2>
          <p className="text-xs text-secondary-var">Configure user profile, currency preference, budget targets, PIN security, and data backup</p>
        </div>
      </div>

      {savedSuccess && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4" />
          <span>Settings saved successfully!</span>
        </div>
      )}

      {/* Section 1: User Profile Settings */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <div className="flex items-center space-x-2">
          <User className="w-5 h-5 text-[#EA3B35]" />
          <h3 className="text-base font-bold text-primary-var">User Profile</h3>
        </div>

        <form onSubmit={handleSaveProfile} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-secondary-var">Display Name</label>
            <input
              type="text"
              value={nameInput}
              onChange={(e) => setNameInput(e.target.value)}
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var focus:outline-none focus:border-[#EA3B35]"
              placeholder="e.g. John Doe"
            />
          </div>

          <button
            type="submit"
            className="px-5 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold transition-all shadow-md shadow-[#EA3B35]/25 cursor-pointer"
          >
            Save Profile Name
          </button>
        </form>
      </div>

      {/* Section 2: Currency Preference */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <div className="flex items-center space-x-2">
          <Coins className="w-5 h-5 text-[#EA3B35]" />
          <h3 className="text-base font-bold text-primary-var">Currency Preference</h3>
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-medium text-secondary-var">Select Display Currency</label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {currencyOptions.map((opt) => {
              const isSelected = settings.currency === opt.symbol;
              return (
                <button
                  type="button"
                  key={opt.symbol}
                  onClick={() => {
                    setCurrency(opt.symbol);
                    setSavedSuccess(true);
                    setTimeout(() => setSavedSuccess(false), 2000);
                  }}
                  className={`p-3 rounded-xl border text-xs font-bold text-left transition-all cursor-pointer flex items-center justify-between ${
                    isSelected
                      ? 'bg-[#EA3B35] text-white border-[#EA3B35] shadow-md'
                      : 'bg-sub-surface border-theme text-primary-var hover:border-[#EA3B35]/50'
                  }`}
                >
                  <span>{opt.label}</span>
                  {isSelected && <CheckCircle2 className="w-4 h-4 text-white" />}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Section 3: Budget Period & Limit */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <h3 className="text-base font-bold text-primary-var">Budget Period & Limit Target</h3>

        <form onSubmit={handleSaveBudget} className="space-y-4">
          <div className="grid grid-cols-3 gap-3">
            {(['WEEKLY', 'MONTHLY', 'CUSTOM'] as const).map(p => (
              <button
                type="button"
                key={p}
                onClick={() => setBudgetPeriod(p, settings.budget.limitAmount)}
                className={`py-2.5 rounded-xl border text-xs font-bold transition-colors cursor-pointer ${
                  settings.budget.periodType === p
                    ? 'bg-[#EA3B35] text-white border-[#EA3B35]'
                    : 'bg-sub-surface text-secondary-var border-theme hover:text-primary-var'
                }`}
              >
                {p}
              </button>
            ))}
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-medium text-secondary-var">Budget Limit Target ({settings.currency})</label>
            <input
              type="number"
              value={limitInput}
              onChange={(e) => setLimitInput(e.target.value)}
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var focus:outline-none focus:border-[#EA3B35]"
              placeholder="e.g. 10000"
            />
          </div>

          <button
            type="submit"
            className="px-5 py-2.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold transition-all shadow-md shadow-[#EA3B35]/25 cursor-pointer"
          >
            Update Budget Target
          </button>
        </form>
      </div>

      {/* Section 4: Master PIN Security Lock */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <div className="flex items-center space-x-2">
          <Lock className="w-5 h-5 text-rose-500" />
          <h3 className="text-base font-bold text-primary-var">Master Security PIN Lock</h3>
        </div>

        {settings.pinLock ? (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-between">
            <div className="text-xs text-emerald-400 font-bold">
              ✓ App is protected with a Master PIN
            </div>
            <button
              onClick={handleRemovePin}
              className="px-3.5 py-1.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-500 border border-rose-500/30 text-xs font-bold transition-colors cursor-pointer"
            >
              Remove Security Lock
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-xs text-secondary-var">
              Set a 4+ digit PIN to lock the application when left unattended.
            </p>
            <div className="flex items-center space-x-3">
              <input
                type="password"
                value={pinInput}
                onChange={(e) => setPinInput(e.target.value)}
                placeholder="Enter 4-digit PIN"
                maxLength={8}
                className="bg-sub-surface border border-theme rounded-xl px-4 py-2 text-sm text-primary-var focus:outline-none focus:border-[#EA3B35]"
              />
              <button
                onClick={handleSetPin}
                className="px-4 py-2 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold transition-all shadow-md shadow-[#EA3B35]/25 cursor-pointer"
              >
                Enable Security Lock
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Section 5: Data Migration & Sync */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <div className="flex items-center space-x-2">
          <Database className="w-5 h-5 text-[#EA3B35]" />
          <h3 className="text-base font-bold text-primary-var">Data Sync & Backup Tools</h3>
        </div>

        <p className="text-xs text-secondary-var">
          Import Android JSON backup files to transfer your finance history to Desktop, or export CSV/PDF reports.
        </p>

        <div className="flex flex-col sm:flex-row items-center gap-3">
          <button
            onClick={() => setIsExportImportOpen(true)}
            className="flex-1 py-3 px-4 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold flex items-center justify-center space-x-2 shadow-lg shadow-[#EA3B35]/25 transition-all cursor-pointer"
          >
            <Database className="w-4 h-4" />
            <span>Open Data Sync & Backup Center</span>
          </button>

          <button
            onClick={resetOnboarding}
            className="py-3 px-4 rounded-xl bg-sub-surface hover:bg-theme-main text-primary-var border border-theme text-xs font-bold flex items-center justify-center space-x-1.5 transition-colors cursor-pointer"
          >
            <Sparkles className="w-4 h-4 text-[#EA3B35]" />
            <span>Re-run Onboarding Tour</span>
          </button>

          <button
            onClick={() => {
              requestDeleteConfirmation({
                title: '⚠️ Permanent Data Wipeout',
                message: 'CAUTION: Are you sure you want to clear ALL application data? All wallets, transactions, planned payments, and debts will be permanently deleted.',
                confirmText: 'Wipe All Data',
                isDanger: true,
                onConfirm: () => clearAllData()
              });
            }}
            className="py-3 px-4 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-500 border border-rose-500/30 text-xs font-bold flex items-center justify-center space-x-1.5 transition-colors cursor-pointer"
          >
            <Trash2 className="w-4 h-4" />
            <span>Clear All Data</span>
          </button>
        </div>
      </div>
    </div>
  );
};
