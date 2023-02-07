package com.ruoyu.secme;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.databinding.ActivityHomePageBinding;

public class HomePage extends AppCompatActivity {

    private ActivityHomePageBinding binding;
    private WebSocketService mWebSocketService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startWebSocketService();
        checkOnline();


        binding = ActivityHomePageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_chatlists, R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home_page);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

    }

    public void checkOnline(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
//                        Log.i("testDebug", "check on line");
                        Thread.sleep(1000);
                        if(mWebSocketService.client.isClosed()){
                            Intent intent = new Intent(HomePage.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            break;
                        }
                    }catch (Exception e){
                        break;
                    }
                }
            }
        }).start();
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