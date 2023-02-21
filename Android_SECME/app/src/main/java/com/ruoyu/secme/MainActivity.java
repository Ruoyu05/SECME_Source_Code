package com.ruoyu.secme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.JsonHelper;
import com.ruoyu.secme.HelperFile.RSAHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.CertifyKeyCheck;
import com.ruoyu.secme.JsonType.JsonMessage;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private WebSocketService mWebSocketService = null;

    private static final int WRITE_PERMISSION = 0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startWebSocketService();
        requestWritePermission();

        //debug设置
        EditText edtHost = (EditText) findViewById(R.id.edtHost);
        EditText edtPort = (EditText) findViewById(R.id.edtPort);
        EditText edtUsername = (EditText) findViewById(R.id.edtUsername);
        EditText edtPassword = (EditText) findViewById(R.id.edtPassword);
        edtHost.setText("");
        edtPort.setText("");
        edtUsername.setText("");
        edtPassword.setText("");
        Log.i("testDebug", "开启service");

        //注册按钮
        Button btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //关闭已有连接
                if (mWebSocketService.client != null) {
                    mWebSocketService.client.close();
                }
                //跳转注册页面
                Intent intent = new Intent(MainActivity.this, RegisterPage.class);
                startActivity(intent);
            }
        });
        //登陆按钮
        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TextView txtErrorMessage = (TextView) findViewById(R.id.txtErrorMessage);
                txtErrorMessage.setText("");
                //开启service
                //172.20.10.11

                String host = edtHost.getText().toString();
                String portStr = edtPort.getText().toString();
                mWebSocketService.setClient(host, portStr);
                mWebSocketService.client.host = host;
                mWebSocketService.client.portStr = portStr;
                mWebSocketService.connect();

                TextView edtUsername = (TextView) findViewById(R.id.edtUsername);
                String username = edtUsername.getText().toString();
                TextView edtPassword = (TextView) findViewById(R.id.edtPassword);
                String password = edtPassword.getText().toString();


                if (username.equals("")) {
                    txtErrorMessage.setText("ユーザー名を入力してください!");
                } else if (password.equals("")) {
                    txtErrorMessage.setText("パスワードを入力してください!");
                } else {
                    mWebSocketService.client.setUserInfo(username, password);
                    mWebSocketService.client.doLogin = true;
                    mWebSocketService.client.authedFailure = false;
                    showLoading();
                }
            }
        });
    }

    //显示等待页面并执行状态检测
    private void showLoading() {
        AlertDialog mDialog = new AlertDialog.Builder(MainActivity.this, R.style.myWaittingDialog)
                .setTitle("")
                .setMessage("Connecting...")
                .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //取消连接按钮
                        if (mWebSocketService.client != null) {
                            mWebSocketService.client.close();
                        }
                    }
                })
                .create();
        mDialog.setMessage("サーバーに接続中...");
        mDialog.show();
        new Thread(new Runnable() {
            @SuppressLint("Range")
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(200);
                        if (mWebSocketService.client != null) {

                            if (mWebSocketService.client.authedFailure) {
                                TextView txtErrorMessage = (TextView) findViewById(R.id.txtErrorMessage);
                                txtErrorMessage.setText("ユーザー名またはパスワードが違います!");
                                mDialog.dismiss();
                                break;
                            }
                            if (mWebSocketService.client.isAuthed()) {
                                //创建数据库
                                String userMd5Str = mWebSocketService.client.userMd5Str;
                                DBHelper dbHelper = new DBHelper(MainActivity.this, userMd5Str);
                                SQLiteDatabase database = dbHelper.getReadableDatabase();
                                mWebSocketService.client.dbHelper = dbHelper;
                                mWebSocketService.client.setCertifyPrivateKey(dbHelper.getCertify_PrivateKey(database));
                                //插入数据用
//                            dbHelper.installValue(database);
                                try {
                                    mDialog.setMessage("鍵を検証中...");
                                    Cursor cursor = database.query("CertifyStorage", null, null, null, null, null, null);
                                    if (cursor != null) {
                                        Log.i("testDebug", "data件数:" + cursor.getCount() + "件");
                                        if (cursor.moveToNext()) {
                                            String certify_publickey = cursor.getString(cursor.getColumnIndex("certify_publickey"));
                                            String certify_privatekey = cursor.getString(cursor.getColumnIndex("certify_privatekey"));
                                            certify_privatekey = RSAHelper.getFormMateString(certify_privatekey);

                                            mWebSocketService.client.setCertifyPrivateKey(certify_privatekey);

                                            String certify_publickey_for_ios = certify_publickey;
                                            Log.i("testDebug", "publickey: " + certify_publickey);
                                            Log.i("testDebug", "privatekey: " + certify_privatekey);
                                            //验证密钥对
                                            RSAHelper rsaHelper = new RSAHelper();
//                                        rsaHelper.is_Java_PublicKey(certify_publickey);
//                                        rsaHelper.isPrivateKey(certify_privatekey);
                                            Log.i("testDebug", "Java公钥:" + rsaHelper.is_Java_PublicKey(certify_publickey));
                                            Log.i("testDebug", "IOS公钥:" + rsaHelper.is_IOS_PublicKey(certify_publickey));
                                            Log.i("testDebug", "私钥:" + rsaHelper.isPrivateKey(certify_privatekey));
                                            Log.i("testDebug", "成对:" + rsaHelper.isKeyPair(certify_publickey, certify_privatekey));
                                            if (rsaHelper.is_Java_PublicKey(certify_publickey) && rsaHelper.isPrivateKey(certify_privatekey) && rsaHelper.isKeyPair(certify_publickey, certify_privatekey)) {
                                                //密钥成对
                                                Log.i("testDebug", "密钥成对");
                                                sendCertifyKeyCheck(certify_publickey);
                                            } else {
                                                //密钥不成对
                                                Log.i("testDebug", "密钥不成对");
                                                sendCertifyKeyCheck("none");
                                            }
                                        } else {
                                            Log.i("testDebug", "无密钥对");
                                            sendCertifyKeyCheck("none");
                                        }
                                    } else {
                                        Log.i("testDebug", "无密钥对 cursor = null");
                                        sendCertifyKeyCheck("none");
                                    }
                                    cursor.close();

                                } finally {
                                    dbHelper.close();
                                }

                                while (true) {
                                    Thread.sleep(200);
                                    if (mWebSocketService.client.certifyKeyChecked || mWebSocketService.client.needCreateCertifyKey || mWebSocketService.client.needCopyCertifyKey) {
                                        mDialog.dismiss();
                                        if (mWebSocketService != null && mWebSocketService.client.certifyKeyChecked) {
                                            //从数据库调取私钥

                                            //执行跳转
                                            Intent intent = new Intent(MainActivity.this, HomePage.class);
                                            startActivity(intent);
                                            finish();
                                            break;
                                        } else if (mWebSocketService.client.needCreateCertifyKey) {
                                            //执行跳转
                                            Log.i("testDebug", "跳转CreateCertifyKey");
                                            Intent intent = new Intent(MainActivity.this, CreateCertifyKey.class);
                                            startActivity(intent);
                                            finish();
                                            break;
                                        } else if (mWebSocketService.client.needCopyCertifyKey) {
                                            Log.i("testDebug", "跳转CopyCertifyKey");
                                            Intent intent = new Intent(MainActivity.this, CopyCertifyKey.class);
                                            startActivity(intent);
                                            finish();
                                            break;
                                        }
                                    }
                                }
                                break;
                            } else if (mWebSocketService.client.isConnected) {
                                //连接成功
                                mDialog.setMessage("ユーザーを検証中...");
                            }
                        }
                    } catch (InterruptedException e) {
                        mDialog.dismiss();
                        break;
                    }
                }
            }
        }).start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_PERMISSION) {
            if (requestCode == WRITE_PERMISSION) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d(LOG_TAG, "Write Permission Failed");
//                    Toast.makeText(this, "请求授权本地存储权限!", Toast.LENGTH_SHORT).show();
//                    finish();
                }
            }
        }
    }

    private void requestWritePermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, WRITE_PERMISSION);
        }
    }

    private void sendCertifyKeyCheck(String certify_publickey) {
        JsonMessage jsonMessage = new JsonMessage();
        jsonMessage.type = "CertifyKeyCheck";
        CertifyKeyCheck certifyKeyCheck = new CertifyKeyCheck();
        certifyKeyCheck.username = mWebSocketService.client.username;
        certifyKeyCheck.certify_publickey = certify_publickey;
        jsonMessage.contents = certifyKeyCheck;
        String certifyKeyCheckStr = JsonHelper.buildJsonMessage(jsonMessage);
        try {
            String encryptJsonMessageStr = RSAHelper.encryptJsonMessage(certifyKeyCheckStr, mWebSocketService.client.server_publicKeyStr);
            mWebSocketService.client.send(encryptJsonMessageStr.getBytes());
            Log.i("testDebug", "sendCertifyKeyCheck发送");
        } catch (Exception e) {
            Log.i("testDebug", "sendCertifyKeyCheck加密失败");
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