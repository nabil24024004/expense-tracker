import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { BarChart3, TrendingUp, TrendingDown, PiggyBank, PieChart as PieChartIcon, Calendar, ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, PieChart, Pie, Cell } from 'recharts';

export const AnalyticsScreen: React.FC = () => {
  const { expenses, settings } = useApp();
  const [timeRange, setTimeRange] = useState<'THIS_MONTH' | 'LAST_30_DAYS' | 'ALL_TIME'>('THIS_MONTH');

  const currency = settings.currency || '৳';
  const formatAmount = (num: number) => {
    if (settings.hideBalance) return '••••';
    return `${currency}${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  // Filter expenses based on selected timeRange
  const filteredExpenses = expenses.filter(e => {
    const eDate = new Date(e.date);
    const now = new Date();

    if (timeRange === 'THIS_MONTH') {
      return eDate.getMonth() === now.getMonth() && eDate.getFullYear() === now.getFullYear();
    } else if (timeRange === 'LAST_30_DAYS') {
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
      return eDate >= thirtyDaysAgo;
    }
    return true;
  });

  const totalIncome = filteredExpenses.filter(e => e.type === 'INCOME').reduce((s, e) => s + e.amount, 0);
  const totalExpense = filteredExpenses.filter(e => e.type === 'EXPENSE').reduce((s, e) => s + e.amount, 0);
  const netSavings = totalIncome - totalExpense;
  const savingsRate = totalIncome > 0 ? Math.max(0, Math.round((netSavings / totalIncome) * 100)) : 0;

  // Category Breakdown Data for Pie Chart & Bar Chart
  const categoryMap: { [cat: string]: number } = {};
  filteredExpenses
    .filter(e => e.type === 'EXPENSE')
    .forEach(e => {
      categoryMap[e.category] = (categoryMap[e.category] || 0) + e.amount;
    });

  const categoryData = Object.keys(categoryMap).map(cat => ({
    name: cat,
    amount: categoryMap[cat]
  })).sort((a, b) => b.amount - a.amount);

  // Income vs Expense Comparison Bar Chart Data
  const incomeVsExpenseData = [
    { name: 'Total Inflow', amount: totalIncome, fill: '#10B981' },
    { name: 'Total Outflow', amount: totalExpense, fill: '#EA3B35' },
    { name: 'Net Savings', amount: Math.max(0, netSavings), fill: '#3B82F6' }
  ];

  const COLOR_PALETTE = ['#EA3B35', '#6366F1', '#3B82F6', '#8B5CF6', '#F59E0B', '#10B981', '#EC4899', '#06B6D4'];

  return (
    <div className="space-y-6 pb-12">
      {/* Header Banner */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-[#EA3B35]/10 border border-[#EA3B35]/20 text-[#EA3B35]">
            <BarChart3 className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-primary-var">Financial Analytics & Insights</h2>
            <p className="text-xs text-secondary-var">Visual breakdown of category spending, savings rate, and cash flow</p>
          </div>
        </div>

        {/* Time Range Filter */}
        <div className="flex items-center space-x-2">
          <Calendar className="w-4 h-4 text-secondary-var" />
          <select
            value={timeRange}
            onChange={(e: any) => setTimeRange(e.target.value)}
            className="bg-sub-surface border border-theme rounded-xl px-3 py-2 text-xs text-primary-var focus:outline-none"
          >
            <option value="THIS_MONTH">This Month</option>
            <option value="LAST_30_DAYS">Last 30 Days</option>
            <option value="ALL_TIME">All Time</option>
          </select>
        </div>
      </div>

      {/* Top 3 KPI Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="p-5 rounded-2xl bg-card-surface border border-theme flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
            <ArrowUpRight className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-secondary-var uppercase tracking-wide">Total Income</span>
            <div className="text-2xl font-extrabold text-emerald-500">{formatAmount(totalIncome)}</div>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-card-surface border border-theme flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-[#EA3B35]/10 text-[#EA3B35] border border-[#EA3B35]/20">
            <ArrowDownLeft className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-secondary-var uppercase tracking-wide">Total Expenses</span>
            <div className="text-2xl font-extrabold text-[#EA3B35]">{formatAmount(totalExpense)}</div>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-card-surface border border-theme flex items-center space-x-3">
          <div className="p-3 rounded-xl bg-indigo-500/10 text-indigo-500 border border-indigo-500/20">
            <PiggyBank className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-bold text-secondary-var uppercase tracking-wide">Savings Rate Ratio</span>
            <div className="text-2xl font-extrabold text-indigo-500">{savingsRate}%</div>
          </div>
        </div>
      </div>

      {/* Analytics Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chart 1: Income vs Expenses Comparison Bar Chart */}
        <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
          <div>
            <h3 className="text-base font-bold text-primary-var">Cash Flow Comparison</h3>
            <p className="text-xs text-secondary-var">Inflow vs Outflow vs Net Savings comparison</p>
          </div>

          <div className="h-64 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={incomeVsExpenseData}>
                <XAxis dataKey="name" stroke="var(--text-secondary)" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="var(--text-secondary)" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => `${currency}${v}`} />
                <Tooltip
                  contentStyle={{ backgroundColor: 'var(--card-surface)', borderColor: 'var(--border-color)', borderRadius: '12px', color: 'var(--text-primary)', fontSize: '12px' }}
                  formatter={(val: any) => [`${currency}${val}`, 'Amount']}
                />
                <Bar dataKey="amount" radius={[8, 8, 0, 0]}>
                  {incomeVsExpenseData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.fill} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Chart 2: Category Spending Distribution */}
        <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-primary-var">Category Distribution</h3>
              <p className="text-xs text-secondary-var">Spending proportion per category</p>
            </div>
            <PieChartIcon className="w-5 h-5 text-[#EA3B35]" />
          </div>

          {categoryData.length === 0 ? (
            <div className="h-64 flex items-center justify-center text-xs text-secondary-var border border-dashed border-theme rounded-xl">
              No expense data recorded for this period.
            </div>
          ) : (
            <div className="h-64 w-full flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={4}
                    dataKey="amount"
                  >
                    {categoryData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLOR_PALETTE[index % COLOR_PALETTE.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ backgroundColor: 'var(--card-surface)', borderColor: 'var(--border-color)', borderRadius: '12px', color: 'var(--text-primary)', fontSize: '12px' }}
                    formatter={(val: any) => [`${currency}${val}`, 'Spent']}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>

      {/* Category Breakdown Table */}
      <div className="p-6 rounded-2xl bg-card-surface border border-theme space-y-4">
        <h3 className="text-base font-bold text-primary-var">Detailed Category Breakdown</h3>

        {categoryData.length === 0 ? (
          <div className="py-8 text-center text-xs text-secondary-var border border-dashed border-theme rounded-xl">
            No expenses logged for this time range.
          </div>
        ) : (
          <div className="divide-y divide-theme">
            {categoryData.map((cat, idx) => {
              const percentage = totalExpense > 0 ? Math.round((cat.amount / totalExpense) * 100) : 0;
              const color = COLOR_PALETTE[idx % COLOR_PALETTE.length];

              return (
                <div key={cat.name} className="py-3 flex items-center justify-between hover:bg-sub-surface px-2 rounded-xl transition-colors">
                  <div className="flex items-center space-x-3">
                    <div className="w-3.5 h-3.5 rounded-full" style={{ backgroundColor: color }} />
                    <span className="text-sm font-semibold text-primary-var">{cat.name}</span>
                  </div>

                  <div className="flex items-center space-x-4 text-xs font-bold">
                    <span className="text-secondary-var">{percentage}% of total</span>
                    <span className="text-primary-var">{formatAmount(cat.amount)}</span>
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
