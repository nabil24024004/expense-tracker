import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { Lock, KeyRound, AlertCircle } from 'lucide-react';

export const SecurityLockModal: React.FC = () => {
  const { isLocked, unlockApp } = useApp();
  const [pinInput, setPinInput] = useState<string>('');
  const [error, setError] = useState<boolean>(false);

  if (!isLocked) return null;

  const handleUnlock = (e: React.FormEvent) => {
    e.preventDefault();
    const success = unlockApp(pinInput);
    if (success) {
      setPinInput('');
      setError(false);
    } else {
      setError(true);
      setPinInput('');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-app-main select-none p-4">
      <div className="w-full max-w-sm rounded-2xl bg-card-surface border border-theme p-8 space-y-6 text-center shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        <div className="w-16 h-16 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-500 mx-auto flex items-center justify-center">
          <Lock className="w-8 h-8" />
        </div>

        <div>
          <h2 className="text-xl font-bold text-primary-var">Application Locked</h2>
          <p className="text-xs text-secondary-var mt-1">Enter your Master PIN to unlock financial records</p>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-500 text-xs font-semibold flex items-center justify-center space-x-1.5">
            <AlertCircle className="w-4 h-4" />
            <span>Incorrect PIN entered</span>
          </div>
        )}

        <form onSubmit={handleUnlock} className="space-y-4">
          <input
            type="password"
            autoFocus
            maxLength={8}
            value={pinInput}
            onChange={(e) => setPinInput(e.target.value)}
            placeholder="••••"
            className="w-full bg-sub-surface border border-theme rounded-xl px-4 py-3 text-center text-2xl tracking-[0.5em] font-mono text-primary-var focus:outline-none focus:border-[#EA3B35]"
          />

          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white font-bold text-xs uppercase tracking-wider shadow-lg shadow-[#EA3B35]/25 transition-all active:scale-95 flex items-center justify-center space-x-2 cursor-pointer"
          >
            <KeyRound className="w-4 h-4" />
            <span>Unlock Application</span>
          </button>
        </form>
      </div>
    </div>
  );
};
