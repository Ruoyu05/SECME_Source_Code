package com.ruoyu.secme.ui.home;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.WriterException;
import com.ruoyu.secme.CreateCertifyKey;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.QRCodeHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private WebSocketService mWebSocketService = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        startWebSocketService();

        //生成PublicKey QR
        binding.btnShowPublicKey.setOnClickListener(v -> {
            String userMd5Str = mWebSocketService.client.userMd5Str;
            DBHelper dbHelper = new DBHelper(getActivity(), userMd5Str);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String publicKeyStr = dbHelper.getCertifyPublicKey(db);

            try {
                Bitmap bitmap = new QRCodeHelper().buildQRBitmap(publicKeyStr);
                binding.imgQRCode.setImageBitmap(bitmap);
            } catch (WriterException e) {
                Log.i("testDebug", "PublicKey QR生成失败");
            }
        });
        //生成PrivateKey QR
        binding.btnShowPrivateKey.setOnClickListener(v -> {
            String userMd5Str = mWebSocketService.client.userMd5Str;
            DBHelper dbHelper = new DBHelper(getActivity(), userMd5Str);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String privateKey = dbHelper.getCertifyPrivateKey(db);
            try {
                Bitmap bitmap = new QRCodeHelper().buildQRBitmap(privateKey);
                binding.imgQRCode.setImageBitmap(bitmap);
            } catch (WriterException e) {
                Log.i("testDebug", "PrivateKey QR生成失败");
            }
        });


        homeViewModel.data.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String data) {
                // viewModelのLiveDataを監視して　更新された時にここが呼ばれる！
            }
        });
        return root;
    }

    private void startWebSocketService() {
        //联系后台服务
        Intent bindIntent = new Intent(getActivity(), WebSocketService.class);
        getActivity().bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //服务与活动成功绑定
            mWebSocketService = ((WebSocketService.JWebSocketClientBinder) iBinder).getService();
            Log.i("testDebug", "WebSocket服务与NotificationsFragment成功绑定");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //服务与活动断开
            mWebSocketService = null;
            Log.i("testDebug", "WebSocket服务与NotificationsFragment成功断开: ");
        }
    };
}