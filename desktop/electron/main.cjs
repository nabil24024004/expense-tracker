const { app, BrowserWindow, ipcMain, dialog, Tray, Menu, globalShortcut, Notification } = require('electron');
const path = require('path');
const fs = require('fs');

let mainWindow = null;
let tray = null;

function createWindow() {
  const iconPath = path.join(__dirname, '../public/icon.png');

  mainWindow = new BrowserWindow({
    width: 1280,
    height: 850,
    minWidth: 900,
    minHeight: 650,
    frame: false,
    titleBarStyle: 'hidden',
    backgroundColor: '#0C0C0E',
    icon: fs.existsSync(iconPath) ? iconPath : undefined,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: false
    }
  });

  const isDev = process.env.NODE_ENV !== 'production' && !app.isPackaged;

  if (isDev) {
    mainWindow.loadURL('http://localhost:5173');
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function createTray() {
  try {
    const iconPath = path.join(__dirname, '../public/icon.png');

    const contextMenu = Menu.buildFromTemplate([
      {
        label: 'Expense Tracker Desktop',
        enabled: false
      },
      { type: 'separator' },
      {
        label: 'Show App',
        click: () => {
          if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.show();
            mainWindow.focus();
          } else {
            createWindow();
          }
        }
      },
      {
        label: 'Quick Add Expense (Ctrl+Alt+E)',
        click: () => {
          if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.show();
            mainWindow.focus();
            mainWindow.webContents.send('trigger-quick-add');
          }
        }
      },
      { type: 'separator' },
      {
        label: 'Quit',
        click: () => {
          app.isQuitting = true;
          app.quit();
        }
      }
    ]);

    if (fs.existsSync(iconPath)) {
      tray = new Tray(iconPath);
      tray.setToolTip('Expense Tracker');
      tray.setContextMenu(contextMenu);
    }
  } catch (e) {
    console.log('Tray initialization notice:', e.message);
  }
}

app.whenReady().then(() => {
  createWindow();
  createTray();

  try {
    globalShortcut.register('CommandOrControl+Alt+E', () => {
      if (mainWindow) {
        if (mainWindow.isMinimized()) mainWindow.restore();
        mainWindow.show();
        mainWindow.focus();
        mainWindow.webContents.send('trigger-quick-add');
      }
    });
  } catch (e) {
    console.log('Shortcut registration warning:', e.message);
  }

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('will-quit', () => {
  globalShortcut.unregisterAll();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// IPC Window Controls
ipcMain.on('window-minimize', () => {
  if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-maximize', () => {
  if (mainWindow) {
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize();
    } else {
      mainWindow.maximize();
    }
  }
});

ipcMain.on('window-close', () => {
  if (mainWindow) mainWindow.close();
});

// IPC Desktop Notifications
ipcMain.on('send-notification', (event, { title, body }) => {
  if (Notification.isSupported()) {
    new Notification({ title, body }).show();
  }
});

// IPC File System - Save File Dialog (JSON, CSV, PDF)
ipcMain.handle('save-file-dialog', async (event, { defaultName, content, filters }) => {
  if (!mainWindow) return { success: false, error: 'No active window' };
  
  const { filePath } = await dialog.showSaveDialog(mainWindow, {
    defaultPath: defaultName,
    filters: filters || [{ name: 'All Files', extensions: ['*'] }]
  });

  if (filePath) {
    try {
      fs.writeFileSync(filePath, content, typeof content === 'string' ? 'utf8' : undefined);
      return { success: true, filePath };
    } catch (err) {
      return { success: false, error: err.message };
    }
  }
  return { success: false, cancelled: true };
});

// IPC File System - Open File Dialog (JSON import)
ipcMain.handle('open-file-dialog', async (event, { filters }) => {
  if (!mainWindow) return { success: false, error: 'No active window' };
  
  const { filePaths } = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile'],
    filters: filters || [{ name: 'JSON Backup', extensions: ['json'] }]
  });

  if (filePaths && filePaths.length > 0) {
    try {
      const content = fs.readFileSync(filePaths[0], 'utf8');
      return { success: true, content, filePath: filePaths[0] };
    } catch (err) {
      return { success: false, error: err.message };
    }
  }
  return { success: false, cancelled: true };
});
