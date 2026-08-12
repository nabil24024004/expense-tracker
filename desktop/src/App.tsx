import React from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { TitleBar } from './components/layout/TitleBar';
import { Header } from './components/layout/Header';
import { Sidebar } from './components/layout/Sidebar';

import { HomeScreen } from './components/screens/HomeScreen';
import { AccountsScreen } from './components/screens/AccountsScreen';
import { AnalyticsScreen } from './components/screens/AnalyticsScreen';
import { PlannedScreen } from './components/screens/PlannedScreen';
import { DebtsScreen } from './components/screens/DebtsScreen';
import { HistoryScreen } from './components/screens/HistoryScreen';
import { SettingsScreen } from './components/screens/SettingsScreen';

import { AddExpenseModal } from './components/modals/AddExpenseModal';
import { TransferModal } from './components/modals/TransferModal';
import { AddAccountModal } from './components/modals/AddAccountModal';
import { AddPlannedModal } from './components/modals/AddPlannedModal';
import { AddDebtModal } from './components/modals/AddDebtModal';
import { SettleDebtModal } from './components/modals/SettleDebtModal';
import { ExportImportModal } from './components/modals/ExportImportModal';
import { SecurityLockModal } from './components/modals/SecurityLockModal';
import { ConfirmDeleteModal } from './components/modals/ConfirmDeleteModal';
import { AccountDetailsModal } from './components/modals/AccountDetailsModal';

const AppContent: React.FC = () => {
  const { activeTab, plannedTransactions } = useApp();

  React.useEffect(() => {
    if (window.electronAPI && plannedTransactions.length > 0) {
      const todayStr = new Date().toISOString().split('T')[0];
      const dueCount = plannedTransactions.filter(p => p.nextDueDate <= todayStr).length;
      if (dueCount > 0) {
        window.electronAPI.sendNotification(
          'Expense Tracker Reminder 🔔',
          `You have ${dueCount} scheduled payment(s) due or overdue today.`
        );
      }
    }
  }, []);

  const renderActiveScreen = () => {
    switch (activeTab) {
      case 'home': return <HomeScreen />;
      case 'accounts': return <AccountsScreen />;
      case 'analytics': return <AnalyticsScreen />;
      case 'planned': return <PlannedScreen />;
      case 'debts': return <DebtsScreen />;
      case 'history': return <HistoryScreen />;
      case 'settings': return <SettingsScreen />;
      default: return <HomeScreen />;
    }
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-app-main text-primary-var overflow-hidden transition-colors duration-200">
      {/* Custom Frameless Desktop TitleBar */}
      <TitleBar />

      <div className="flex flex-1 overflow-hidden">
        {/* Navigation Sidebar */}
        <Sidebar />

        {/* Main Content Area */}
        <div className="flex-1 flex flex-col overflow-hidden">
          <Header />
          <main className="flex-1 overflow-y-auto p-6 bg-app-main">
            {renderActiveScreen()}
          </main>
        </div>
      </div>

      {/* Global Interactive Modal Overlays */}
      <AddExpenseModal />
      <TransferModal />
      <AddAccountModal />
      <AddPlannedModal />
      <AddDebtModal />
      <SettleDebtModal />
      <ExportImportModal />
      <SecurityLockModal />
      <ConfirmDeleteModal />
      <AccountDetailsModal />
    </div>
  );
};

export default function App() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  );
}
