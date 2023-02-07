package com.ruoyu.secme.ui.notifications;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.MainActivity;
import com.ruoyu.secme.databinding.FragmentNotificationsBinding;
import com.ruoyu.secme.ui.home.HomeViewModel;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    private WebSocketService mWebSocketService = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        startWebSocketService();

        binding.btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //测试用
                notificationsViewModel.doSomeTest();
                //断开连接
                mWebSocketService.disconnect();
                //跳转页面
//                Intent intent = new Intent(getActivity(), MainActivity.class);
//                startActivity(intent);
            }
        });
        notificationsViewModel.data.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String data) {
                // viewModelのLiveDataを監視して　更新された時にここが呼ばれる！


            }
        });

        return root;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startWebSocketService() {
        //联系后台服务
        Intent bindIntent = new Intent(getActivity(),WebSocketService.class);
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