package com.ruoyu.secme.HelperFile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ruoyu.secme.MainActivity;

import java.net.URI;

public class WebSocketService extends Service {
    public WebSocketHelper client = null;
    private final static int GRAY_SERVICE_ID = 1001;
    private final static String CHANNEL_ID = "1";
    private Notification notification;
    private JWebSocketClientBinder mBinder = new JWebSocketClientBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //用于Activity和service通讯
    public class JWebSocketClientBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("testDebug", "建立WebSocketService服务");
//        Log.d("testDebug", "启动notification");
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "name", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext(), CHANNEL_ID);
        notification = builder.build();
        startForeground(1, notification);
    }

    //灰色保活
    public static class GrayInnerService extends Service {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d("testDebug", "onStartCommand()");
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, notification);

        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //Android4.3以下 ，隐藏Notification上的图标
            Log.d("testDebug", "Android4.3以下");
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            //Android4.3 - Android8.0，隐藏Notification上的图标
            Log.d("testDebug", "Android4.3 - Android8.0");
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
//            Log.d("testDebug", "Android8.0以上");
            Intent nfIntent = new Intent(this, MainActivity.class);
            //启动后台服务
            startForegroundService(nfIntent);
        }
        return START_STICKY;
    }

    public class AuxiliaryService extends Service {

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public void setClient(String host, String port) {
//        String url = "ws://192.168.100.26:10086";    //协议标识符是ws
        String url = "ws://" + host + ":" + port;
        URI uri = URI.create(url);
        client = new WebSocketHelper(uri);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.close();
    }

    public void disconnect() {
        client.close();
    }

    public void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    client.connectBlocking();
                } catch (InterruptedException e) {
//                    Log.d("testDebug", "client.connectBlocking() catch");
                    e.printStackTrace();
                }
            }
        }.start();
    }

}
