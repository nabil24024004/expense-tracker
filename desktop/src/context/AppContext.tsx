import React, { createContext, useContext, useState, useEffect } from 'react';
import type {
  Account,
  Expense,
  PlannedTransaction,
  DebtDue,
  AppSettings,
  ExpenseTrackerBackup,
  CategoryInfo
} from '../types';
import confetti from 'canvas-confetti';

export interface ConfirmDeleteOptions {
  title?: string;
  message: string;
  confirmText?: string;
  isDanger?: boolean;
  onConfirm: () => void;
}

const DEFAULT_SETTINGS: AppSettings = {
  isDarkMode: false,
  hideBalance: false,
  currency: '৳',
  pinLock: null,
  budget: {
    periodType: 'MONTHLY',
    limitAmount: 10000.00
  }
};

interface AppContextType {
  accounts: Account[];
  expenses: Expense[];
  plannedTransactions: PlannedTransaction[];
  debtsDues: DebtDue[];
  settings: AppSettings;
  searchQuery: string;
  activeCategoryFilter: string;
  activeTypeFilter: string;
  activeTab: string;
  userName: string;
  isLocked: boolean;
  hasCompletedOnboarding: boolean;
  confirmDeleteModal: ConfirmDeleteOptions | null;

  // Modals
  isAddExpenseOpen: boolean;
  isAddAccountOpen: boolean;
  isTransferOpen: boolean;
  isAddPlannedOpen: boolean;
  isAddDebtOpen: boolean;
  isSettleDebtOpen: boolean;
  selectedDebtForSettle: DebtDue | null;
  selectedAccountForDetails: Account | null;
  isExportImportOpen: boolean;
  setSelectedAccountForDetails: (account: Account | null) => void;

  // State Setters & Actions
  setActiveTab: (tab: string) => void;
  setSearchQuery: (query: string) => void;
  setActiveCategoryFilter: (cat: string) => void;
  setActiveTypeFilter: (type: string) => void;
  setUserName: (name: string) => void;
  setCurrency: (currency: string) => void;
  toggleDarkMode: () => void;
  toggleHideBalance: () => void;
  setPinLock: (pin: string | null) => void;
  unlockApp: (pin: string) => boolean;
  lockApp: () => void;
  setBudgetPeriod: (periodType: 'WEEKLY' | 'MONTHLY' | 'CUSTOM', limit: number) => void;

  // Modal Actions
  setIsAddExpenseOpen: (open: boolean) => void;
  setIsAddAccountOpen: (open: boolean) => void;
  setIsTransferOpen: (open: boolean) => void;
  setIsAddPlannedOpen: (open: boolean) => void;
  setIsAddDebtOpen: (open: boolean) => void;
  openSettleDebtModal: (debt: DebtDue) => void;
  setIsSettleDebtOpen: (open: boolean) => void;
  setIsExportImportOpen: (open: boolean) => void;
  requestDeleteConfirmation: (options: ConfirmDeleteOptions) => void;
  closeDeleteConfirmation: () => void;

  // CRUD Operations
  addExpense: (expense: Omit<Expense, 'id'>) => void;
  deleteExpense: (id: string) => void;
  addAccount: (account: Omit<Account, 'id' | 'currentBalance'>) => void;
  deleteAccount: (id: string) => void;
  transferFunds: (fromWalletId: string, toWalletId: string, amount: number, note: string) => boolean;
  addPlannedTransaction: (plan: Omit<PlannedTransaction, 'id'>) => void;
  deletePlannedTransaction: (id: string) => void;
  payPlannedTransaction: (plan: PlannedTransaction) => void;
  skipPlannedTransaction: (planId: string) => void;
  addDebtDue: (debt: Omit<DebtDue, 'id' | 'paidAmount' | 'isSettled'>) => void;
  settleDebtDue: (debtId: string, amountToPay: number, logAsTransaction: boolean, targetWalletId?: string) => void;
  deleteDebtDue: (id: string) => void;

  // Custom Categories
  customCategories: CategoryInfo[];
  addCustomCategory: (cat: Omit<CategoryInfo, 'id' | 'isCustom'>) => void;
  deleteCustomCategory: (id: string) => void;

