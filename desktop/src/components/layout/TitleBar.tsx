import React from 'react';
import { Minus, Square, X } from 'lucide-react';

export const TitleBar: React.FC = () => {
  const handleMinimize = () => {
    if (window.electronAPI) window.electronAPI.minimizeWindow();
  };

  const handleMaximize = () => {
    if (window.electronAPI) window.electronAPI.maximizeWindow();
  };

  const handleClose = () => {
    if (window.electronAPI) window.electronAPI.closeWindow();
  };

  return (
    <div className="h-9 w-full bg-app-main border-b border-theme flex items-center justify-between px-3 drag-region select-none text-xs text-secondary-var z-50">
      <div className="flex items-center space-x-2.5">
        <img
          src="/logo.png"
          alt="Expense Tracker Logo"
          className="w-5 h-5 object-contain"
        />
        <span className="font-bold text-primary-var tracking-tight text-xs">Expense Tracker Desktop</span>
        <span className="text-[10px] px-1.5 py-0.5 rounded bg-sub-surface text-secondary-var border border-theme font-semibold">v2.0.0</span>
      </div>

      <div className="flex items-center space-x-1 no-drag">
        <button
          onClick={handleMinimize}
          className="w-8 h-6 flex items-center justify-center rounded hover:bg-sub-surface text-secondary-var hover:text-primary-var transition-colors cursor-pointer"
          title="Minimize"
        >
          <Minus className="w-3.5 h-3.5" />
        </button>
        <button
          onClick={handleMaximize}
          className="w-8 h-6 flex items-center justify-center rounded hover:bg-sub-surface text-secondary-var hover:text-primary-var transition-colors cursor-pointer"
          title="Maximize"
        >
          <Square className="w-3 h-3" />
        </button>
        <button
          onClick={handleClose}
          className="w-8 h-6 flex items-center justify-center rounded hover:bg-rose-600 text-secondary-var hover:text-white transition-colors cursor-pointer"
          title="Close"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
