import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { createJSONBackup, parseJSONBackup, generateCSVContent, generatePDFReport } from '../../utils/exportImport';
import { X, Database, Download, Upload, FileText, CheckCircle2, AlertCircle } from 'lucide-react';

export const ExportImportModal: React.FC = () => {
  const {
    isExportImportOpen,
    setIsExportImportOpen,
    accounts,
    expenses,
    plannedTransactions,
    debtsDues,
    settings,
    importBackupData
  } = useApp();

  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  if (!isExportImportOpen) return null;

  // 1. Export JSON Backup
  const handleExportJSON = async () => {
    const jsonStr = createJSONBackup(accounts, expenses, plannedTransactions, debtsDues, settings);

    if (window.electronAPI) {
      const res = await window.electronAPI.saveFile('expense_tracker_backup.json', jsonStr, [
        { name: 'JSON Backup', extensions: ['json'] }
      ]);

      if (res.success) {
        setStatusMessage({ type: 'success', text: `Backup exported successfully to ${res.filePath}` });
      }
    } else {
      // Web Fallback download
      const blob = new Blob([jsonStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'expense_tracker_backup.json';
      a.click();
      setStatusMessage({ type: 'success', text: 'Backup downloaded as expense_tracker_backup.json' });
    }
  };

  // 2. Import JSON Backup
  const handleImportJSON = async () => {
    let jsonStr = '';

    if (window.electronAPI) {
      const res = await window.electronAPI.openFile([{ name: 'JSON Backup', extensions: ['json'] }]);
      if (res.success && res.content) {
        jsonStr = res.content;
      } else {
        return;
      }
    } else {
      // Browser file input fallback
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = '.json';
      input.onchange = (e: any) => {
        const file = e.target.files[0];
        if (file) {
          const reader = new FileReader();
          reader.onload = (event) => {
            const content = event.target?.result as string;
            const backup = parseJSONBackup(content);
            if (backup) {
              importBackupData(backup);
              setStatusMessage({ type: 'success', text: 'Backup data imported successfully!' });
            } else {
              setStatusMessage({ type: 'error', text: 'Invalid backup file format' });
            }
          };
          reader.readAsText(file);
        }
      };
      input.click();
      return;
    }

    if (jsonStr) {
      const backup = parseJSONBackup(jsonStr);
      if (backup) {
        importBackupData(backup);
        setStatusMessage({ type: 'success', text: 'Backup data imported successfully!' });
      } else {
        setStatusMessage({ type: 'error', text: 'Failed to parse JSON backup' });
      }
    }
  };

  // 3. Export CSV
  const handleExportCSV = async () => {
    const csvContent = generateCSVContent(expenses, accounts);

    if (window.electronAPI) {
      const res = await window.electronAPI.saveFile('expenses_ledger.csv', csvContent, [
        { name: 'CSV File', extensions: ['csv'] }
      ]);
      if (res.success) {
        setStatusMessage({ type: 'success', text: `CSV exported to ${res.filePath}` });
      }
    } else {
      const blob = new Blob([csvContent], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'expenses_ledger.csv';
      a.click();
      setStatusMessage({ type: 'success', text: 'CSV ledger downloaded' });
    }
  };

  // 4. Export PDF Financial Report
  const handleExportPDF = async () => {
    const doc = generatePDFReport(expenses, accounts, settings, settings.budget.periodType);
    doc.save('Expense_Tracker_Financial_Report.pdf');
    setStatusMessage({ type: 'success', text: 'PDF Financial Report downloaded!' });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 select-none">
      <div className="w-full max-w-lg rounded-2xl bg-card-surface border border-theme p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-primary-var flex items-center space-x-2">
            <Database className="w-5 h-5 text-[#EA3B35]" />
            <span>Data Sync & Export/Import Center</span>
          </h3>
          <button
            onClick={() => setIsExportImportOpen(false)}
            className="p-1 rounded-lg text-secondary-var hover:text-primary-var hover:bg-sub-surface transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {statusMessage && (
          <div
            className={`p-3 rounded-xl border text-xs font-semibold flex items-center space-x-2 ${
              statusMessage.type === 'success'
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-500'
                : 'bg-rose-500/10 border-rose-500/30 text-rose-500'
            }`}
          >
            {statusMessage.type === 'success' ? <CheckCircle2 className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
            <span>{statusMessage.text}</span>
          </div>
        )}

        <div className="space-y-3">
          {/* Card 1: Android <-> Desktop Sync (JSON) */}
          <div className="p-4 rounded-xl bg-sub-surface border border-theme space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-sm font-semibold text-primary-var">Full JSON Backup Sync</h4>
                <p className="text-xs text-secondary-var">Seamlessly transfer data between Android and Desktop</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-1">
              <button
                onClick={handleExportJSON}
                className="py-2.5 px-3 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-500 border border-emerald-500/30 text-xs font-semibold flex items-center justify-center space-x-1.5 transition-colors cursor-pointer"
              >
                <Download className="w-4 h-4" />
                <span>Export JSON</span>
              </button>

              <button
                onClick={handleImportJSON}
                className="py-2.5 px-3 rounded-xl bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-500 border border-indigo-500/30 text-xs font-semibold flex items-center justify-center space-x-1.5 transition-colors cursor-pointer"
              >
                <Upload className="w-4 h-4" />
                <span>Import JSON</span>
              </button>
            </div>
          </div>

          {/* Card 2: Export CSV Ledger */}
          <div className="p-4 rounded-xl bg-sub-surface border border-theme flex items-center justify-between">
            <div>
              <h4 className="text-sm font-semibold text-primary-var">CSV Ledger Export</h4>
              <p className="text-xs text-secondary-var">Open transaction logs in Excel or Google Sheets</p>
            </div>
            <button
              onClick={handleExportCSV}
              className="px-4 py-2 rounded-xl bg-card-surface hover:bg-sub-surface text-primary-var border border-theme text-xs font-semibold flex items-center space-x-1.5 transition-colors cursor-pointer"
            >
              <FileText className="w-4 h-4 text-purple-500" />
              <span>Export CSV</span>
            </button>
          </div>

          {/* Card 3: Generate PDF Report */}
          <div className="p-4 rounded-xl bg-sub-surface border border-theme flex items-center justify-between">
            <div>
              <h4 className="text-sm font-semibold text-primary-var">PDF Financial Summary Report</h4>
              <p className="text-xs text-secondary-var">Download a formatted PDF statement with totals</p>
            </div>
            <button
              onClick={handleExportPDF}
              className="px-4 py-2 rounded-xl bg-card-surface hover:bg-sub-surface text-primary-var border border-theme text-xs font-semibold flex items-center space-x-1.5 transition-colors cursor-pointer"
            >
              <FileText className="w-4 h-4 text-amber-500" />
              <span>Download PDF</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
