import React from 'react';
import { useApp } from '../../context/AppContext';
import { Search, Eye, EyeOff, Lock, Sun, Moon, Database } from 'lucide-react';

export const Header: React.FC = () => {
  const {
    searchQuery,
    setSearchQuery,
    settings,
    userName,
    toggleHideBalance,
    toggleDarkMode,
    lockApp,
    setIsExportImportOpen
  } = useApp();

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good Morning';
    if (hour < 17) return 'Good Afternoon';
    return 'Good Evening';
  };

  return (
    <header className="h-16 border-b border-theme bg-app-main px-6 flex items-center justify-between select-none">
      {/* Greeting & Avatar */}
      <div className="flex items-center space-x-3">
        <div className="w-9 h-9 rounded-full bg-[#EA3B35] text-white flex items-center justify-center font-bold text-sm shadow-md shadow-[#EA3B35]/20">
          {userName.charAt(0).toUpperCase()}
        </div>
        <div>
          <div className="text-xs text-secondary-var">{getGreeting()} 👋</div>
          <div className="text-sm font-bold text-primary-var tracking-tight">{userName}</div>
        </div>
      </div>

      {/* Global Search Input */}
      <div className="relative w-72 hidden md:block">
        <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-secondary-var" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search transactions..."
          className="w-full bg-card-surface border border-theme rounded-xl pl-10 pr-4 py-2 text-xs text-primary-var placeholder:text-secondary-var focus:outline-none focus:border-[#EA3B35]/60 transition-colors"
        />
      </div>

      {/* Control Actions */}
      <div className="flex items-center space-x-2.5">
        {/* Privacy Masking Toggle */}
        <button
          onClick={toggleHideBalance}
          className={`p-2.5 rounded-xl border transition-colors flex items-center space-x-1.5 cursor-pointer ${
            settings.hideBalance
              ? 'bg-amber-500/10 border-amber-500/30 text-amber-500'
              : 'bg-card-surface border-theme text-secondary-var hover:text-primary-var'
          }`}
          title={settings.hideBalance ? 'Show sensitive balances' : 'Hide balances (Privacy Mode)'}
        >
          {settings.hideBalance ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          <span className="text-xs font-semibold hidden lg:inline">
            {settings.hideBalance ? 'Masked' : 'Privacy'}
          </span>
        </button>

        {/* Sync & Backup Dialog */}
        <button
          onClick={() => setIsExportImportOpen(true)}
          className="p-2.5 rounded-xl bg-card-surface border border-theme text-secondary-var hover:text-primary-var transition-colors flex items-center space-x-1.5 cursor-pointer"
          title="Backup & Data Import/Export"
        >
          <Database className="w-4 h-4 text-[#EA3B35]" />
          <span className="text-xs font-semibold hidden lg:inline">Sync Data</span>
        </button>

        {/* Lock App */}
        {settings.pinLock && (
          <button
            onClick={lockApp}
            className="p-2.5 rounded-xl bg-card-surface border border-theme text-secondary-var hover:text-primary-var transition-colors cursor-pointer"
            title="Lock Application"
          >
            <Lock className="w-4 h-4 text-rose-500" />
          </button>
        )}

        {/* Dark/Light Theme Toggle */}
        <button
          onClick={toggleDarkMode}
          className="p-2.5 rounded-xl bg-card-surface border border-theme text-secondary-var hover:text-primary-var transition-colors cursor-pointer"
          title={settings.isDarkMode ? 'Switch to Light Theme' : 'Switch to Dark Theme'}
        >
          {settings.isDarkMode ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-indigo-500" />}
        </button>
      </div>
    </header>
  );
};
