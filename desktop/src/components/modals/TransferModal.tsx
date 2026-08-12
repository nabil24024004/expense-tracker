import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { X, ArrowRightLeft, Calculator } from 'lucide-react';
import { evaluateMathExpression } from '../../utils/mathEvaluator';

export const TransferModal: React.FC = () => {
  const {
    isTransferOpen,
    setIsTransferOpen,
    accounts,
    transferFunds,
    settings
  } = useApp();

  const [fromWalletId, setFromWalletId] = useState<string>(accounts[0]?.id || '');
  const [toWalletId, setToWalletId] = useState<string>(accounts[1]?.id || accounts[0]?.id || '');
  const [amountInput, setAmountInput] = useState<string>('');
  const [note, setNote] = useState<string>('');

  if (!isTransferOpen) return null;

  const mathResult = evaluateMathExpression(amountInput);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!mathResult.isValid || mathResult.value <= 0 || fromWalletId === toWalletId) return;

    const ok = transferFunds(fromWalletId, toWalletId, mathResult.value, note);
    if (ok) {
      setAmountInput('');
      setNote('');
      setIsTransferOpen(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 select-none">
      <div className="w-full max-w-md rounded-2xl bg-card-surface border border-theme p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-primary-var flex items-center space-x-2">
            <ArrowRightLeft className="w-5 h-5 text-purple-500" />
            <span>Transfer Funds Between Wallets</span>
          </h3>
          <button
            onClick={() => setIsTransferOpen(false)}
            className="p-1 rounded-lg text-secondary-var hover:text-primary-var hover:bg-sub-surface transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-medium text-secondary-var">From Source Wallet</label>
            <select
              value={fromWalletId}
              onChange={(e) => setFromWalletId(e.target.value)}
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var focus:outline-none focus:border-purple-500"
            >
              {accounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} ({settings.currency || '৳'}{acc.currentBalance.toFixed(2)})
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-secondary-var">To Destination Wallet</label>
            <select
              value={toWalletId}
              onChange={(e) => setToWalletId(e.target.value)}
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var focus:outline-none focus:border-purple-500"
            >
              {accounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} ({settings.currency || '৳'}{acc.currentBalance.toFixed(2)})
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <div className="flex justify-between items-center text-xs">
              <label className="font-medium text-secondary-var flex items-center space-x-1">
                <Calculator className="w-3.5 h-3.5 text-purple-500" />
                <span>Transfer Amount</span>
              </label>
              {mathResult.isValid && (
                <span className="text-purple-500 font-bold text-xs bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/20">
                  = {settings.currency || '৳'}{mathResult.value.toFixed(2)}
                </span>
              )}
            </div>
            <input
              type="text"
              required
              value={amountInput}
              onChange={(e) => setAmountInput(e.target.value)}
              placeholder="e.g. 500"
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var font-mono focus:outline-none focus:border-purple-500"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-secondary-var">Transfer Note (Optional)</label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="e.g. ATM cash withdrawal"
              className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-2.5 text-sm text-primary-var focus:outline-none focus:border-purple-500"
            />
          </div>

          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-purple-500/20 transition-all active:scale-95 cursor-pointer"
          >
            Execute Inter-Wallet Transfer
          </button>
        </form>
      </div>
    </div>
  );
};