  // Backup Sync
  importBackupData: (backup: ExpenseTrackerBackup) => void;
  clearAllData: () => void;
  triggerConfetti: () => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

// Helper to filter out any old demo items from localStorage
const sanitizeLoaded = <T extends { id: string }>(savedJson: string | null): T[] => {
  if (!savedJson) return [];
  try {
    const parsed: T[] = JSON.parse(savedJson);
    if (!Array.isArray(parsed)) return [];
    // Remove old demo IDs if present
    return parsed.filter(item => !item.id.startsWith('exp-') && !item.id.startsWith('acc-') && !item.id.startsWith('plan-') && !item.id.startsWith('debt-'));
  } catch (e) {
    return [];
  }
};

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Empty data by default - NO DEMO DATA
  const [accounts, setAccounts] = useState<Account[]>(() => {
    return sanitizeLoaded<Account>(localStorage.getItem('et_desktop_accounts'));
  });

  const [expenses, setExpenses] = useState<Expense[]>(() => {
    return sanitizeLoaded<Expense>(localStorage.getItem('et_desktop_expenses'));
  });

  const [plannedTransactions, setPlannedTransactions] = useState<PlannedTransaction[]>(() => {
    return sanitizeLoaded<PlannedTransaction>(localStorage.getItem('et_desktop_planned'));
  });

  const [debtsDues, setDebtsDues] = useState<DebtDue[]>(() => {
    return sanitizeLoaded<DebtDue>(localStorage.getItem('et_desktop_debts'));
  });

