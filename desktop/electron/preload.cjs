const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  minimizeWindow: () => ipcRenderer.send('window-minimize'),
  maximizeWindow: () => ipcRenderer.send('window-maximize'),
  closeWindow: () => ipcRenderer.send('window-close'),
  sendNotification: (title, body) => ipcRenderer.send('send-notification', { title, body }),
  saveFile: (defaultName, content, filters) => ipcRenderer.invoke('save-file-dialog', { defaultName, content, filters }),
  openFile: (filters) => ipcRenderer.invoke('open-file-dialog', { filters }),
  onQuickAdd: (callback) => {
    const subscription = (event) => callback();
    ipcRenderer.on('trigger-quick-add', subscription);
    return () => ipcRenderer.removeListener('trigger-quick-add', subscription);
  }
});
