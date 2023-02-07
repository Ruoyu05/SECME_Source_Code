package com.ruoyu.secme;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;


import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.JsonHelper;
import com.ruoyu.secme.HelperFile.RSAHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.UpdateCertifyPublicKey;

import java.security.KeyPair;

public class CreateCertifyKey extends AppCompatActivity {

    private WebSocketService mWebSocketService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_certify_key);
        startWebSocketService();
        Button btnCreateCertifyKeyCancel = (Button) findViewById(R.id.btnCreateCertifyKeyCancel);
        btnCreateCertifyKeyCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取消
                Intent intent = new Intent(CreateCertifyKey.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        ImageButton imgBtnCreateKeys = (ImageButton) findViewById(R.id.imgBtnCreateKeys);
        imgBtnCreateKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    KeyPair keyPair = RSAHelper.createKeyPair(1024);
                    String newPrivateKey = RSAHelper.getStringFromKey(keyPair.getPrivate()).trim();
                    String newPublicKey = RSAHelper.getStringFromKey(keyPair.getPublic()).trim();



                    //更新本地密钥
                    String userMd5Str = mWebSocketService.client.userMd5Str;

                    DBHelper dbHelper = new DBHelper(CreateCertifyKey.this, userMd5Str);
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    dbHelper.createCertifyKey(db, newPublicKey, newPrivateKey);

                    mWebSocketService.client.setCertifyPrivateKey(newPrivateKey);

                    //上传密钥
                    UpdateCertifyPublicKey updateCertifyPublicKey = new UpdateCertifyPublicKey();
                    updateCertifyPublicKey.username = mWebSocketService.client.username;
                    updateCertifyPublicKey.password = mWebSocketService.client.getPassword();
                    //FIXME 上传过去的是JAVA原版的密钥

                    updateCertifyPublicKey.certify_publickey = newPublicKey;
                    JsonMessage jsonMessage = new JsonMessage();
                    jsonMessage.type = "UpdateCertifyPublicKey";
                    jsonMessage.contents = updateCertifyPublicKey;
                    String jsonMessageStr = JsonHelper.buildJsonMessage(jsonMessage);
                    String encryptedJsonMessageStr = RSAHelper.encryptJsonMessage(jsonMessageStr, mWebSocketService.client.server_publicKeyStr);
                    mWebSocketService.client.send(encryptedJsonMessageStr.getBytes());

                    AlertDialog mDialog = new AlertDialog.Builder(CreateCertifyKey.this, R.style.myWaittingDialog)
                            .setTitle("")
                            .setMessage("Connecting...")
                            .setNegativeButton("取消创建密钥", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mWebSocketService.client.certifyAuthCancel = true;
                                    mWebSocketService.client.close();
                                    Intent intent = new Intent(CreateCertifyKey.this, MainActivity.class);
                                    startActivity(intent);
                                }
                            })
                            .create();
                    mDialog.setMessage("正在创建密钥...");
                    mDialog.show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                Log.i("testDebug", "循环检测密钥状态");
                                try {
                                    Thread.sleep(200);
                                    if (mWebSocketService.client.certifyAuthed) {
                                        //执行跳转
                                        mDialog.setMessage("密钥创建成功...");
                                        mDialog.dismiss();
                                        Intent intent = new Intent(CreateCertifyKey.this, HomePage.class);
                                        startActivity(intent);
                                        finish();
                                        break;
                                    }
                                    if (mWebSocketService.client.certifyAuthCancel) {
                                        mDialog.setMessage("密钥创建失败！");
                                        Toast.makeText(CreateCertifyKey.this, "密钥创建失败。", Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                    break;
                                } catch (InterruptedException e) {
                                    break;
                                }
                            }
                            Log.i("testDebug", "Runnable(mDialog) break");
                        }
                    }).start();
                } catch (Exception e) {

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