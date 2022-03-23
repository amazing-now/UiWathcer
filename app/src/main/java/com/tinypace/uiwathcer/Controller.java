package com.tinypace.uiwathcer;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.UiAutomation;
import android.content.Context;
import android.graphics.Point;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    private long mDownTime;
    private Point phoneSize;
    private UiAutomation uiAutomation;
    private AccessibilityServiceInfo serviceInfo;
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 5;
    private Context context;
    private final KeyCharacterMap mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

    public Controller() {
        initData();
        CheckScreen();
    }

    private void initData() {
        uiAutomation = UiAutomator2.getInstance().getUiAutomation();
        DisplayInfo displayInfo = ServiceManager.getInstance().getDisplayManager().getDisplayInfo();
        phoneSize = new Point(displayInfo.getWidth(), displayInfo.getHeight());
        serviceInfo = uiAutomation.getServiceInfo();
        if (serviceInfo != null) {
            serviceInfo.flags = AccessibilityServiceInfo.DEFAULT |
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;
        }

    }

    private void getSystemContext(){
        Looper.prepareMainLooper();
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Constructor<?> activityThreadConstructor = activityThreadClass.getDeclaredConstructor();
            activityThreadConstructor.setAccessible(true);
            Object activityThread = activityThreadConstructor.newInstance();
            Method getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext");
            context = (Context) getSystemContextMethod.invoke(activityThread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void keepScreenOn() {
        getSystemContext();

        // 获取电源管理器对象
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "bright");
        // 点亮屏
        wakeLock.acquire(12 * 60 * 60 * 1000);
    }

    public String getCurrentPackageName() {
        AccessibilityNodeInfo rootNode = UiAutomator2.getInstance().getRootNode();
        if (rootNode == null) {
            return null;
        }
        return rootNode.getPackageName() != null ? rootNode.getPackageName().toString() : null;
    }

    public void restartApp(String packageName) {
        stopApp(packageName);
        launchApp(packageName);
    }

    public boolean launchApp(String packageName) {
        String cmd = "monkey -p " + packageName + " 1";
        boolean isSuccess = ShellUtils.fastExecCmd(cmd);
        sleep(1000);
        String activity = getActivityName();
        while (!activity.contains(packageName)) {
            isSuccess = ShellUtils.fastExecCmd(cmd);
            sleep(1000);
            activity = getActivityName();
        }
        return isSuccess;
    }

    public boolean stopApp(String packageName) {
        String cmd = "am force-stop " + packageName;
        boolean isSuccess = ShellUtils.fastExecCmd(cmd);
        sleep(1000);
        return isSuccess;
    }

    public String getActivityName() {
        String activityName = "";
        String cmd = "dumpsys window displays";
        String contain = "mCurrentFocus=";
        String result = ShellUtils.execCmd(cmd, contain);
        if (!TextUtils.isEmpty(result)) {
            String[] s = result.split(" ");
            if (s.length > 4) {
                activityName = s[4].replace("}", "");
            }
        }
        return activityName;
    }

    public boolean click(int x, int y) {
        if (x < 0 || y < 0) {
            return false;
        }
        touchDown(x, y);
        SystemClock.sleep(100);
        return touchUp(x, y);
    }

    public boolean clickBySelector(BySelector selector) {
        UiObject object = findObject(selector);
        if (object != null) {
            Point point = object.getVisibleCenter();
            if (point != null) {
                return click(point.x, point.y);
            }
        }
        return false;
    }

    public boolean sendSplitText(String text) {
        text = new String(Base64.decode(text, Base64.DEFAULT));
        KeyEvent[] events = mKeyCharacterMap.getEvents(text.toCharArray());
        if (events != null) {
            long keyDelay = 0;
            for (KeyEvent event2 : events) {
                KeyEvent event = KeyEvent.changeTimeRepeat(event2, SystemClock.uptimeMillis(), 0);
                if (!injectEventSync(event)) {
                    return false;
                }
                SystemClock.sleep(keyDelay);
            }
        }
        return true;
    }

    public boolean hasObject(BySelector selector) {
        AccessibilityNodeInfo node = ByMatcher.findMatch(selector, UiAutomator2.getInstance().getWindowRoots());
        if (node != null) {
            node.recycle();
            return true;
        }
        return false;
    }

    public UiObject findObject(BySelector selector) {
        setServiceToTouchMode(true);
        AccessibilityNodeInfo node = ByMatcher.findMatch(selector, UiAutomator2.getInstance().getWindowRoots());
        setServiceToTouchMode(false);
        return node != null ? new UiObject(node, phoneSize) : null;
    }

    public List<UiObject> findObjects(BySelector selector) {
        setServiceToTouchMode(true);
        List<UiObject> ret = new ArrayList<UiObject>();
        for (AccessibilityNodeInfo node : ByMatcher.findMatches(selector, UiAutomator2.getInstance().getWindowRoots())) {
            ret.add(new UiObject(node, phoneSize));
        }
        setServiceToTouchMode(false);
        return ret;
    }

    private void setServiceToTouchMode(boolean enable) {
        if (serviceInfo == null) {
            return;
        }
        if (enable) {
            serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        } else {
            serviceInfo.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        }
        uiAutomation.setServiceInfo(serviceInfo);
    }

    /**
     * 检测屏幕
     */
    public void CheckScreen() {
        int width = phoneSize.x;
        int height = phoneSize.y;
        int count = 1;
        int maxCount = 8;
        if (isScreenOff()) {
            println("检测到熄屏,开始点亮");
            turnScreenOn();
            keepScreenOn();
            println("保持屏幕常亮");
        }
        while (isStatusBarKeyguard() && count < maxCount) {
            println("检测到锁屏,开始解锁");
            switch (count % 2) {
                case 0:
                    println("尝试左滑解锁");
                    swipe(width * 7 / 8, height / 2, width / 8, height / 2, 10);
                    break;
                case 1:
                    println("尝试上滑解锁");
                    swipe(width / 2, height * 7 / 8, width / 2, height / 8, 10);
                    break;
            }
            sleep(1000);
            count++;
        }
        if (count < maxCount) {
            println("已解锁");
        } else {
            println("解锁失败");
        }
    }


    public boolean touchDown(float x, float y) {
        mDownTime = SystemClock.uptimeMillis();
        MotionEvent event = getMotionEvent(mDownTime, mDownTime, MotionEvent.ACTION_DOWN, x, y);
        return injectEventSync(event);
    }

    public boolean touchUp(float x, float y) {
        final long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = getMotionEvent(mDownTime, eventTime, MotionEvent.ACTION_UP, x, y);
        mDownTime = 0;
        return injectEventSync(event);
    }

    public boolean touchMove(float x, float y) {
        final long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = getMotionEvent(mDownTime, eventTime, MotionEvent.ACTION_MOVE, x, y);
        return injectEventSync(event);
    }

    public boolean swipe(int downX, int downY, int upX, int upY, int steps) {
        boolean ret = false;
        int swipeSteps = steps;
        double xStep = 0;
        double yStep = 0;
        if (swipeSteps == 0) {
            swipeSteps = 1;
        }
        xStep = ((double) (upX - downX)) / swipeSteps;
        yStep = ((double) (upY - downY)) / swipeSteps;
        ret = touchDown(downX, downY);
        for (int i = 1; i < swipeSteps; i++) {
            ret &= touchMove(downX + (int) (xStep * i), downY + (int) (yStep * i));
            if (!ret) {
                break;
            }
            SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);
        }
        ret &= touchUp(upX, upY);
        return (ret);
    }

    private MotionEvent getMotionEvent(long downTime, long eventTime, int action, float x, float y) {
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.pressure = 1;
        coords.size = 1;
        coords.x = x;
        coords.y = y;

        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, 1,
                new MotionEvent.PointerProperties[]{properties}, new MotionEvent.PointerCoords[]{coords},
                0, 0, 1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        return event;
    }

    private boolean injectEventSync(InputEvent event) {
        return uiAutomation.injectInputEvent(event, true);
    }

    private boolean injectKeycode(int keyCode) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0) && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0);
    }

    private boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectEvent(event);
    }

    private boolean injectEvent(InputEvent event) {
        return ServiceManager.getInstance().getInputManager().injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private boolean turnScreenOn() {
        return injectKeycode(KeyEvent.KEYCODE_POWER);
    }

    private boolean isScreenOff() {
        String cmd = "dumpsys window policy";
        String[] contains = {"mAwake=", "screenState="};
        String result = ShellUtils.execCmd(cmd, contains);
        if (!TextUtils.isEmpty(result)) {
            if (result.contains(contains[0])) {
                result = result.split("mS")[0].trim();
                result = result.split(contains[0])[1];
                return "false".equals(result);
            } else {
                result = result.replace("\n", "");
                result = result.split(contains[1])[1];
                return "SCREEN_STATE_OFF".equals(result) || "0".equals(result);
            }
        }
        return false;
    }

    private boolean isStatusBarKeyguard() {
        boolean isShowing = false;
        String cmd = "dumpsys window policy";
        String[] contains = {"showing=", "mShowingLockscreen="};
        String result = ShellUtils.execCmd(cmd, contains);
        if (!TextUtils.isEmpty(result)) {
            for (String s : contains) {
                if (result.contains(s)) {
                    result = result.replace("\n", "");
                    result = result.split(s)[1];
                    isShowing = Boolean.parseBoolean(result.split(" ")[0]);
                    break;
                }
            }
        }
        return isShowing;
    }

    private void println(Object text) {
        System.out.println(String.valueOf(text));
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public UiAutomation getUiAutomation() {
        return uiAutomation;
    }
}
