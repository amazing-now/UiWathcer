package com.tinypace.uiwathcer;

import android.graphics.Rect;
import android.os.Build;
import android.util.Base64;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import java.util.List;
import java.util.regex.Pattern;

public class UiWatcher {
    private static Controller controller;
    private static String packages = "com.android.packageinstaller|com.bbk.account|com.coloros.safecenter|com.aliyun.mobile.permission|com.oplus.appdetail|android";
    private static String installButton = "继续安装|重新安装|始终允许|安装|确定|允许|继续安装旧版本";
    private static String usbPackages = "com.coloros.usbselection|com.android.systemui|com.android.settings|com.vivo.daemonService";
    private static String usbClickText = "取消|关闭|.*传输文件.*";
    private static String usbFilterText = "允许.*USB.*调试.*|.*连接方式.*|传输文件|请移除遮挡物";
    private static String otherPackages = "com.hmct.updater";
    private static String otherFilterText = "稍后提醒";

    private static boolean isRunning = false, isInputPwd = false, isVirusWarn = true, isLoop = false;
    private static boolean isFirstClickInstall = false;
    private static String password = "password";
    private static boolean turnOffPayProtection = false;

    // adb push uiwatcher.apk /data/local/tmp
    // adb shell 'export CLASSPATH=/data/local/tmp/uiwatcher.apk; app_process /system/bin com.tinypace.uiwathcer.UiWatcher' -t 100000 -p password
    public static void main(String[] args) throws Exception {
        long outTime = 30 * 1000;
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("t", "time", true, "run time out (ms)");
        options.addOption("p", "password", true, "insert password");
        options.addOption("l", "loop", true, "keep loop until timeout");
        options.addOption("o", "off", true, "turn off pay protection");
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption('t')) {
            outTime = Long.parseLong(commandLine.getOptionValue('t'));
        }
        if (commandLine.hasOption('p')) {
            password = commandLine.getOptionValue('p');
        }
        if (commandLine.hasOption('l')) {
            isLoop = Boolean.parseBoolean(commandLine.getOptionValue('l'));
        }
        if (commandLine.hasOption('o')) {
            turnOffPayProtection = Boolean.parseBoolean(commandLine.getOptionValue('o'));
        }
        final String brand = Build.BRAND;
        println("===============");
        println("UiWatcher version:1.9.0");
        println("BRAND:" + brand);
        println("SDK:" + Build.VERSION.SDK_INT);
        println("RELEASE:" + Build.VERSION.RELEASE);
        println("PWD:" + password);
        println("TimeOut:" + outTime);
        println("===============");
        println("---START---");
        // 转base64
        password = Base64.encodeToString(password.getBytes(), Base64.DEFAULT);

