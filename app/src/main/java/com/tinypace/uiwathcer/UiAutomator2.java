package com.tinypace.uiwathcer;

import android.app.UiAutomation;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class UiAutomator2 {
    private UiAutomation uiAutomation;
    private HandlerThread mHandlerThread;
    private boolean isRunning = false;
    private volatile static UiAutomator2 uiAutomator2 = null;

    public UiAutomator2() {
        connect();
    }

    public static UiAutomator2 getInstance() {
        if (uiAutomator2 == null) {
            synchronized (UiAutomator2.class) {
                if (uiAutomator2 == null) {
                    uiAutomator2 = new UiAutomator2();
                }
            }
        }
        return uiAutomator2;
    }

    public void restart() {
        try {
            disconnect();
            Thread.sleep(50);
            connect();
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        if (!isRunning) {
            Object connection;
            mHandlerThread = new HandlerThread("UiAutomationHandlerThread");
            mHandlerThread.start();
            try {
                Class<?> UiAutomationConnection = Class.forName("android.app.UiAutomationConnection");
                Constructor<?> newInstance = UiAutomationConnection.getDeclaredConstructor();
                newInstance.setAccessible(true);
                connection = newInstance.newInstance();
                Class<?> IUiAutomationConnection = Class.forName("android.app.IUiAutomationConnection");
                Constructor<?> newUiAutomation = UiAutomation.class.getDeclaredConstructor(Looper.class, IUiAutomationConnection);
                uiAutomation = (UiAutomation) newUiAutomation.newInstance(mHandlerThread.getLooper(), connection);
                Method connect = UiAutomation.class.getDeclaredMethod("connect");
                connect.invoke(uiAutomation);
                isRunning = true;
            } catch (Exception e) {
                e.printStackTrace();
                disconnect();
            }
        }
    }

    private void disconnect() {
        if (uiAutomation != null) {
            uiAutomation.setOnAccessibilityEventListener(null);
            try {
                Method disconnect = UiAutomation.class.getDeclaredMethod("disconnect");
                disconnect.invoke(uiAutomation);
            } catch (Exception e) {
                e.printStackTrace();
            }
            uiAutomation = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        isRunning = false;
    }

    public UiAutomation getUiAutomation() {
        return uiAutomation;
    }

    public void stopUiAutomator() {
        disconnect();
    }


    public AccessibilityNodeInfo[] getWindowRoots() {
        Set<AccessibilityNodeInfo> roots = new HashSet();
        AccessibilityNodeInfo activeRoot = uiAutomation.getRootInActiveWindow();
        if (activeRoot != null) {
            roots.add(activeRoot);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (AccessibilityWindowInfo window : uiAutomation.getWindows()) {
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) {
                    continue;
                }
                roots.add(root);
            }
        }
        return roots.toArray(new AccessibilityNodeInfo[roots.size()]);
    }

    public AccessibilityNodeInfo getRootNode() {
        final int maxRetry = 6;
        long waitInterval = 250;
        AccessibilityNodeInfo rootNode = null;
        for (int x = 0; x < maxRetry; x++) {
            rootNode = uiAutomation.getRootInActiveWindow();
            if (rootNode != null) {
                return rootNode;
            }
            if (x < maxRetry - 1) {
                SystemClock.sleep(waitInterval);
                waitInterval *= 2;
            }
        }
        return rootNode;
    }
}
