export {};

declare global {
  interface Window {
    electronAPI?: {
      minimizeWindow: () => void;
      maximizeWindow: () => void;
      closeWindow: () => void;
      sendNotification: (title: string, body: string) => void;
      saveFile: (
        defaultName: string,
        content: string,
        filters?: { name: string; extensions: string[] }[]
      ) => Promise<{ success: boolean; filePath?: string; cancelled?: boolean; error?: string }>;
      openFile: (
        filters?: { name: string; extensions: string[] }[]
      ) => Promise<{ success: boolean; content?: string; filePath?: string; cancelled?: boolean; error?: string }>;
      onQuickAdd: (callback: () => void) => () => void;
    };
  }
}
