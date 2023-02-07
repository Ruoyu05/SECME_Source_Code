package com.ruoyu.secme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;

public class FriendDetail extends AppCompatActivity {
    private WebSocketService mWebSocketService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_detail);
        startWebSocketService();
        Intent intent = getIntent();

        String friendname = intent.getStringExtra("friendname");
        String friendhost = intent.getStringExtra("friendhost");
        String friendport = intent.getStringExtra("friendport");
        String md5Str = intent.getStringExtra("md5Str");

        TextView txtFriendDetailName = (TextView) findViewById(R.id.txtFriendDetailName);
        txtFriendDetailName.setText(friendname);

        TextView txtFriendDetailHost = (TextView) findViewById(R.id.txtFriendDetailHost);
        txtFriendDetailHost.setText(friendhost);

        TextView txtFriendDetailPort = (TextView) findViewById(R.id.txtFriendDetailPort);
        txtFriendDetailPort.setText(friendport);

        Button btnGoChat = (Button) findViewById(R.id.btnGoChat);

        if (mWebSocketService == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (mWebSocketService != null) {
                            SQLiteDatabase db = mWebSocketService.client.dbHelper.getReadableDatabase();

                            if (DBHelper.isFriendActived(db, md5Str)) {
                                btnGoChat.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent goChatIntent = new Intent(FriendDetail.this, ChatRoom.class);
                                        goChatIntent.putExtra("md5Str", md5Str);
                                        goChatIntent.putExtra("friend_name", friendname);
                                        goChatIntent.putExtra("friend_uuid", DBHelper.getFriendInfo(db,md5Str).friend_uuid);
                                        goChatIntent.putExtra("userMd5Str", mWebSocketService.client.userMd5Str);
                                        startActivity(goChatIntent);
                                    }
                                });
                            } else {
                                btnGoChat.setClickable(false);
                            }
                            break;
                        }
                    }

                }
            }).start();
        }


    }

    private void startWebSocketService() {
        //联系后台服务
        Intent bindIntent = new Intent(this, WebSocketService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //服务与活动成功绑定
            mWebSocketService = ((WebSocketService.JWebSocketClientBinder) iBinder).getService();
            Log.i("testDebug", "WebSocket服务与Application成功绑定");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //服务与活动断开
            mWebSocketService = null;
            Log.i("testDebug", "WebSocket服务与Application成功断开: ");
        }
    };


}