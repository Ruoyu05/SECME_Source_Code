package com.ruoyu.secme;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.ruoyu.secme.HelperFile.JsonHelper;
import com.ruoyu.secme.HelperFile.RSAHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.UserAccount;

public class RegisterPage extends AppCompatActivity {

    private WebSocketService mWebSocketService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        EditText edtRegisterHost = (EditText) findViewById(R.id.edtRegisterHost);
        EditText edtRegisterport = (EditText) findViewById(R.id.edtRegisterPort);

        //debug设置
        edtRegisterHost.setText("47.74.1.184");
//        edtRegisterHost.setText("10.32.1.123");
        edtRegisterport.setText("10086");

        startWebSocketService();

        //服务器连接按钮
        Button btnConServer = (Button) findViewById(R.id.btnConServer);
        btnConServer.setOnClickListener(view -> {
            switchTable();
        });
        Button btnDoRegister = (Button) findViewById(R.id.btnDoRegister);
        btnDoRegister.setOnClickListener(view -> {
            //确认注册
            //清空提示消息
            TextView txtRegisterResultMessage = (TextView) findViewById(R.id.txtRegisterResultMessage);
            txtRegisterResultMessage.setText("");

            //重置
            mWebSocketService.client.gotRegisterFailure = false;
            mWebSocketService.client.gotRegisterSuccess = false;
            mWebSocketService.client.resultMessage = "";

            EditText edtRegisterUsername = (EditText) findViewById(R.id.edtRegisterUsername);
            EditText edtRegisterPassword = (EditText) findViewById(R.id.edtRegisterPassword);
            EditText edtRepeatPassword = (EditText) findViewById(R.id.edtRepeatPassword);

            if (edtRegisterUsername.getText().toString().equals("")) {
                txtRegisterResultMessage.setText("ユーザー名を入力してください!");
            } else if (edtRegisterPassword.getText().toString().equals("")) {
                txtRegisterResultMessage.setText("パスワードを入力してください!");
            } else if (edtRepeatPassword.getText().toString().equals("")) {
                txtRegisterResultMessage.setText("パスワードが一致していません!");
            } else if (!edtRepeatPassword.getText().toString().equals(edtRegisterPassword.getText().toString())) {
                txtRegisterResultMessage.setText("パスワードが一致していません!");
            } else {
                //开始监听结果
                showLoading();
                if (mWebSocketService.client.isConnected) {

                    JsonMessage jsonMessage = new JsonMessage();
                    jsonMessage.type = "RegisterAccount";
                    UserAccount registerAccount = new UserAccount();
                    registerAccount.username = edtRegisterUsername.getText().toString();
                    registerAccount.password = edtRegisterPassword.getText().toString();
                    jsonMessage.contents = registerAccount;
                    String jsonMessageStr = JsonHelper.buildJsonMessage(jsonMessage);
                    try {
                        String encryptedJsonMessage = RSAHelper.encryptJsonMessage(jsonMessageStr, mWebSocketService.client.server_publicKeyStr);
                        mWebSocketService.client.send(encryptedJsonMessage.getBytes());
                    } catch (Exception e) {
                        Log.d("testDebug", "注册用户失败:加密失败");
                    }
                }

            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mWebSocketService != null && mWebSocketService.client.isConnected){
            //断开已有连接
            mWebSocketService.disconnect();

            //关闭table
            setLayoutHeight(180);

            //服务器状态切换为离线
            TextView txtStateOfServer = (TextView) findViewById(R.id.txtStateOfServer);
            ImageView imgServerState = (ImageView) findViewById(R.id.imgServerState);
            imgServerState.setImageResource(R.drawable.cicle_red);
            txtStateOfServer.setText("オフライン");

            //服务器按钮切换文本更换
            switchTable();

        }
    }

    private void connectServer(String host, String port) {
        mWebSocketService.setClient(host, port);
        mWebSocketService.connect();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                        if (mWebSocketService.client.isConnected()) {
                            //连接成功
                            ImageView imgServerState = (ImageView) findViewById(R.id.imgServerState);
                            TextView txtStateOfServer = (TextView) findViewById(R.id.txtStateOfServer);
                            imgServerState.setImageResource(R.drawable.cicle_green);
                            txtStateOfServer.setText("オンライン");
                            openLayoutOnUIThread(RegisterPage.this);
                            break;
                        }
                    } catch (InterruptedException e) {
                        mWebSocketService.client.close();
                        break;
                    }

                }
            }
        }).start();
    }

    //显示等待页面
    private void showLoading() {
        AlertDialog mDialog = new AlertDialog.Builder(RegisterPage.this, R.style.myWaittingDialog).setTitle("").setMessage("Connecting...").create();
        mDialog.setMessage("登録中...");
        mDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                        if (mWebSocketService.client.gotRegisterSuccess) {
                            //跳转页面
                            Intent intent = new Intent(RegisterPage.this, RegisterSuccessPage.class);
                            startActivity(intent);
                            mDialog.dismiss();
                            finish();
                            break;
                        }
                        if (mWebSocketService.client.gotRegisterFailure) {
                            TextView txtRegisterResultMessage = (TextView) findViewById(R.id.txtRegisterResultMessage);
                            txtRegisterResultMessage.setText(mWebSocketService.client.resultMessage);
                            mDialog.dismiss();
                            break;
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
    }

    private void switchTable(){
        Button btnConServer = (Button) findViewById(R.id.btnConServer);
        EditText edtRegisterHost = (EditText) findViewById(R.id.edtRegisterHost);
        EditText edtRegisterport = (EditText) findViewById(R.id.edtRegisterPort);

        TextView txtStateOfServer = (TextView) findViewById(R.id.txtStateOfServer);
        //按钮切换
        if (btnConServer.getText().equals("サーバーを切り替える")) {
            //关闭表单
            closeLayoutOnUIThread(RegisterPage.this);

            edtRegisterHost.setEnabled(true);
            edtRegisterport.setEnabled(true);
            btnConServer.setText("サーバーに接続する");
            ImageView imgServerState = (ImageView) findViewById(R.id.imgServerState);
            imgServerState.setImageResource(R.drawable.cicle_red);
            txtStateOfServer.setText("オフライン");
        } else {
            edtRegisterHost.setEnabled(false);
            edtRegisterport.setEnabled(false);
            btnConServer.setText("サーバーを切り替える");
            //连接服务器
            txtStateOfServer.setText("サーバーに接続中...");
            connectServer(edtRegisterHost.getText().toString(), edtRegisterport.getText().toString());
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

    //设置图层高度
    private void setLayoutHeight(int height_dp) {
        LinearLayout linRegisterSetting = (LinearLayout) findViewById(R.id.linRegisterSetting);
        //dp转int
        int h_int = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height_dp, getResources().getDisplayMetrics());
        ViewGroup.LayoutParams layoutParams;
        layoutParams = linRegisterSetting.getLayoutParams();
        layoutParams.height = h_int;
        linRegisterSetting.setLayoutParams(layoutParams);
    }

    private void openLayoutOnUIThread(Activity act) {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Log.d("testDebug", "执行UI修改");
                setLayoutHeight(530);
            }
        });
    }

    private void closeLayoutOnUIThread(Activity act) {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Log.d("testDebug", "执行UI修改");
                setLayoutHeight(180);
            }
        });
    }

//    public static class EmptyPage extends AppCompatActivity {
//
//        @Override
//        protected void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            setContentView(R.layout.activity_empty_page);
//        }
//    }
}