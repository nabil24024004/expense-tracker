import React from 'react';
import { useApp } from '../../context/AppContext';
import { AlertTriangle, Trash2, X } from 'lucide-react';

export const ConfirmDeleteModal: React.FC = () => {
  const { confirmDeleteModal, closeDeleteConfirmation } = useApp();

  if (!confirmDeleteModal) return null;

  const {
    title = 'Confirm Deletion',
    message,
    confirmText = 'Delete Permanently',
    isDanger = true,
    onConfirm
  } = confirmDeleteModal;

  const handleConfirm = () => {
    onConfirm();
    closeDeleteConfirmation();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 select-none">
      <div className="w-full max-w-md rounded-2xl bg-card-surface border border-theme p-6 space-y-5 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <div className={`w-11 h-11 rounded-2xl flex items-center justify-center ${
              isDanger ? 'bg-rose-500/10 border border-rose-500/20 text-rose-500' : 'bg-amber-500/10 border border-amber-500/20 text-amber-500'
            }`}>
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-primary-var tracking-tight">{title}</h3>
              <p className="text-xs text-secondary-var">Warning Action Required</p>
            </div>
          </div>

          <button
            onClick={closeDeleteConfirmation}
            className="p-1 rounded-lg text-secondary-var hover:text-primary-var hover:bg-sub-surface transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 rounded-xl bg-sub-surface border border-theme text-xs text-primary-var leading-relaxed">
          {message}
        </div>

        <div className="flex items-center space-x-3 pt-1">
          <button
            type="button"
            onClick={closeDeleteConfirmation}
            className="flex-1 py-2.5 rounded-xl bg-sub-surface hover:bg-theme-main border border-theme text-primary-var font-semibold text-xs transition-colors cursor-pointer"
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={handleConfirm}
            className={`flex-1 py-2.5 rounded-xl text-white font-bold text-xs flex items-center justify-center space-x-1.5 shadow-md transition-all active:scale-95 cursor-pointer ${
              isDanger
                ? 'bg-rose-600 hover:bg-rose-500 shadow-rose-600/20'
                : 'bg-[#EA3B35] hover:bg-[#f04b45] shadow-[#EA3B35]/20'
            }`}
          >
            <Trash2 className="w-4 h-4" />
            <span>{confirmText}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