        isRunning = true;
        controller = new Controller();
        final DisplayInfo displayInfo = ServiceManager.getInstance().getDisplayManager().getDisplayInfo();
        // 关闭支付保护
        if (turnOffPayProtection) {
            if (brand.equalsIgnoreCase("oppo")
                    || brand.equalsIgnoreCase("realme")) {
                println("开始OPPO支付保护关闭");
                controller.stopApp("com.coloros.securepay");
                controller.restartApp("com.coloros.phonemanager");
                println("启动手机管家");
                sleep(1000L);
                if (controller.getCurrentPackageName().equalsIgnoreCase("com.coloros.phonemanager")) {
                    sleep(500L);
                    if (controller.hasObject(By.text("稍后"))) {
                        controller.clickBySelector(By.text("稍后"));
                        println("处理管家更新");
                    }

                    if (controller.clickBySelector(By.text("支付保护"))) {
                        println("点击支付保护");
                        sleep(1000L);
                        List<UiObject> objects = controller.findObjects(By.res("android:id/switch_widget"));
                        if (!objects.isEmpty()) {
                            for (UiObject object : objects) {
                                if (object.isChecked()) {
                                    controller.click(object.getVisibleBounds().centerX(), object.getVisibleBounds().centerY());
                                    println("点击关闭");
                                    sleep(500L);
                                } else {
                                    println("已关闭");
                                }
                            }
                        }
                    }
                }
                controller.stopApp("com.coloros.securepay");
                controller.stopApp("com.coloros.phonemanager");
            }
            isRunning = false;
        } else {
            //处理usb弹窗
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!isFirstClickInstall) {
                        sleep(500);
                        UiObject object = controller.findObject(By.text(Pattern.compile(usbFilterText)).pkg(Pattern.compile(usbPackages)));
                        if (object != null) {
                            UiObject uiObject = controller.findObject(By.text(Pattern.compile(usbClickText)).pkg(Pattern.compile(usbPackages)));
                            if (uiObject != null) {
                                println(uiObject.getText() + "弹窗处理:" + object.getText());
                                controller.click(uiObject.getVisibleCenter().x, uiObject.getVisibleCenter().y);
                            }
                        }
                    }
                    println("usb等弹窗处理结束");
                }
            }).start();
            //处理更新弹窗
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!isFirstClickInstall) {
                        sleep(500);
                        UiObject object = controller.findObject(By.text(Pattern.compile(otherFilterText)).pkg(Pattern.compile(otherPackages)));
                        if (object != null) {
                            println("更新弹窗处理:" + object.getText());
                            controller.click(object.getVisibleCenter().x, object.getVisibleCenter().y);
                        }
                    }
                    println("更新弹窗处理结束");
                }
            }).start();

            if (brand.equalsIgnoreCase("oppo")
                    || brand.equalsIgnoreCase("realme")
                    || brand.equalsIgnoreCase("oneplus")
                    ) {
                //oppo输入密码
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRunning) {
                            UiObject object = controller.findObject(By.res("com.coloros.safecenter:id/et_login_passwd_edit")
                                    .clazz("android.widget.EditText").pkg(Pattern.compile(packages)));
                            if (object == null) {
                                object = controller.findObject(By.res("com.oplus.safecenter:id/et_login_passwd_edit")
                                        .clazz("android.widget.EditText").pkg(Pattern.compile(packages)));
                            }
                            if (object != null) {
                                println("input password");
                                isInputPwd = true;
                                sleep(1000);
                                controller.sendSplitText(password);
                                sleep(1000);
                                isInputPwd = false;
                                isFirstClickInstall = true;
                                break;
                            }
                        }
                        println("input password end");
                    }
                }).start();
                //oppo无视风险安装
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRunning && isVirusWarn) {
                            sleep(500);
                            UiObject object = controller.findObject(By.res("com.android.packageinstaller:id/virus_warning")
                                    .pkg(Pattern.compile(packages)));
                            if (object == null) {
                                object = controller.findObject(By.textContains("无视风险安装")
                                        .pkg(Pattern.compile(packages)));
                            }
                            if (object != null) {
                                println("find virus_warning");
                                int x = object.getVisibleCenter().x + (object.getVisibleBounds().width() / 3);
                                int y = object.getVisibleCenter().y;
                                controller.click(x, y);
                                isFirstClickInstall = true;
                                break;
                            }
                        }
                        println("find virus_warning end");
                    }
                }).start();
                if (brand.equalsIgnoreCase("oneplus")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (isRunning) {
                                sleep(500);
                                UiObject object = controller.findObject(By.res("com.android.packageinstaller:id/btn_continue_install_old").pkg("com.android.packageinstaller"));
                                if (object == null) {
                                    object = controller.findObject(By.res("com.android.packageinstaller:id/oppo_permission_list").pkg("com.android.packageinstaller"));
                                    if (object != null) {
                                        int x = displayInfo.getWidth() / 2;
                                        int y = (displayInfo.getHeight() - object.getVisibleBounds().bottom) / 3 + object.getVisibleBounds().bottom;
                                        println("oppo_permission_list click bottom");
                                        controller.click(x, y);
                                        isFirstClickInstall = true;
                                    }
                                } else {
                                    Rect rect = object.getVisibleBounds();
                                    controller.click(rect.centerX(), rect.centerY());
                                    isFirstClickInstall = true;
                                    println("install old");
                                }
                            }
                        }
                    }).start();
                }
                //oppo根据权限框定位安装控件
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRunning) {
                            sleep(500);
                            UiObject object = controller.findObject(By.text("退出安装").pkg(Pattern.compile(packages)));
                            if (object == null) {
                                object = controller.findObject(By.text(Pattern.compile(".*软件商店安装")).pkg(Pattern.compile(packages)));
                                if (object == null) {
                                    object = controller.findObject(By.res("com.android.packageinstaller:id/oppo_permission_list")
                                            .pkg(Pattern.compile(packages)));
                                    if (object == null) {
                                        object = controller.findObject(By.res("com.android.packageinstaller:id/permission_list")
                                                .pkg(Pattern.compile(packages)));
                                    }
                                    if (object != null) {
                                        println(object.getResourceName() + " click");
                                        isVirusWarn = false;
                                        int height;
                                        if (displayInfo.getHeight() <= 1920) {
                                            height = 265;
                                        } else {
                                            height = 345;
                                        }
                                        if (displayInfo.getHeight() - object.getVisibleBounds().bottom < height) {
                                            int width = object.getVisibleBounds().width();
                                            int x = width - (width / 4);
                                            int y = (displayInfo.getHeight() - object.getVisibleBounds().bottom) / 2 + object.getVisibleBounds().bottom;
                                            println("oppo_permission_list click right");
                                            controller.click(x, y);
                                        } else {
                                            int x = displayInfo.getWidth() / 2;
                                            int y = (displayInfo.getHeight() - object.getVisibleBounds().bottom) / 3 + object.getVisibleBounds().bottom;
                                            println("oppo_permission_list click top");
                                            controller.click(x, y);
                                        }
                                        isFirstClickInstall = true;
                                        println("oppo_permission_list end");
                                    }
                                } else {
                                    println("btn_app_store_safe click");
                                    isVirusWarn = false;
                                    Rect rect = object.getVisibleBounds();
                                    int y = rect.centerY() + rect.height();
                                    controller.click(rect.centerX(), y);
                                    isFirstClickInstall = true;
                                    println("btn_app_store_safe end");
                                }
                            } else {
                                println("bottom_button_layout click");
                                isVirusWarn = false;
                                Rect rect = object.getVisibleBounds();
                                int y;
                                if (controller.hasObject(By.res("com.android.packageinstaller:id/new_risk_button_layout"))) {
                                    y = rect.centerY() + rect.height();
                                    println("bottom_button_layout below");
                                } else {
                                    y = rect.centerY() - rect.height();
                                    println("bottom_button_layout above");
                                }
                                controller.click(rect.centerX(), y);
                                isFirstClickInstall = true;
                                println("bottom_button_layout end");
                            }
                        }
                    }
                }).start();
            } else if (brand.equalsIgnoreCase("vivo")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRunning) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                UiObject object = controller.findObject(By.text("忽略风险"));
                                if (object != null) {
                                    controller.click(object.getVisibleCenter().x, object.getVisibleCenter().y);
                                    println("find 忽略风险");
                                }
                            }

                            UiObject object = controller.findObject(By.textStartsWith("请输入vivo")
                                    .clazz("android.widget.EditText").pkg(Pattern.compile(packages)));
                            if (object != null) {
                                sleep(1000);
                                println("input password");
                                isInputPwd = true;
                                sleep(1000);
                                controller.sendSplitText(password);
                                sleep(1000);
                                isInputPwd = false;
                                break;
                            }
                        }
                        println("input password end");
                    }
                }).start();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRunning) {
                        if (!isInputPwd) {
                            List<UiObject> objects;
                            if (brand.equalsIgnoreCase("oppo")
                                    || brand.equalsIgnoreCase("realme")) {
                                objects = controller.findObjects(By.text(Pattern.compile(installButton)).pkg(Pattern.compile(packages)).enabled(true));
                            } else {
                                objects = controller.findObjects(By.text(Pattern.compile(installButton)).pkg(Pattern.compile(packages)));
                            }

                            if (objects.size() == 1) {
                                UiObject object = objects.get(0);
                                println("find " + object.getText());
                                controller.click(object.getVisibleCenter().x, object.getVisibleCenter().y);
                                isFirstClickInstall = true;
                            } else {
                                UiObject object = controller.findObject(By.text("完成").pkg(Pattern.compile(packages)));
                                if (object != null) {
                                    println("find 完成");
                                    controller.click(object.getVisibleCenter().x, object.getVisibleCenter().y);
//                                isInputPwdFinish = false;
//                                isVirusWarn = true;
                                    if (!isLoop) {
                                        isRunning = false;
                                    }
                                    break;
                                }
                            }
                        }
                        sleep(500);
                    }
                }
            }).start();
        }
        long startTime = System.currentTimeMillis();
        while (isRunning && System.currentTimeMillis() - startTime < outTime) {
            Thread.sleep(1000);
        }
        UiAutomator2.getInstance().stopUiAutomator();
        println("---STOP---");
        System.exit(0);
    }

    private static void println(Object text) {
        System.out.println(String.valueOf(text));
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