  const [settings, setSettings] = useState<AppSettings>(() => {
    const saved = localStorage.getItem('et_desktop_settings');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed) {
          if (!parsed.currency || parsed.currency === '$') {
            parsed.currency = '৳';
          }
          return parsed;
        }
      } catch (e) {}
    }
    return DEFAULT_SETTINGS;
  });

  const [userName, setUserNameState] = useState<string>(() => {
    return localStorage.getItem('et_desktop_username') || 'User';
  });

  const [isLocked, setIsLocked] = useState<boolean>(() => {
    return !!settings.pinLock;
  });

  const [searchQuery, setSearchQuery] = useState<string>('');
  const [activeCategoryFilter, setActiveCategoryFilter] = useState<string>('ALL');
  const [activeTypeFilter, setActiveTypeFilter] = useState<string>('ALL');
  const [activeTab, setActiveTab] = useState<string>('home');
  const [customCategories, setCustomCategories] = useState<CategoryInfo[]>(() => {
    const saved = localStorage.getItem('et_desktop_custom_categories');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {}
    }
    return [];
  });

  // Modals
  const [isAddExpenseOpen, setIsAddExpenseOpen] = useState<boolean>(false);
  const [isAddAccountOpen, setIsAddAccountOpen] = useState<boolean>(false);
  const [isTransferOpen, setIsTransferOpen] = useState<boolean>(false);
  const [isAddPlannedOpen, setIsAddPlannedOpen] = useState<boolean>(false);
  const [isAddDebtOpen, setIsAddDebtOpen] = useState<boolean>(false);
  const [isSettleDebtOpen, setIsSettleDebtOpen] = useState<boolean>(false);
  const [selectedDebtForSettle, setSelectedDebtForSettle] = useState<DebtDue | null>(null);
  const [selectedAccountForDetails, setSelectedAccountForDetails] = useState<Account | null>(null);
  const [isExportImportOpen, setIsExportImportOpen] = useState<boolean>(false);
  const [confirmDeleteModal, setConfirmDeleteModal] = useState<ConfirmDeleteOptions | null>(null);

  const requestDeleteConfirmation = (options: ConfirmDeleteOptions) => {
    setConfirmDeleteModal(options);
  };

  const closeDeleteConfirmation = () => {
    setConfirmDeleteModal(null);
  };

  // Sync state to localStorage
  useEffect(() => {
    localStorage.setItem('et_desktop_accounts', JSON.stringify(accounts));
  }, [accounts]);

  useEffect(() => {
    localStorage.setItem('et_desktop_expenses', JSON.stringify(expenses));
  }, [expenses]);

  useEffect(() => {
    localStorage.setItem('et_desktop_planned', JSON.stringify(plannedTransactions));
  }, [plannedTransactions]);

  useEffect(() => {
    localStorage.setItem('et_desktop_debts', JSON.stringify(debtsDues));
  }, [debtsDues]);

  useEffect(() => {
    localStorage.setItem('et_desktop_settings', JSON.stringify(settings));
    if (settings.isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [settings]);

  const setUserName = (name: string) => {
    setUserNameState(name);
    localStorage.setItem('et_desktop_username', name);
  };

  const setCurrency = (currencySymbol: string) => {
    setSettings(prev => ({ ...prev, currency: currencySymbol }));
  };

  // IPC listener for Quick Add shortcut (Ctrl+Alt+E)
  useEffect(() => {
    if (window.electronAPI) {
      const cleanup = window.electronAPI.onQuickAdd(() => {
        setIsAddExpenseOpen(true);
      });
      return cleanup;
    }
  }, []);

  const triggerConfetti = () => {
    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 }
    });
  };

  const toggleDarkMode = () => {
    setSettings(prev => ({ ...prev, isDarkMode: !prev.isDarkMode }));
  };

  const toggleHideBalance = () => {
    setSettings(prev => ({ ...prev, hideBalance: !prev.hideBalance }));
  };

  const setPinLock = (pin: string | null) => {
    setSettings(prev => ({ ...prev, pinLock: pin }));
  };

  const unlockApp = (pin: string): boolean => {
    if (settings.pinLock === pin) {
      setIsLocked(false);
      return true;
    }
    return false;
  };

  const lockApp = () => {
    if (settings.pinLock) {
      setIsLocked(true);
    }
  };

  const setBudgetPeriod = (periodType: 'WEEKLY' | 'MONTHLY' | 'CUSTOM', limitAmount: number) => {
    setSettings(prev => ({
      ...prev,
      budget: { ...prev.budget, periodType, limitAmount }
    }));
  };

  // CRUD: Expenses
  const addExpense = (expenseData: Omit<Expense, 'id'>) => {
    const newId = `usr-exp-${Date.now()}`;
    const newExpense: Expense = { ...expenseData, id: newId };

    setExpenses(prev => [newExpense, ...prev]);

    // Update account balance if walletId exists
    if (expenseData.walletId) {
      setAccounts(prev =>
        prev.map(acc => {
          if (acc.id === expenseData.walletId) {
            const delta = expenseData.type === 'INCOME' ? expenseData.amount : -expenseData.amount;
            return { ...acc, currentBalance: acc.currentBalance + delta };
          }
          return acc;
        })
      );
    }

    if (expenseData.type === 'INCOME') {
      triggerConfetti();
    }
  };

  const deleteExpense = (id: string) => {
    const target = expenses.find(e => e.id === id);
    if (!target) return;

    setExpenses(prev => prev.filter(e => e.id !== id));

    if (target.walletId) {
      setAccounts(prev =>
        prev.map(acc => {
          if (acc.id === target.walletId) {
            const delta = target.type === 'INCOME' ? -target.amount : target.amount;
            return { ...acc, currentBalance: acc.currentBalance + delta };
          }
          return acc;
        })
      );
    }
  };

  // CRUD: Accounts
  const addAccount = (accData: Omit<Account, 'id' | 'currentBalance'>) => {
    const newAcc: Account = {
      ...accData,
      id: `usr-acc-${Date.now()}`,
      currentBalance: accData.startingBalance
    };
    setAccounts(prev => [...prev, newAcc]);
  };

  const deleteAccount = (id: string) => {
    setAccounts(prev => prev.filter(a => a.id !== id));
  };

  // Transfer
  const transferFunds = (fromWalletId: string, toWalletId: string, amount: number, note: string): boolean => {
    if (fromWalletId === toWalletId || amount <= 0) return false;

    const fromWallet = accounts.find(a => a.id === fromWalletId);
    const toWallet = accounts.find(a => a.id === toWalletId);

    if (!fromWallet || !toWallet) return false;

    const timestamp = new Date().toISOString();

    const outExpense: Expense = {
      id: `usr-tr-out-${Date.now()}`,
      title: `Transfer to ${toWallet.name}`,
      amount,
      type: 'EXPENSE',
      category: 'Transfer',
      categoryIcon: 'ArrowRightLeft',
      walletId: fromWalletId,
      date: timestamp,
      tags: ['transfer'],
      note: note || `Transfer to ${toWallet.name}`
    };

    const inExpense: Expense = {
      id: `usr-tr-in-${Date.now()}`,
      title: `Transfer from ${fromWallet.name}`,
      amount,
      type: 'INCOME',
      category: 'Transfer',
      categoryIcon: 'ArrowRightLeft',
      walletId: toWalletId,
      date: timestamp,
      tags: ['transfer'],
      note: note || `Transfer from ${fromWallet.name}`
    };

    setExpenses(prev => [outExpense, inExpense, ...prev]);

    setAccounts(prev =>
      prev.map(a => {
        if (a.id === fromWalletId) return { ...a, currentBalance: a.currentBalance - amount };
        if (a.id === toWalletId) return { ...a, currentBalance: a.currentBalance + amount };
        return a;
      })
    );

    return true;
  };

  // Planned Transactions
  const addPlannedTransaction = (planData: Omit<PlannedTransaction, 'id'>) => {
    const newPlan: PlannedTransaction = { ...planData, id: `usr-plan-${Date.now()}` };
    setPlannedTransactions(prev => [...prev, newPlan]);
  };

  const deletePlannedTransaction = (id: string) => {
    setPlannedTransactions(prev => prev.filter(p => p.id !== id));
  };

  const payPlannedTransaction = (plan: PlannedTransaction) => {
    addExpense({
      title: plan.title,
      amount: plan.amount,
      type: plan.type,
      category: plan.category,
      categoryIcon: plan.type === 'INCOME' ? 'Briefcase' : 'Film',
      walletId: plan.walletId,
      date: new Date().toISOString(),
      tags: ['recurring', plan.category.toLowerCase()],
      note: `Scheduled ${plan.frequency.toLowerCase()} payment`
    });

    const current = new Date(plan.nextDueDate);
    if (plan.frequency === 'DAILY') current.setDate(current.getDate() + 1);
    else if (plan.frequency === 'WEEKLY') current.setDate(current.getDate() + 7);
    else if (plan.frequency === 'MONTHLY') current.setMonth(current.getMonth() + 1);
    else if (plan.frequency === 'YEARLY') current.setFullYear(current.getFullYear() + 1);

    setPlannedTransactions(prev =>
      prev.map(p => (p.id === plan.id ? { ...p, nextDueDate: current.toISOString().split('T')[0] } : p))
    );
  };

  const skipPlannedTransaction = (planId: string) => {
    setPlannedTransactions(prev =>
      prev.map(p => {
        if (p.id === planId) {
          const current = new Date(p.nextDueDate);
          if (p.frequency === 'DAILY') current.setDate(current.getDate() + 1);
          else if (p.frequency === 'WEEKLY') current.setDate(current.getDate() + 7);
          else if (p.frequency === 'MONTHLY') current.setMonth(current.getMonth() + 1);
          else if (p.frequency === 'YEARLY') current.setFullYear(current.getFullYear() + 1);
          return { ...p, nextDueDate: current.toISOString().split('T')[0] };
        }
        return p;
      })
    );
  };

  // Debts
  const addDebtDue = (debtData: Omit<DebtDue, 'id' | 'paidAmount' | 'isSettled'>) => {
    const newDebt: DebtDue = {
      ...debtData,
      id: `usr-debt-${Date.now()}`,
      paidAmount: 0,
      isSettled: false
    };
    setDebtsDues(prev => [newDebt, ...prev]);
  };

  const openSettleDebtModal = (debt: DebtDue) => {
    setSelectedDebtForSettle(debt);
    setIsSettleDebtOpen(true);
  };

  const settleDebtDue = (
    debtId: string,
    amountToPay: number,
    logAsTransaction: boolean,
    targetWalletId?: string
  ) => {
    const debt = debtsDues.find(d => d.id === debtId);
    if (!debt) return;

    const updatedPaid = debt.paidAmount + amountToPay;
    const isSettled = updatedPaid >= debt.amount;

    setDebtsDues(prev =>
      prev.map(d => (d.id === debtId ? { ...d, paidAmount: updatedPaid, isSettled } : d))
    );

    if (logAsTransaction && targetWalletId) {
      const type = debt.type === 'LENT' ? 'INCOME' : 'EXPENSE';
      addExpense({
        title: `Debt Repayment: ${debt.personName}`,
        amount: amountToPay,
        type,
        category: 'Debt Settlement',
        categoryIcon: 'Shield',
        walletId: targetWalletId,
        date: new Date().toISOString(),
        tags: ['debt', debt.type.toLowerCase()],
        note: `Settlement for ${debt.personName}`
      });
    }

    if (isSettled) {
      triggerConfetti();
    }
  };

  const deleteDebtDue = (id: string) => {
    setDebtsDues(prev => prev.filter(d => d.id !== id));
  };

  const addCustomCategory = (catData: Omit<CategoryInfo, 'id' | 'isCustom'>) => {
    const newCat: CategoryInfo = {
      ...catData,
      id: `cat-${Date.now()}`,
      isCustom: true
    };
    setCustomCategories(prev => [...prev, newCat]);
  };

  const deleteCustomCategory = (id: string) => {
    setCustomCategories(prev => prev.filter(c => c.id !== id));
  };

  const importBackupData = (backup: ExpenseTrackerBackup) => {
    if (backup.accounts) setAccounts(backup.accounts);
    if (backup.expenses) setExpenses(backup.expenses);
    if (backup.plannedTransactions) setPlannedTransactions(backup.plannedTransactions);
    if (backup.debtsDues) setDebtsDues(backup.debtsDues);
    if (backup.settings) setSettings(backup.settings);
    triggerConfetti();
  };

  const clearAllData = () => {
    setAccounts([]);
    setExpenses([]);
    setPlannedTransactions([]);
    setDebtsDues([]);
    localStorage.removeItem('et_desktop_accounts');
    localStorage.removeItem('et_desktop_expenses');
    localStorage.removeItem('et_desktop_planned');
    localStorage.removeItem('et_desktop_debts');
  };

  return (
    <AppContext.Provider
      value={{
        accounts,
        expenses,
        plannedTransactions,
        debtsDues,
        settings,
        searchQuery,
        activeCategoryFilter,
        activeTypeFilter,
        activeTab,
        userName,
        isLocked,
        confirmDeleteModal,
        isAddExpenseOpen,
        isAddAccountOpen,
        isTransferOpen,
        isAddPlannedOpen,
        isAddDebtOpen,
        isSettleDebtOpen,
        selectedDebtForSettle,
        selectedAccountForDetails,
        isExportImportOpen,
        setSelectedAccountForDetails,
        requestDeleteConfirmation,
        closeDeleteConfirmation,
        setActiveTab,
        setSearchQuery,
        setActiveCategoryFilter,
        setActiveTypeFilter,
        setUserName,
        setCurrency,
        toggleDarkMode,
        toggleHideBalance,
        setPinLock,
        unlockApp,
        lockApp,
        setBudgetPeriod,
        setIsAddExpenseOpen,
        setIsAddAccountOpen,
        setIsTransferOpen,
        setIsAddPlannedOpen,
        setIsAddDebtOpen,
        openSettleDebtModal,
        setIsSettleDebtOpen,
        setIsExportImportOpen,
        addExpense,
        deleteExpense,
        addAccount,
        deleteAccount,
        transferFunds,
        addPlannedTransaction,
        deletePlannedTransaction,
        payPlannedTransaction,
        skipPlannedTransaction,
        addDebtDue,
        settleDebtDue,
        deleteDebtDue,
        customCategories,
        addCustomCategory,
        deleteCustomCategory,
        importBackupData,
        clearAllData,
        triggerConfetti
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
