import React from 'react';
import { useApp } from '../../context/AppContext';
import { Wallet, TrendingUp, TrendingDown, Sparkles, Plus, Utensils, ShoppingBag, Car, Film, Activity, Zap, FolderOpen } from 'lucide-react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip } from 'recharts';

export const HomeScreen: React.FC = () => {
  const {
    accounts,
    expenses,
    settings,
    setIsAddExpenseOpen,
    setIsAddAccountOpen,
    setIsTransferOpen
  } = useApp();

  const currency = settings.currency || '৳';
  const formatAmount = (num: number) => {
    if (settings.hideBalance) return '••••';
    return `${currency}${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const includedAccounts = accounts.filter(a => !a.isExcluded);
  const totalBalance = includedAccounts.reduce((sum, a) => sum + a.currentBalance, 0);

  const currentMonthExpenses = expenses.filter(e => {
    const eDate = new Date(e.date);
    const now = new Date();
    return e.type === 'EXPENSE' && eDate.getMonth() === now.getMonth() && eDate.getFullYear() === now.getFullYear();
  });

  const totalSpentThisMonth = currentMonthExpenses.reduce((sum, e) => sum + e.amount, 0);
  const budgetLimit = settings.budget.limitAmount || 10000;
  const remainingBudget = budgetLimit - totalSpentThisMonth;
  const budgetPercentage = Math.min(100, Math.max(0, Math.round((totalSpentThisMonth / budgetLimit) * 100)));

  const chartData = Array.from({ length: 7 }).map((_, idx) => {
    const d = new Date();
    d.setDate(d.getDate() - (6 - idx));
    const dateStr = d.toISOString().split('T')[0];
    const dayLabel = d.toLocaleDateString('en-US', { weekday: 'short' });

    const daySpent = expenses
      .filter(e => e.type === 'EXPENSE' && e.date.startsWith(dateStr))
      .reduce((sum, e) => sum + e.amount, 0);

    return { day: dayLabel, amount: daySpent };
  });

  const quickCategories = [
    { name: 'Food', icon: Utensils, color: 'bg-[#EA3B35]/10 text-[#EA3B35] border-[#EA3B35]/20' },
    { name: 'Shopping', icon: ShoppingBag, color: 'bg-indigo-500/10 text-indigo-500 border-indigo-500/20' },
    { name: 'Travel', icon: Car, color: 'bg-blue-500/10 text-blue-500 border-blue-500/20' },
    { name: 'Bills', icon: Film, color: 'bg-purple-500/10 text-purple-500 border-purple-500/20' },
    { name: 'Health', icon: Activity, color: 'bg-rose-500/10 text-rose-500 border-rose-500/20' },
    { name: 'Utilities', icon: Zap, color: 'bg-amber-500/10 text-amber-500 border-amber-500/20' }
  ];

  return (
    <div className="space-y-6 pb-12">
      {/* Top Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* Card 1: Total Balance / Net Worth */}
        <div className="p-5 rounded-2xl bg-card-surface border border-theme relative overflow-hidden group">
          <div className="absolute -right-6 -bottom-6 w-28 h-28 bg-[#EA3B35]/15 rounded-full blur-2xl group-hover:bg-[#EA3B35]/25 transition-all" />
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-secondary-var tracking-wide uppercase">Primary Net Balance</span>
            <div className="p-2 rounded-xl bg-[#EA3B35]/10 text-[#EA3B35] border border-[#EA3B35]/20">
              <Wallet className="w-4 h-4" />
            </div>
          </div>
          <div className="text-3xl font-extrabold text-primary-var tracking-tight">{formatAmount(totalBalance)}</div>
          <div className="mt-3 text-[11px] text-secondary-var flex items-center justify-between pt-1 border-t border-theme">
            <span>{accounts.length === 0 ? 'No active wallets' : `${includedAccounts.length} active wallet(s)`}</span>
            {accounts.length > 0 ? (
              <button onClick={() => setIsTransferOpen(true)} className="text-[#EA3B35] hover:underline font-semibold cursor-pointer">
                Transfer &rarr;
              </button>
            ) : (
              <button onClick={() => setIsAddAccountOpen(true)} className="text-[#EA3B35] hover:underline font-semibold cursor-pointer">
                + Create Wallet
              </button>
            )}
          </div>
        </div>

        {/* Card 2: Remaining Budget Tracker */}
        <div className="p-5 rounded-2xl bg-card-surface border border-theme relative overflow-hidden">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center space-x-2">
              <span className="text-xs font-bold text-secondary-var tracking-wide uppercase">Remaining Budget</span>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#EA3B35]/10 border border-[#EA3B35]/30 text-[#EA3B35] font-bold">
                {settings.budget.periodType}
              </span>
            </div>
            <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="text-3xl font-extrabold text-primary-var tracking-tight">
            {formatAmount(remainingBudget)}
          </div>

          {/* Progress Bar */}
          <div className="mt-3 space-y-1">
            <div className="w-full h-2.5 bg-sub-surface rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  budgetPercentage > 90 ? 'bg-rose-500' : budgetPercentage > 75 ? 'bg-amber-500' : 'bg-[#EA3B35]'
                }`}
                style={{ width: `${budgetPercentage}%` }}
              />
            </div>
            <div className="flex justify-between text-[11px] text-secondary-var font-medium pt-0.5">
              <span>Spent: {formatAmount(totalSpentThisMonth)}</span>
              <span>Limit: {formatAmount(budgetLimit)}</span>
            </div>
          </div>
        </div>

        {/* Card 3: Smart Insights Banner */}
        <div className="p-5 rounded-2xl bg-card-surface border border-theme flex flex-col justify-between">
          <div className="flex items-center space-x-2">
            <Sparkles className="w-4 h-4 text-amber-500" />
            <span className="text-xs font-bold text-primary-var uppercase tracking-wide">Quick Insights</span>
          </div>
          <div className="my-2 space-y-1">
            <div className="text-sm font-semibold text-primary-var">
              {expenses.length === 0
                ? 'Welcome! Log your first transaction to see live insights.'
                : budgetPercentage > 85
                ? '⚠️ Budget Warning: Over 85% spent.'
                : '💡 You are on track with your spending targets!'}
            </div>
            <p className="text-xs text-secondary-var">
              Daily average: {formatAmount(totalSpentThisMonth / (new Date().getDate() || 1))}
            </p>
          </div>
          <div className="text-[11px] text-[#EA3B35] font-bold cursor-pointer hover:underline" onClick={() => setIsAddExpenseOpen(true)}>
            + Tap to log transaction &rarr;
          </div>
        </div>
      </div>

      {/* Main Section: 7-Day Spending Chart & Quick Spend Hub */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 7-Day Spending Trend Line Chart */}
        <div className="lg:col-span-2 p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-primary-var">7-Day Spending Trend</h3>
              <p className="text-xs text-secondary-var">Visual outflow graph matching Android app</p>
            </div>
            <div className="text-xs font-bold text-[#EA3B35] bg-[#EA3B35]/10 border border-[#EA3B35]/30 px-3 py-1 rounded-xl">
              7D Outflow: {formatAmount(chartData.reduce((s, c) => s + c.amount, 0))}
            </div>
          </div>

          <div className="h-56 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="spendGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#EA3B35" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#EA3B35" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="day" stroke="var(--text-secondary)" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="var(--text-secondary)" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => `${currency}${v}`} />
                <Tooltip
                  contentStyle={{ backgroundColor: 'var(--card-surface)', borderColor: 'var(--border-color)', borderRadius: '12px', color: 'var(--text-primary)', fontSize: '12px' }}
                  formatter={(val: any) => [`${currency}${val}`, 'Spent']}
                />
                <Area type="monotone" dataKey="amount" stroke="#EA3B35" strokeWidth={3} fillOpacity={1} fill="url(#spendGradient)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Quick Spend Hub */}
        <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
          <div>
            <h3 className="text-base font-bold text-primary-var">Quick Spend Hub</h3>
            <p className="text-xs text-secondary-var">Tap category to log transaction</p>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-1">
            {quickCategories.map((cat) => {
              const Icon = cat.icon;
              return (
                <button
                  key={cat.name}
                  onClick={() => setIsAddExpenseOpen(true)}
                  className={`p-3.5 rounded-xl border ${cat.color} flex flex-col items-center justify-center space-y-2 hover:scale-[1.02] active:scale-95 transition-all cursor-pointer`}
                >
                  <Icon className="w-5 h-5" />
                  <span className="text-xs font-bold">{cat.name}</span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Recent Transactions Section */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-primary-var">Recent Transactions</h3>
            <p className="text-xs text-secondary-var">Latest activity logged across your wallets</p>
          </div>
          <button
            onClick={() => setIsAddExpenseOpen(true)}
            className="px-3.5 py-1.5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold flex items-center space-x-1 transition-all shadow-md shadow-[#EA3B35]/20 cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add Transaction</span>
          </button>
        </div>

        {expenses.length === 0 ? (
          <div className="py-12 text-center space-y-3 border-2 border-dashed border-theme rounded-2xl">
            <div className="w-12 h-12 rounded-full bg-sub-surface text-secondary-var mx-auto flex items-center justify-center">
              <FolderOpen className="w-6 h-6" />
            </div>
            <div>
              <div className="text-sm font-semibold text-primary-var">No transactions logged yet</div>
              <p className="text-xs text-secondary-var mt-0.5">Tap 'Add Transaction' or use a category shortcut to get started.</p>
            </div>
            <button
              onClick={() => setIsAddExpenseOpen(true)}
              className="px-4 py-2 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold shadow-md shadow-[#EA3B35]/20 transition-all cursor-pointer inline-flex items-center space-x-1.5"
            >
              <Plus className="w-4 h-4" />
              <span>Log First Transaction</span>
            </button>
          </div>
        ) : (
          <div className="divide-y divide-theme">
            {expenses.slice(0, 5).map((exp) => {
              const acc = accounts.find((a) => a.id === exp.walletId);
              const isIncome = exp.type === 'INCOME';

              return (
                <div key={exp.id} className="py-3 flex items-center justify-between hover:bg-sub-surface/50 px-2 rounded-xl transition-colors">
                  <div className="flex items-center space-x-3.5">
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                        isIncome ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20' : 'bg-[#EA3B35]/10 text-[#EA3B35] border border-[#EA3B35]/20'
                      }`}
                    >
                      {isIncome ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-primary-var">{exp.title}</div>
                      <div className="flex items-center space-x-2 text-xs text-secondary-var">
                        <span>{exp.category}</span>
                        <span>•</span>
                        <span className="px-1.5 py-0.5 rounded bg-sub-surface border border-theme" style={{ color: acc?.colorHex || 'var(--text-primary)' }}>
                          {acc?.name || 'Cash'}
                        </span>
                        <span>•</span>
                        <span>{new Date(exp.date).toLocaleDateString()}</span>
                      </div>
                    </div>
                  </div>

                  <div className={`text-sm font-bold ${isIncome ? 'text-emerald-500' : 'text-[#EA3B35]'}`}>
                    {isIncome ? '+' : '-'} {formatAmount(exp.amount)}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
