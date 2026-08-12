import React, { useState, useRef, useEffect } from 'react';
import { useApp } from '../../context/AppContext';
import { gsap } from 'gsap';
import { ArrowRight, ArrowLeft, Check, ShieldCheck, Calculator, Tag, Wallet, BarChart2 } from 'lucide-react';
import type { AccountType } from '../../types';

export const OnboardingScreen: React.FC = () => {
  const { completeOnboarding } = useApp();

  const [currentSlide, setCurrentSlide] = useState<number>(0);
  const [userNameInput, setUserNameInput] = useState<string>('Captain');
  const [currencyInput, setCurrencyInput] = useState<string>('৳');

  // Wallet Setup Form State
  const [walletName, setWalletName] = useState<string>('Main Bank Account');
  const [bankName, setBankName] = useState<string>('Primary Bank');
  const [cardType, setCardType] = useState<AccountType>('bank');
  const [startingBalance, setStartingBalance] = useState<string>('5000');

  // GSAP Refs
  const slideContainerRef = useRef<HTMLDivElement>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const bgOrb1Ref = useRef<HTMLDivElement>(null);
  const bgOrb2Ref = useRef<HTMLDivElement>(null);

  // GSAP Background Animated Gradient Loop
  useEffect(() => {
    if (bgOrb1Ref.current) {
      gsap.to(bgOrb1Ref.current, {
        x: 60,
        y: -40,
        scale: 1.2,
        rotation: 45,
        duration: 7,
        repeat: -1,
        yoyo: true,
        ease: 'sine.easeInOut'
      });
    }

    if (bgOrb2Ref.current) {
      gsap.to(bgOrb2Ref.current, {
        x: -70,
        y: 50,
        scale: 1.15,
        rotation: -35,
        duration: 9,
        repeat: -1,
        yoyo: true,
        ease: 'sine.easeInOut'
      });
    }
  }, []);

  // Smooth GSAP Entrance on Slide Change
  useEffect(() => {
    if (slideContainerRef.current) {
      gsap.fromTo(
        slideContainerRef.current,
        { opacity: 0, y: 12 },
        { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' }
      );
    }

    if (titleRef.current) {
      gsap.fromTo(
        titleRef.current,
        { opacity: 0, y: -8 },
        { opacity: 1, y: 0, duration: 0.4, delay: 0.05, ease: 'power2.out' }
      );
    }

    if (cardRef.current) {
      gsap.fromTo(
        cardRef.current,
        { opacity: 0, y: 12 },
        { opacity: 1, y: 0, duration: 0.4, delay: 0.1, ease: 'power2.out' }
      );
    }
  }, [currentSlide]);

  const handleNext = () => {
    if (currentSlide < 3) {
      setCurrentSlide(prev => prev + 1);
    } else {
      handleFinish();
    }
  };

  const handleBack = () => {
    if (currentSlide > 0) {
      setCurrentSlide(prev => prev - 1);
    }
  };

  const handleFinish = () => {
    completeOnboarding(
      userNameInput.trim() || 'User',
      currencyInput,
      {
        name: walletName.trim() || 'Main Account',
        bankName: bankName.trim() || 'Primary Bank',
        cardType,
        startingBalance: parseFloat(startingBalance) || 0
      }
    );
  };

  const currencyOptions = [
    { code: '৳', label: '৳ BDT (Taka)' },
    { code: '$', label: '$ USD (Dollar)' },
    { code: '€', label: '€ EUR (Euro)' },
    { code: '£', label: '£ GBP (Pound)' },
    { code: '₹', label: '₹ INR (Rupee)' }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#09090B] text-white p-6 overflow-hidden select-none">
      {/* Animated Gradient Mesh Orbs */}
      <div
        ref={bgOrb1Ref}
        className="absolute top-1/4 -left-20 w-96 h-96 rounded-full bg-gradient-to-br from-[#EA3B35]/35 via-rose-600/20 to-amber-500/10 blur-[100px] pointer-events-none"
      />
      <div
        ref={bgOrb2Ref}
        className="absolute bottom-1/4 -right-20 w-[450px] h-[450px] rounded-full bg-gradient-to-tl from-indigo-600/25 via-emerald-500/15 to-transparent blur-[120px] pointer-events-none"
      />

      {/* Subtle Dot Matrix Overlay */}
      <div className="absolute inset-0 bg-[radial-gradient(#ffffff0a_1px,transparent_1px)] [background-size:24px_24px] pointer-events-none" />

      {/* Top Subtle Border Accent */}
      <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-transparent via-[#EA3B35]/80 to-transparent" />

      <div className="w-full max-w-xl relative z-10 space-y-6">
        {/* Top Header with App Logo */}
        <div className="flex items-center justify-between pb-2 border-b border-white/10">
          <div className="flex items-center space-x-3">
            <img
              src="/logo.png"
              alt="Expense Tracker Logo"
              className="w-8 h-8 rounded-xl object-contain shadow-md"
            />
            <div>
              <span className="text-sm font-bold text-white tracking-tight">Expense Tracker</span>
              <span className="text-[10px] text-zinc-400 block leading-none">Desktop Edition</span>
            </div>
          </div>

          <button
            onClick={handleFinish}
            className="text-xs text-zinc-400 hover:text-white transition-colors cursor-pointer font-medium"
          >
            Skip Setup
          </button>
        </div>

        {/* Glassmorphic Slide Card Container */}
        <div
          ref={slideContainerRef}
          className="p-8 rounded-2xl bg-zinc-900/80 border border-white/10 backdrop-blur-2xl shadow-2xl space-y-6 min-h-[430px] flex flex-col justify-between"
        >
          {/* STEP 1: Personalization */}
          {currentSlide === 0 && (
            <div className="space-y-6">
              <div className="space-y-1.5">
                <span className="text-xs font-semibold text-[#EA3B35] tracking-wide uppercase">
                  Step 1 of 4 • Profile Personalization
                </span>
                <h1 ref={titleRef} className="text-2xl font-bold text-white tracking-tight">
                  Welcome to Expense Tracker
                </h1>
                <p className="text-xs text-zinc-400">
                  Set up your profile display name and primary currency preferences.
                </p>
              </div>

              <div ref={cardRef} className="space-y-5 pt-1">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-zinc-300">Account Display Name</label>
                  <input
                    type="text"
                    value={userNameInput}
                    onChange={(e) => setUserNameInput(e.target.value)}
                    placeholder="Enter your name"
                    className="w-full bg-zinc-800/80 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-[#EA3B35] transition-colors"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-semibold text-zinc-300">Primary Currency Symbol</label>
                  <div className="grid grid-cols-3 gap-2.5">
                    {currencyOptions.map(opt => (
                      <button
                        type="button"
                        key={opt.code}
                        onClick={() => setCurrencyInput(opt.code)}
                        className={`py-2.5 px-3 rounded-xl border text-xs font-semibold transition-all cursor-pointer ${
                          currencyInput === opt.code
                            ? 'bg-[#EA3B35] border-[#EA3B35] text-white shadow-md'
                            : 'bg-zinc-800/80 border-white/10 text-zinc-400 hover:text-white'
                        }`}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* STEP 2: Smart Financial Tracking */}
          {currentSlide === 1 && (
            <div className="space-y-6">
              <div className="space-y-1.5">
                <span className="text-xs font-semibold text-[#EA3B35] tracking-wide uppercase">
                  Step 2 of 4 • Financial Tools
                </span>
                <h1 ref={titleRef} className="text-2xl font-bold text-white tracking-tight">
                  Built for Financial Clarity
                </h1>
                <p className="text-xs text-zinc-400">
                  Real-time cash flow monitoring, custom category tags, and smart math expression inputs.
                </p>
              </div>

              <div ref={cardRef} className="grid grid-cols-2 gap-3 pt-1">
                <div className="p-4 rounded-xl bg-zinc-800/60 border border-white/10 space-y-1.5">
                  <div className="flex items-center space-x-2 text-[#EA3B35]">
                    <Calculator className="w-4 h-4" />
                    <span className="text-xs font-bold text-white">Inline Math Evaluator</span>
                  </div>
                  <p className="text-[11px] text-zinc-400 leading-relaxed">
                    Evaluate arithmetic expressions directly in transaction inputs (e.g. <span className="font-mono text-white">150 + 45 * 1.05</span>).
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-zinc-800/60 border border-white/10 space-y-1.5">
                  <div className="flex items-center space-x-2 text-indigo-400">
                    <Tag className="w-4 h-4" />
                    <span className="text-xs font-bold text-white">Custom Categories</span>
                  </div>
                  <p className="text-[11px] text-zinc-400 leading-relaxed">
                    Define custom category tags tailored to your lifestyle and budgeting goals.
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-zinc-800/60 border border-white/10 space-y-1.5">
                  <div className="flex items-center space-x-2 text-emerald-400">
                    <BarChart2 className="w-4 h-4" />
                    <span className="text-xs font-bold text-white">Visual Analytics</span>
                  </div>
                  <p className="text-[11px] text-zinc-400 leading-relaxed">
                    Inspect cash flow distribution, savings rates, and 7-day spending trends.
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-zinc-800/60 border border-white/10 space-y-1.5">
                  <div className="flex items-center space-x-2 text-amber-400">
                    <ShieldCheck className="w-4 h-4" />
                    <span className="text-xs font-bold text-white">Action Protection</span>
                  </div>
                  <p className="text-[11px] text-zinc-400 leading-relaxed">
                    Theme-aware confirmation dialogs safeguard against accidental deletions.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* STEP 3: Security & Backup */}
          {currentSlide === 2 && (
            <div className="space-y-6">
              <div className="space-y-1.5">
                <span className="text-xs font-semibold text-[#EA3B35] tracking-wide uppercase">
                  Step 3 of 4 • Data Security & Mobility
                </span>
                <h1 ref={titleRef} className="text-2xl font-bold text-white tracking-tight">
                  Seamless Cross-Platform Data Sync
                </h1>
                <p className="text-xs text-zinc-400">
                  Export and import your complete financial history between Android and Desktop.
                </p>
              </div>

              <div ref={cardRef} className="p-5 rounded-xl bg-zinc-800/60 border border-white/10 space-y-3.5">
                <div className="flex items-start space-x-3 text-xs text-white">
                  <div className="w-5 h-5 rounded-full bg-[#EA3B35]/20 border border-[#EA3B35]/40 text-[#EA3B35] flex items-center justify-center font-bold flex-shrink-0 mt-0.5">✓</div>
                  <div>
                    <span className="font-bold">Android JSON Migration:</span> Import backup files directly from the Android app to restore wallets, logs, and debts.
                  </div>
                </div>

                <div className="flex items-start space-x-3 text-xs text-white">
                  <div className="w-5 h-5 rounded-full bg-[#EA3B35]/20 border border-[#EA3B35]/40 text-[#EA3B35] flex items-center justify-center font-bold flex-shrink-0 mt-0.5">✓</div>
                  <div>
                    <span className="font-bold">Desktop Shortcuts:</span> Access instant transaction logging anytime with global keybindings (<kbd className="px-1.5 py-0.5 rounded bg-zinc-800 border border-white/10 font-mono text-[10px]">Ctrl+Alt+E</kbd>).
                  </div>
                </div>

                <div className="flex items-start space-x-3 text-xs text-white">
                  <div className="w-5 h-5 rounded-full bg-[#EA3B35]/20 border border-[#EA3B35]/40 text-[#EA3B35] flex items-center justify-center font-bold flex-shrink-0 mt-0.5">✓</div>
                  <div>
                    <span className="font-bold">Local Security:</span> Secure your desktop dashboard with PIN lock protection.
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* STEP 4: Initial Primary Wallet */}
          {currentSlide === 3 && (
            <div className="space-y-5">
              <div className="space-y-1.5">
                <span className="text-xs font-semibold text-[#EA3B35] tracking-wide uppercase">
                  Step 4 of 4 • Primary Wallet Setup
                </span>
                <h1 ref={titleRef} className="text-2xl font-bold text-white tracking-tight">
                  Configure Your Primary Wallet
                </h1>
                <p className="text-xs text-zinc-400">
                  Set up your primary account card to start logging expenses immediately.
                </p>
              </div>

              <div ref={cardRef} className="space-y-3.5 pt-1">
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-zinc-300">Wallet Name</label>
                    <input
                      type="text"
                      value={walletName}
                      onChange={(e) => setWalletName(e.target.value)}
                      placeholder="e.g. Main Bank Account"
                      className="w-full bg-zinc-800/80 border border-white/10 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-[#EA3B35]"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-zinc-300">Bank / Institution</label>
                    <input
                      type="text"
                      value={bankName}
                      onChange={(e) => setBankName(e.target.value)}
                      placeholder="e.g. City Bank"
                      className="w-full bg-zinc-800/80 border border-white/10 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-[#EA3B35]"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-zinc-300">Account Type</label>
                    <select
                      value={cardType}
                      onChange={(e: any) => setCardType(e.target.value)}
                      className="w-full bg-zinc-800/80 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#EA3B35]"
                    >
                      <option value="bank">Bank Account</option>
                      <option value="cash">Cash Wallet</option>
                      <option value="credit">Credit Card</option>
                      <option value="savings">Savings Vault</option>
                    </select>
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs font-semibold text-zinc-300">Starting Balance ({currencyInput})</label>
                    <input
                      type="number"
                      value={startingBalance}
                      onChange={(e) => setStartingBalance(e.target.value)}
                      placeholder="0.00"
                      className="w-full bg-zinc-800/80 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-[#EA3B35]"
                    />
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Navigation Controls Bar */}
          <div className="flex items-center justify-between pt-4 border-t border-white/10">
            {/* Step Indicators */}
            <div className="flex items-center space-x-2">
              {[0, 1, 2, 3].map(idx => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setCurrentSlide(idx)}
                  className={`h-2 rounded-full transition-all cursor-pointer ${
                    currentSlide === idx ? 'w-6 bg-[#EA3B35]' : 'w-2 bg-zinc-700 hover:bg-zinc-500'
                  }`}
                />
              ))}
            </div>

            <div className="flex items-center space-x-3">
              {currentSlide > 0 && (
                <button
                  type="button"
                  onClick={handleBack}
                  className="py-2 px-4 rounded-xl bg-zinc-800/80 hover:bg-zinc-700 text-white border border-white/10 text-xs font-semibold flex items-center space-x-1 transition-colors cursor-pointer"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back</span>
                </button>
              )}

              <button
                type="button"
                onClick={handleNext}
                className="py-2 px-5 rounded-xl bg-[#EA3B35] hover:bg-[#f04b45] text-white text-xs font-bold flex items-center space-x-1.5 shadow-md shadow-[#EA3B35]/30 transition-all active:scale-95 cursor-pointer"
              >
                <span>{currentSlide === 3 ? 'Complete Setup' : 'Continue'}</span>
                {currentSlide === 3 ? <Check className="w-4 h-4" /> : <ArrowRight className="w-4 h-4" />}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
