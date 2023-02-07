package com.ruoyu.secme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.JsonHelper;
import com.ruoyu.secme.HelperFile.RSAHelper;
import com.ruoyu.secme.HelperFile.TypeChangeHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.Contents;
import com.ruoyu.secme.JsonType.FriendInfo;
import com.ruoyu.secme.JsonType.Inside;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.SendFrom;
import com.ruoyu.secme.JsonType.SendTo;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class FriendQRResult extends AppCompatActivity {
    private WebSocketService mWebSocketService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_qrresult);
        startWebSocketService();

        Intent intent = getIntent();
        String host = intent.getStringExtra("host");
        String port = intent.getStringExtra("port");
        String user = intent.getStringExtra("user");
        String verifyMd5 = intent.getStringExtra("verifyMd5");
        String friend_certifyPublicKey = intent.getStringExtra("friend_certifyPublicKey");

        TextView txtQRUsername = (TextView) findViewById(R.id.txtQRUsername);
        txtQRUsername.setText(user);

        TextView txtQRHost = (TextView) findViewById(R.id.txtQRHost);
        txtQRHost.setText(host);

        TextView txtQRPort = (TextView) findViewById(R.id.txtQRPort);
        txtQRPort.setText(port);

        TextView txtQRMd5 = (TextView) findViewById(R.id.txtQRMd5);
        txtQRMd5.setText(verifyMd5);

        TextView btnAddFriendRequest = (TextView) findViewById(R.id.btnAddFriendRequest);

        btnAddFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Gson gson = new Gson();
                    EditText edtNickName = (EditText) findViewById(R.id.edtNickName);
                    String userMd5Str = mWebSocketService.client.userMd5Str;
                    DBHelper dbHelper = new DBHelper(FriendQRResult.this, userMd5Str);
                    SQLiteDatabase db = dbHelper.getReadableDatabase();

                    String certifyPublicKeyStr = dbHelper.getCertifyPublicKey(db);
                    String certifyPrivateKeyStr = dbHelper.getCertifyPrivateKey(db);

                    FriendInfo friendInfo = new FriendInfo();
                    friendInfo.friendName = user;
                    friendInfo.host = host;
                    friendInfo.port = port;
                    friendInfo.friend_certifyPublicKey = friend_certifyPublicKey;
                    friendInfo.md5Str = verifyMd5;
                    friendInfo.my_certifyPublicKey = certifyPublicKeyStr;
                    //以下
                    friendInfo.nickName = edtNickName.getText().toString();
                    FriendInfo returnFrindInfo = dbHelper.addNewFriend(db, friendInfo);

                    if (RSAHelper.is_IOS_PublicKey(returnFrindInfo.friend_certifyPublicKey)) {
                        returnFrindInfo.friend_certifyPublicKey = RSAHelper.getStringFromKey(RSAHelper.getPublicKey_from_ios(returnFrindInfo.friend_certifyPublicKey));
                    }
                    //发送申请好友报文

                    SendFrom sendFrom = new SendFrom();
                    sendFrom.username = mWebSocketService.client.username;
                    sendFrom.host = mWebSocketService.client.host;
                    sendFrom.port = mWebSocketService.client.portStr;
                    String sendFrom_jsonStr = gson.toJson(sendFrom);
                    String encrypted_sendfrom = RSAHelper.encryptJsonMessage(sendFrom_jsonStr, returnFrindInfo.friend_certifyPublicKey);

                    SendTo sendTo = new SendTo();
                    sendTo.username = returnFrindInfo.friendName;
                    sendTo.host = returnFrindInfo.host;
                    sendTo.port = returnFrindInfo.port;

                    Inside inside = new Inside();
                    inside.friend_chat_publickey = returnFrindInfo.my_chat_publickey;
                    inside.certify_publickey = returnFrindInfo.my_certifyPublicKey;
                    inside.uuid = returnFrindInfo.friend_uuid;
                    inside.applaymessage = "验证申请";
                    String inside_jsonStr = gson.toJson(inside);
                    String encrypted_inside = RSAHelper.encryptJsonMessage(inside_jsonStr, returnFrindInfo.friend_certifyPublicKey);

                    Contents contents = new Contents();
                    contents.send_from = encrypted_sendfrom;
                    contents.send_to = sendTo;
                    contents.inside = encrypted_inside;
                    String signStr = sendFrom_jsonStr + inside_jsonStr;

                    String signStrMd5 = TypeChangeHelper.getMd5(signStr);
                    Log.i("testDebug", "signStrMd5:" + signStrMd5);
                    contents.signature = RSAHelper.sign(signStrMd5, certifyPrivateKeyStr);

                    JsonMessage postSendRequest = new JsonMessage();
                    postSendRequest.type = "PostSendRequest";
                    postSendRequest.contents = contents;
                    String postSendRequestStr = JsonHelper.buildJsonMessage(postSendRequest);
                    String encrypted_postSendRequestStr = RSAHelper.encryptJsonMessage(postSendRequestStr, mWebSocketService.client.server_publicKeyStr);

                    mWebSocketService.client.send(encrypted_postSendRequestStr.getBytes());
                    Log.i("testDebug", "已经发送好友申请");

                } catch (Exception e) {
                    Log.i("testDebug", "加密PostSendRequest失败");
                }
            }
        });
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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //服务与活动断开
            mWebSocketService = null;
        }
    };
}