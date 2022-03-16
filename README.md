# UiWathcer
无需安装即可实现app自动点击安装（输入密码等），目前基本支持市面上的常用手机。     
实现原理：通过反射的方式连接uiautomator
## 操作步骤：
先将编译好的 uiwatcher.apk push 到手机里，执行以下命令：  
`adb push uiwatcher.apk /data/local/tmp`  
        
然后执行以下命令即启动：  
`adb shell 'export CLASSPATH=/data/local/tmp/uiwatcher.apk; app_process /system/bin com.tinypace.uiwathcer.UiWatcher'`  
        
命令参数可选：  
**-t  运行时长（ms，默认30s）**   
**-p  输入的密码**  
**-l  执行一次or直到超时（true or false，默认false）**  
**-o  是否关闭支付保护，用于银行类（true or false）**  
        
例：`adb shell 'export CLASSPATH=/data/local/tmp/uiwatcher.apk; app_process /system/bin com.tinypace.uiwathcer.UiWatcher' -t 100000 -p password -l true`  
