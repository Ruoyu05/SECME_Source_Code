package com.ruoyu.secme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class RegisterSuccessPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authed_success);

        Button btnGoLogin = (Button)findViewById(R.id.btnGoLogin);
        btnGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转注册页面
                Intent intent = new Intent(RegisterSuccessPage.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}