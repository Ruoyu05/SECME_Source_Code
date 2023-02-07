package com.ruoyu.secme.ui.dashboard;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.ruoyu.secme.AddFriendPage;
import com.ruoyu.secme.ChatRoom;
import com.ruoyu.secme.FriendDetail;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.FriendItem;
import com.ruoyu.secme.HelperFile.RSAHelper;
import com.ruoyu.secme.HelperFile.TypeChangeHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.Contents;
import com.ruoyu.secme.JsonType.FriendInfo;
import com.ruoyu.secme.JsonType.Inside;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.SendFrom;
import com.ruoyu.secme.JsonType.SendTo;
import com.ruoyu.secme.MyQRCode;
import com.ruoyu.secme.R;
import com.ruoyu.secme.databinding.FragmentDashboardBinding;

import org.bouncycastle.jcajce.provider.asymmetric.RSA;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.UUID;

public class DashboardFragment extends Fragment {

    private RowModeAdapter adapter;
    private FragmentDashboardBinding binding;
    private WebSocketService mWebSocketService = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        startWebSocketService();

        binding.imgBtnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddFriendPage.class);
            startActivity(intent);
        });

        adapter = new RowModeAdapter(getActivity());

        binding.listFriends.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("testDebug", "listFriends点击位置:" + position);

                FriendItem friendItem = adapter.getItem(position);

                Intent intent = new Intent(getActivity(), FriendDetail.class);
                String friendname = friendItem.name;
                String friendhost = friendItem.host;
                String friendport = friendItem.port;
                String md5Str = friendItem.md5Str;

                intent.putExtra("friendname", friendname);
                intent.putExtra("friendhost", friendhost);
                intent.putExtra("friendport", friendport);
                intent.putExtra("md5Str", md5Str);

                startActivity(intent);
            }
        });

        dashboardViewModel.data.observe(getViewLifecycleOwner(), new Observer<String>() {
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
        updateTable();
    }

    private void updateTable() {
        SQLiteDatabase db = new DBHelper(getActivity(), mWebSocketService.client.userMd5Str).getReadableDatabase();
        ArrayList<FriendItem> friendItem = DBHelper.getFriendItem(db);
        adapter = new RowModeAdapter(getActivity());
        for (int i = 0; i < friendItem.stream().count(); i++) {
            Log.i("testDebug", "i = " + i);
            adapter.add(friendItem.get(i));
        }
        binding.listFriends.setAdapter(adapter);
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


    class RowModeAdapter extends ArrayAdapter<FriendItem> {

        RowModeAdapter(Context context) {
            super(context, R.layout.friend_list_item);
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Log.i("debugTest", "position" + position);
            FriendItem item = getItem(position);
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.friend_list_item, null);
            }
            if (item != null) {
                TextView txtFriendName = (TextView) convertView.findViewById(R.id.txtFriendName);
                txtFriendName.setText(item.name);
                ImageButton imgBtnAgree = (ImageButton) convertView.findViewById(R.id.imgBtnAgree);
                ImageButton imgBtnDelect = (ImageButton) convertView.findViewById(R.id.imgBtnDelect);
                ImageButton imgBtnWaitting = (ImageButton) convertView.findViewById(R.id.imgBtnWaitting);

                //设定监听
                imgBtnAgree.setOnClickListener(v -> {

                    agreeFriend(item.md5Str);
                });

                imgBtnDelect.setOnClickListener(v -> {
                    delectFriend();
                });

                imgBtnWaitting.setOnClickListener(v -> {
                    showWaittingFriend();
                });

                //设定显示
                if (item.friend_uuid.equals("null")) {
                    Log.i("debugTest", "friend_uuid:" + item.friend_uuid);
                    //如果为空，说明是等待好友回复，只显示等待按钮
                    imgBtnAgree.setVisibility(View.VISIBLE);//隐藏按钮
                    imgBtnDelect.setVisibility(View.VISIBLE);//显示按钮
                    imgBtnWaitting.setVisibility(View.GONE);//隐藏按钮
                } else if (item.active) {
                    //显示同意或者删除按钮
                    imgBtnAgree.setVisibility(View.GONE);
                    imgBtnDelect.setVisibility(View.GONE);
                    imgBtnWaitting.setVisibility(View.GONE);
                } else {
                    //显示同意或者删除按钮
                    imgBtnAgree.setVisibility(View.GONE);
                    imgBtnDelect.setVisibility(View.GONE);
                    imgBtnWaitting.setVisibility(View.VISIBLE);
                }
            }
            return convertView;
        }
    }

    private void agreeFriend(String md5Str) {
        Toast.makeText(getActivity(), "Agree请求！", Toast.LENGTH_SHORT).show();
        try {

            //服务器公钥
            String server_publicKeyStr = mWebSocketService.client.server_publicKeyStr;

            //获取好友的聊天公钥

            //获取好友的身份公钥

            DBHelper dbHelper = mWebSocketService.client.dbHelper;
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            FriendInfo friendInfo = dbHelper.getFriendInfo(db, md5Str);
            //FIXME friend_uuid 为空值

            //将好友激活
            //生成一对聊天密钥

            KeyPair my_Chat_KeyPair = RSAHelper.createKeyPair(1024);
            //FIXME 密钥没要做IOS更换

            String my_chat_publickey = RSAHelper.getStringFromKey(my_Chat_KeyPair.getPublic());
            String my_chat_privatekey = RSAHelper.getStringFromKey(my_Chat_KeyPair.getPrivate());


            Log.i("testDebug", "生成的my_chat_publickey:" + my_chat_publickey);
            String friend_uuid = UUID.randomUUID().toString();

            friendInfo.friend_uuid = friend_uuid;
            friendInfo.my_chat_privatekey = my_chat_privatekey;
            friendInfo.my_chat_publickey = my_chat_publickey;
            friendInfo.active = true;

            //获取自己的身份公钥和私钥
            String my_Certify_PrivateKey = dbHelper.getCertify_PrivateKey(db);
            String my_Certify_PublicKey = dbHelper.getCertify_PublicKey(db);
            Gson gson = new Gson();

            SendFrom sendFrom = new SendFrom();
            sendFrom.username = mWebSocketService.client.username;
            sendFrom.host = mWebSocketService.client.host;
            sendFrom.port = mWebSocketService.client.portStr;
            sendFrom.uuid = friendInfo.my_uuid;
            String sendFromStr = gson.toJson(sendFrom);


            if (RSAHelper.is_IOS_PublicKey(friendInfo.friend_certifyPublicKey)) {
                //为IOS的公钥 做个转换
                PublicKey ios_pk = RSAHelper.getPublicKey_from_ios(friendInfo.friend_certifyPublicKey);
                friendInfo.friend_certifyPublicKey = RSAHelper.getStringFromKey(ios_pk);
            }

            String encryptedSendFromStr = RSAHelper.encryptJsonMessage(sendFromStr, friendInfo.friend_certifyPublicKey);
            //加密

            Inside inside = new Inside();
            inside.friend_chat_publickey = friendInfo.my_chat_publickey;
            inside.uuid = friendInfo.friend_uuid;
            String insideStr = gson.toJson(inside);

            if (RSAHelper.is_IOS_PublicKey(friendInfo.friend_chat_publickey)) {
                //为IOS的公钥 做个转换
                PublicKey ios_pk = RSAHelper.getPublicKey_from_ios(friendInfo.friend_chat_publickey);
                friendInfo.friend_chat_publickey = RSAHelper.getStringFromKey(ios_pk);
            }

            String encryptedInsideStr = RSAHelper.encryptJsonMessage(insideStr, friendInfo.friend_chat_publickey);

            SendTo sendTo = new SendTo();
            sendTo.username = friendInfo.friendName;
            sendTo.host = friendInfo.host;
            sendTo.port = friendInfo.port;
            sendTo.uuid = friendInfo.friend_uuid;

            String signStr = sendFromStr + insideStr;
            String md5code = TypeChangeHelper.getMd5(signStr);

            String signature = RSAHelper.sign(md5code, my_Certify_PrivateKey);

            Contents contents = new Contents();
            contents.send_from = encryptedSendFromStr;
            contents.signature = signature;
            contents.send_to = sendTo;
            contents.inside = encryptedInsideStr;

            JsonMessage jsonMessage = new JsonMessage();
            jsonMessage.type = "PostSendRequest";
            jsonMessage.contents = contents;

            String jsonMessageStr = gson.toJson(jsonMessage);

            Log.i("testDebug", "is_IOS_PublicKey:" + RSAHelper.is_IOS_PublicKey(mWebSocketService.client.server_publicKeyStr));

            Log.i("testDebug", "is_Java_PublicKey:" + RSAHelper.is_Java_PublicKey(mWebSocketService.client.server_publicKeyStr));

            String encryptedJsonMessageStr = RSAHelper.encryptJsonMessage(jsonMessageStr, mWebSocketService.client.server_publicKeyStr);

            //将更新后的好友数据存到数据库
            DBHelper.updateFriend(db, friendInfo);
            Log.i("testDebug", "更新完成");

            //发送
            mWebSocketService.client.send(encryptedJsonMessageStr.getBytes());
            Log.i("testDebug", "发送完成");


        } catch (Exception e) {
            e.printStackTrace();
        }

        updateTable();

    }

    private void delectFriend() {
        Toast.makeText(getActivity(), "删除好友！", Toast.LENGTH_SHORT).show();
    }

    private void showWaittingFriend() {
        Toast.makeText(getActivity(), "等待好友通过请求！", Toast.LENGTH_SHORT).show();
    }


}
