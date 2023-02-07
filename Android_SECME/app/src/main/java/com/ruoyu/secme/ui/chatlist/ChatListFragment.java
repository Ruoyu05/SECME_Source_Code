package com.ruoyu.secme.ui.chatlist;


import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyu.secme.ChatRoom;
import com.ruoyu.secme.CreateCertifyKey;
import com.ruoyu.secme.HelperFile.ChatItem;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.FriendItem;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.LastMessageItem;
import com.ruoyu.secme.MainActivity;
import com.ruoyu.secme.R;
import com.ruoyu.secme.databinding.FragmentChatlistBinding;
import com.ruoyu.secme.ui.dashboard.DashboardFragment;

import java.util.ArrayList;

public class ChatListFragment extends Fragment {
    private ChatModeAdapter adapter;
    private FragmentChatlistBinding binding;
    private WebSocketService mWebSocketService = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ChatListViewModel chatListViewModel =
                new ViewModelProvider(this).get(ChatListViewModel.class);


        binding = FragmentChatlistBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        startWebSocketService();

        binding.listChat.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("testDebug", "listChat点击位置:" + position);
                Intent intent = new Intent(getActivity(), ChatRoom.class);
                LastMessageItem lastMessageItem = adapter.getItem(position);

                intent.putExtra("md5Str",lastMessageItem.md5Str);
                intent.putExtra("friend_name",lastMessageItem.friendName);
                intent.putExtra("friend_nickname",lastMessageItem.nickName);
                intent.putExtra("friend_uuid",DBHelper.getFriendInfo(mWebSocketService.client.dbHelper.getReadableDatabase(),lastMessageItem.md5Str).friend_uuid);
                intent.putExtra("userMd5Str",mWebSocketService.client.userMd5Str);
                startActivity(intent);

            }
        });

        adapter = new ChatModeAdapter(getActivity());
        chatListViewModel.data.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String data) {
                // viewModelのLiveDataを監視して　更新された時にここが呼ばれる！

            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWebSocketService == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if(mWebSocketService != null){
                            updateTable();
                            break;
                        }
                    }
                }
            }).start();
        } else {
           updateTable();
        }

    }

    private void updateTable() {
        Log.i("debugTest", "进行更新表格");
        SQLiteDatabase db = new DBHelper(getActivity(), mWebSocketService.client.userMd5Str).getReadableDatabase();
        DBHelper dbHelper = mWebSocketService.client.dbHelper;
        ArrayList<LastMessageItem> chatItems = dbHelper.getLastMessageItem(db);
        adapter = new ChatModeAdapter(getActivity());
        for (int i = 0; i < chatItems.stream().count(); i++) {
            adapter.add(chatItems.get(i));
        }
        binding.listChat.setAdapter(adapter);
    }

    class ChatModeAdapter extends ArrayAdapter<LastMessageItem> {

        ChatModeAdapter(Context context) {
            super(context, R.layout.chat_lists);
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Log.i("debugTest", "position" + position);
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.chat_lists, null);
            }
            LastMessageItem lastMessageItem = getItem(position);
            TextView txtChatFriendName = (TextView) convertView.findViewById(R.id.txtChatFriendName);
            txtChatFriendName.setText(lastMessageItem.friendName);
            TextView txtChatFriendMessage = (TextView) convertView.findViewById(R.id.txtChatFriendMessage);
            txtChatFriendMessage.setText(lastMessageItem.lastMessage);

            return convertView;
        }
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


