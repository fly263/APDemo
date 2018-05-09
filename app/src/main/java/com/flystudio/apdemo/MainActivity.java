package com.flystudio.apdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

/**
 * 本类仅为测试使用，为了方便copy，所以一些变量没有声明为全局的，如wifiManager
 */
public class MainActivity extends AppCompatActivity {
    private TextView tvInfo;
    private boolean isOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = (TextView) findViewById(R.id.tvInfo);
        getWifiAPConfig();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAP:
                if (isHasPermissions()) {
                    setWifiApEnabled(true);
                }
                break;
            case R.id.btnManualAP:
                openAPUI();
                break;
            case R.id.btnReadSetting:
                getWifiAPConfig();
                break;
            case R.id.btnSwitch:
                if (isHasPermissions()) {
                    isOpen = !isOpen;
                    switchWifiApEnabled(isOpen);
                }
                break;
        }
    }

    private boolean isHasPermissions() {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "打开热点需要启用“修改系统设置”权限，请手动开启", Toast.LENGTH_SHORT).show();

                //清单文件中需要android.permission.WRITE_SETTINGS，否则打开的设置页面开关是灰色的
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                //判断系统能否处理，部分ROM无此action，如魅族Flyme
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    //打开应用详情页
                    intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + this.getPackageName()));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            } else {
                result = true;
            }
        } else {
            result = true;
        }
        return result;
    }

    /**
     * 自定义wifi热点
     *
     * @param enabled 开启or关闭
     * @return
     */
    private boolean setWifiApEnabled(boolean enabled) {
        boolean result = false;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return false;
        }
        if (enabled) {
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
        }
        try {
            //热点的配置类
            //WifiConfiguration apConfig = new WifiConfiguration();
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration apConfig = (WifiConfiguration) method.invoke(wifiManager);
            //配置热点的名称
            apConfig.SSID = "ap_test";
            //配置热点的密码，至少八位
            apConfig.preSharedKey = "12345678";
            //必须指定allowedKeyManagement，否则会显示为无密码
            //指定安全性为WPA_PSK，在不支持WPA_PSK的手机上看不到密码
            //apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            //指定安全性为WPA2_PSK，（官方值为4，小米为6，如果指定为4，小米会变为无密码热点）
            int indexOfWPA2_PSK = 4;
            //从WifiConfiguration.KeyMgmt数组中查找WPA2_PSK的值
            for (int i = 0; i < WifiConfiguration.KeyMgmt.strings.length; i++) {
                if (WifiConfiguration.KeyMgmt.strings[i].equals("WPA2_PSK")) {
                    indexOfWPA2_PSK = i;
                    break;
                }
            }
            apConfig.allowedKeyManagement.set(indexOfWPA2_PSK);
            //TODO Android 8.0已废弃setWifiApEnabled方法，未测试
            //通过反射调用设置热点
            method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            //返回热点打开状态
            result = (Boolean) method.invoke(wifiManager, apConfig, enabled);
            if (!result) {
                Toast.makeText(this, "热点创建失败，请手动创建！", Toast.LENGTH_SHORT).show();
                openAPUI();
            }
        } catch (Exception e) {
            Toast.makeText(this, "热点创建失败，请手动创建！", Toast.LENGTH_SHORT).show();
            openAPUI();
        }
        return result;
    }

    /**
     * 切换状态
     *
     * @param enabled
     * @return
     */
    private boolean switchWifiApEnabled(boolean enabled) {
        boolean result = false;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return false;
        }
        if (enabled) {
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
        }
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            //读取已有热点配置信息
            WifiConfiguration apConfig = (WifiConfiguration) method.invoke(wifiManager);

            //通过反射调用设置热点
            method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            //返回热点打开状态
            result = (Boolean) method.invoke(wifiManager, apConfig, enabled);
            if (!result) {
                Toast.makeText(this, "热点创建失败，请手动创建！", Toast.LENGTH_SHORT).show();
                openAPUI();
            }
        } catch (Exception e) {
            Toast.makeText(this, "热点创建失败，请手动创建！", Toast.LENGTH_SHORT).show();
            openAPUI();
        }
        return result;
    }

    /**
     * 打开网络共享与热点设置页面
     */
    private void openAPUI() {
        Intent intent = new Intent();
        //直接打开热点设置页面（不同ROM有差异）
        ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity");
        //下面这个是打开网络共享与热点设置页面
        //ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setComponent(comp);
        startActivity(intent);
    }

    /**
     * 判断是否已打开WiFi热点
     *
     * @return
     */
    private boolean isWifiApEnabled() {
        boolean isOpen = false;
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return false;
            }
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            isOpen = (boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return isOpen;
    }

    /**
     * 读取热点配置信息
     */
    private void getWifiAPConfig() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return;
            }
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration apConfig = (WifiConfiguration) method.invoke(wifiManager);
            if (apConfig == null) {
                tvInfo.setText("未配置热点");
                return;
            }
            tvInfo.setText(String.format("热点名称：%s\r\n", apConfig.SSID));
            tvInfo.append(String.format("密码：%s\n", apConfig.preSharedKey));
            // Android 4.2.2异常，返回{}
            //使用apConfig.allowedKeyManagement.toString()返回{0}这样的格式，需要截取中间的具体数值
            //下面几种写法都可以
            //int index = Integer.valueOf(apConfig.allowedKeyManagement.toString().substring(1, 2));
            //int index = Integer.valueOf(String.valueOf(apConfig.allowedKeyManagement.toString().charAt(1)));
            //int index = Integer.valueOf(apConfig.allowedKeyManagement.toString().charAt(1)+"");
            int index = apConfig.allowedKeyManagement.toString().charAt(1) - '0';
            //从KeyMgmt数组中取出对应的文本
            String apType = WifiConfiguration.KeyMgmt.strings[index];
            tvInfo.append(String.format(Locale.getDefault(), "WifiConfiguration.KeyMgmt：%s\r\n", Arrays.toString(WifiConfiguration.KeyMgmt.strings)));
            tvInfo.append(String.format(Locale.getDefault(), "安全性：%d，%s\r\n", index, apType));
            isOpen = isWifiApEnabled();
            tvInfo.append("是否已开启：" + isOpen);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
