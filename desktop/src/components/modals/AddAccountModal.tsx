import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import type { AccountType } from '../../types';
import { X, Wallet, Palette } from 'lucide-react';

export const AddAccountModal: React.FC = () => {
  const {
    isAddAccountOpen,
    setIsAddAccountOpen,
    addAccount
  } = useApp();

  const [name, setName] = useState<string>('');
  const [bankName, setBankName] = useState<string>('');
  const [cardType, setCardType] = useState<AccountType>('bank');
  const [startingBalance, setStartingBalance] = useState<string>('0');
  const [colorHex, setColorHex] = useState<string>('#3B82F6');
  const [isExcluded, setIsExcluded] = useState<boolean>(false);

  if (!isAddAccountOpen) return null;

  const colorPresets = ['#10B981', '#3B82F6', '#8B5CF6', '#F59E0B', '#EF4444', '#EC4899', '#06B6D4'];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const balance = parseFloat(startingBalance) || 0;
    if (!name.trim()) return;

    addAccount({
      name: name.trim(),
      bankName: bankName.trim() || 'Personal',
      cardType,
      startingBalance: balance,
      colorHex,
      isExcluded
    });

    setName('');
    setBankName('');
    setStartingBalance('0');
    setIsAddAccountOpen(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 select-none">
      <div className="w-full max-w-md rounded-2xl bg-card-surface border border-theme p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-primary-var flex items-center space-x-2">
            <Wallet className="w-5 h-5 text-[#EA3B35]" />
            <span>Create New Wallet / Card</span>
          </h3>
          <button
            onClick={() => setIsAddAccountOpen(false)}
            className="p-1 rounded-lg text-secondary-var hover:text-primary-var hover:bg-sub-surface transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-secondary-var">Wallet / Account Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Main Payroll, Cash, Savings"
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var focus:outline-none focus:border-[#EA3B35]"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-medium text-secondary-var">Bank / Provider</label>
              <input
                type="text"
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                placeholder="e.g. Chase, Citi, Cash"
                className="w-full bg-sub-surface border border-theme rounded-xl px-3 py-2 text-xs text-primary-var focus:outline-none focus:border-[#EA3B35]"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-medium text-secondary-var">Account Type</label>
              <select
                value={cardType}
                onChange={(e) => setCardType(e.target.value as AccountType)}
                className="w-full bg-sub-surface border border-theme rounded-xl px-3 py-2 text-xs text-primary-var focus:outline-none focus:border-[#EA3B35]"
              >
                <option value="bank">Bank Account</option>
                <option value="cash">Cash</option>
                <option value="credit">Credit Card</option>
                <option value="savings">Savings</option>
                <option value="investment">Investment</option>
              </select>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-secondary-var">Initial Starting Balance</label>
            <input
              type="number"
              value={startingBalance}
              onChange={(e) => setStartingBalance(e.target.value)}
              placeholder="0.00"
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var font-mono focus:outline-none focus:border-[#EA3B35]"
            />
          </div>

          {/* Color Preset Palette */}
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-secondary-var flex items-center space-x-1">
              <Palette className="w-3.5 h-3.5 text-[#EA3B35]" />
              <span>Card Badge Color</span>
            </label>
            <div className="flex items-center space-x-2">
              {colorPresets.map(hex => (
                <button
                  type="button"
                  key={hex}
                  onClick={() => setColorHex(hex)}
                  className={`w-7 h-7 rounded-full border-2 transition-transform cursor-pointer ${
                    colorHex === hex ? 'border-primary-var scale-110' : 'border-transparent'
                  }`}
                  style={{ backgroundColor: hex }}
                />
              ))}
            </div>
          </div>

          {/* Exclude from Net Balance Checkbox */}
          <div className="flex items-center space-x-2 pt-2">
            <input
              type="checkbox"
              id="isExcludedCheck"
              checked={isExcluded}
              onChange={(e) => setIsExcluded(e.target.checked)}
              className="w-4 h-4 rounded bg-sub-surface border-theme text-[#EA3B35] focus:ring-0 cursor-pointer"
            />
            <label htmlFor="isExcludedCheck" className="text-xs text-secondary-var cursor-pointer">
              Exclude wallet from Primary Net Balance total
            </label>
          </div>

          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-[#EA3B35]/25 transition-all active:scale-95 cursor-pointer"
          >
            Create Wallet Card
          </button>
        </form>
      </div>
    </div>
  );
};
