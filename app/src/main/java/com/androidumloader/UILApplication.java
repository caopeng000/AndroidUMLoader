package com.androidumloader;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

/******************************************
 * 类名称：UILApplication
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 10:41
 ******************************************/
public class UILApplication extends Application {
    @Override
    public void onCreate() {
        //添加严格模式，判断是否在主线程做耗时操作
        if (Constants.Config.DEVELOPER_MODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDialog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build());
        }

        super.onCreate();
    }
}
