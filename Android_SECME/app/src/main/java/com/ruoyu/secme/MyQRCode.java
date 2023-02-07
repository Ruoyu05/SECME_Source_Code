package com.ruoyu.secme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.zxing.WriterException;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.JsonHelper;
import com.ruoyu.secme.HelperFile.QRCodeHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.MyQRCodeInfo;

public class MyQRCode extends AppCompatActivity {

    private WebSocketService mWebSocketService = null;
    private String userMd5Str = "";
    private SQLiteDatabase db = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qrcode);

        Intent intent = getIntent();
        String host = intent.getStringExtra("host");
        String port = intent.getStringExtra("port");
        String user = intent.getStringExtra("user");
        String verifyMd5 = intent.getStringExtra("verifyMd5");
        String myQRCodeStr = intent.getStringExtra("myQRCodeStr");

        TextView txtUsername = (TextView)findViewById(R.id.txtUsername);
        txtUsername.setText(user);

        TextView txtPort = (TextView)findViewById(R.id.txtPort);
        txtPort.setText(port);

        TextView txtHost = (TextView)findViewById(R.id.txtHost);
        txtHost.setText(host);

        TextView txtMd5 = (TextView)findViewById(R.id.txtMd5);
        txtMd5.setText(verifyMd5);


        try {
            ImageView imgMyQRCode = (ImageView) findViewById(R.id.imgMyQRCode);
            Bitmap bitmap = new QRCodeHelper().buildQRBitmap(myQRCodeStr);
            imgMyQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {

        }

        ImageButton imgBtnScanFriend = (ImageButton) findViewById(R.id.imgBtnScanFriend);
        imgBtnScanFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}