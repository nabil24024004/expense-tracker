export type AccountType = 'cash' | 'bank' | 'credit' | 'savings' | 'investment';

export interface Account {
  id: string;
  name: string;
  bankName: string;
  cardType: AccountType;
  startingBalance: number;
  currentBalance: number;
  colorHex: string;
  isExcluded: boolean; // Exclude from primary dashboard net balance
}

export type TransactionType = 'EXPENSE' | 'INCOME';

export interface Expense {
  id: string;
  title: string;
  amount: number;
  type: TransactionType;
  category: string;
  categoryIcon: string;
  walletId: string;
  date: string; // ISO String format YYYY-MM-DDTHH:mm
  tags: string[];
  note: string;
}

export type FrequencyType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface PlannedTransaction {
  id: string;
  title: string;
  amount: number;
  type: TransactionType;
  category: string;
  walletId: string;
  frequency: FrequencyType;
  nextDueDate: string; // YYYY-MM-DD
  autoDeposit: boolean;
}

export type DebtType = 'LENT' | 'BORROWED';

export interface DebtDue {
  id: string;
  personName: string;
  type: DebtType;
  amount: number;
  paidAmount: number;
  dueDate: string; // YYYY-MM-DD
  note: string;
  isSettled: boolean;
}

export type BudgetPeriodType = 'WEEKLY' | 'MONTHLY' | 'CUSTOM';

export interface BudgetSettings {
  periodType: BudgetPeriodType;
  limitAmount: number;
  customStartDate?: string;
  customEndDate?: string;
}

export interface AppSettings {
  isDarkMode: boolean;
  hideBalance: boolean;
  currency: string;
  pinLock: string | null;
  budget: BudgetSettings;
}

export interface CategoryInfo {
  id?: string;
  name: string;
  iconName: string;
  color: string;
  type: TransactionType;
  isCustom?: boolean;
}

// Global JSON Backup Migration Interface
export interface ExpenseTrackerBackup {
  version: string;
  exportDate: string;
  accounts: Account[];
  expenses: Expense[];
  plannedTransactions: PlannedTransaction[];
  debtsDues: DebtDue[];
  settings: AppSettings;
}
