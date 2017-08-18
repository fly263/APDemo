package flystudio.com.apdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAP:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(this)) {
                        Toast.makeText(this, "打开热点需要启用“修改系统设置”权限，请手动开启", Toast.LENGTH_SHORT).show();

                        //清单文件中需要android.permission.WRITE_SETTINGS，否则打开的设置页面开关是灰色的
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + this.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        setWifiApEnabled(true);
                    }
                } else {
                    setWifiApEnabled(true);
                }
                break;
            case R.id.btnManualAP:
                openAPUI();
                break;
        }
    }

    /**
     * wifi热点开关
     *
     * @param enabled 开启or关闭
     * @return
     */
    private boolean setWifiApEnabled(boolean enabled) {
        boolean result = false;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (enabled) {
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
        }
        try {
            //热点的配置类
            WifiConfiguration apConfig = new WifiConfiguration();
            //配置热点的名称
            apConfig.SSID = "ap_test";
            //配置热点的密码，至少八位
            apConfig.preSharedKey = "12345678";
            //必须指定allowedKeyManagement，否则会显示为无密码
            //指定安全性为WPA_PSK，在不支持WPA_PSK的手机上看不到密码
            //apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            //指定安全性为WPA2_PSK，（官方值为4，小米为6，如果指定为4，小米会变为无密码热点）
            apConfig.allowedKeyManagement.set(isMIUI() ? 6 : 4);
            //通过反射调用设置热点
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
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
     * 判断是否为MIUI系统，参考http://blog.csdn.net/xx326664162/article/details/52438706
     *
     * @return
     */
    public static boolean isMIUI() {
        try {
            String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
            String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
            String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));

            return prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                    || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                    || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null;
        } catch (final IOException e) {
            return false;
        }
    }


    /**
     * 打开网络共享与热点设置页面
     */
    private void openAPUI() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity");
        intent.setComponent(comp);
        startActivity(intent);
    }
}
