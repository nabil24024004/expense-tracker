import type { Account, Expense, PlannedTransaction, DebtDue, AppSettings, ExpenseTrackerBackup } from '../types';
import jsPDF from 'jspdf';
import * as XLSX from 'xlsx';

export function createJSONBackup(
  accounts: Account[],
  expenses: Expense[],
  plannedTransactions: PlannedTransaction[],
  debtsDues: DebtDue[],
  settings: AppSettings
): string {
  const backup: ExpenseTrackerBackup = {
    version: '2.0.0',
    exportDate: new Date().toISOString(),
    accounts,
    expenses,
    plannedTransactions,
    debtsDues,
    settings
  };

  return JSON.stringify(backup, null, 2);
}

export function parseJSONBackup(jsonString: string): ExpenseTrackerBackup | null {
  try {
    const parsed = JSON.parse(jsonString);
    if (parsed && Array.isArray(parsed.accounts) && Array.isArray(parsed.expenses)) {
      return parsed as ExpenseTrackerBackup;
    }
  } catch (e) {
    console.error('Failed to parse backup JSON:', e);
  }
  return null;
}

export function generateCSVContent(expenses: Expense[], accounts: Account[]): string {
  const accountMap = new Map(accounts.map(a => [a.id, a.name]));

  const rows = expenses.map(exp => ({
    ID: exp.id,
    Date: new Date(exp.date).toLocaleDateString() + ' ' + new Date(exp.date).toLocaleTimeString(),
    Type: exp.type,
    Title: exp.title,
    Amount: exp.amount,
    Category: exp.category,
    Wallet: accountMap.get(exp.walletId) || 'Unknown Wallet',
    Tags: exp.tags.join('; '),
    Note: exp.note || ''
  }));

  const worksheet = XLSX.utils.json_to_sheet(rows);
  const csvOutput = XLSX.utils.sheet_to_csv(worksheet);
  return csvOutput;
}

export function generatePDFReport(
  expenses: Expense[],
  accounts: Account[],
  settings: AppSettings,
  budgetPeriodName: string = 'Current Month'
): jsPDF {
  const doc = new jsPDF();
  const currency = settings.currency || '৳';

  // Title Header
  doc.setFillColor(12, 12, 14); // Dark charcoal accent
  doc.rect(0, 0, 210, 35, 'F');
  
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(22);
  doc.text('Expense Tracker Financial Report', 14, 20);
  
  doc.setFontSize(10);
  doc.setTextColor(160, 160, 160);
  doc.text(`Generated: ${new Date().toLocaleDateString()} | Period: ${budgetPeriodName}`, 14, 28);

  // Summary Totals
  const totalExpense = expenses
    .filter(e => e.type === 'EXPENSE')
    .reduce((sum, e) => sum + e.amount, 0);

  const totalIncome = expenses
    .filter(e => e.type === 'INCOME')
    .reduce((sum, e) => sum + e.amount, 0);

  const netSavings = totalIncome - totalExpense;

  doc.setTextColor(30, 30, 30);
  doc.setFontSize(12);
  doc.text(`Total Income: ${currency}${totalIncome.toFixed(2)}`, 14, 45);
  doc.text(`Total Expenses: ${currency}${totalExpense.toFixed(2)}`, 80, 45);
  doc.text(`Net Savings: ${currency}${netSavings.toFixed(2)}`, 150, 45);

  doc.setLineWidth(0.5);
  doc.setDrawColor(200, 200, 200);
  doc.line(14, 50, 196, 50);

  // Transactions Table Header
  doc.setFontSize(10);
  doc.setTextColor(100, 100, 100);
  doc.text('Date', 14, 58);
  doc.text('Title', 45, 58);
  doc.text('Category', 95, 58);
  doc.text('Type', 135, 58);
  doc.text('Amount', 170, 58);

  let y = 66;
  const accountMap = new Map(accounts.map(a => [a.id, a.name]));

  expenses.slice(0, 35).forEach(exp => {
    if (y > 275) {
      doc.addPage();
      y = 20;
    }

    doc.setFontSize(9);
    doc.setTextColor(40, 40, 40);
    doc.text(new Date(exp.date).toLocaleDateString(), 14, y);
    doc.text(exp.title.substring(0, 22), 45, y);
    doc.text(exp.category, 95, y);

    if (exp.type === 'INCOME') {
      doc.setTextColor(16, 185, 129); // Green
      doc.text('+ ' + currency + exp.amount.toFixed(2), 170, y);
      doc.text('Income', 135, y);
    } else {
      doc.setTextColor(239, 68, 68); // Red
      doc.text('- ' + currency + exp.amount.toFixed(2), 170, y);
      doc.text('Expense', 135, y);
    }

    y += 7;
  });

  return doc;
}
