package com.ruoyu.secme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ruoyu.secme.HelperFile.ChatMessageTable;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.RSAHelper;
import com.ruoyu.secme.HelperFile.TypeChangeHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.Contents;
import com.ruoyu.secme.JsonType.FriendInfo;
import com.ruoyu.secme.JsonType.Inside;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.SendFrom;
import com.ruoyu.secme.JsonType.SendTo;

import java.security.PublicKey;
import java.util.ArrayList;

public class ChatRoom extends AppCompatActivity {

    private WebSocketService mWebSocketService = null;
    private SQLiteDatabase db = null;
    private String friend_uuid = null;
    private ChatMessageAdapter adapter;
    private boolean stopCheckMessage = false;

    final Handler handler = new Handler();
    final Runnable upDateUI = new Runnable() {
        @Override
        public void run() {
            Log.i("testDebug", "更新UI");
            updateTable(db,friend_uuid);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        startWebSocketService();

        Intent intent = getIntent();
        String friend_name = intent.getStringExtra("friend_name");
        String md5Str = intent.getStringExtra("md5Str");
        friend_uuid = intent.getStringExtra("friend_uuid");
        String userMd5Str = intent.getStringExtra("userMd5Str");

        db = new DBHelper(ChatRoom.this, userMd5Str).getReadableDatabase();

        TextView txtChatRoomName = (TextView) findViewById(R.id.txtChatRoomName);
        txtChatRoomName.setText(friend_name);

        adapter = new ChatMessageAdapter(ChatRoom.this);

        ImageButton imgBtnSendMessage = (ImageButton) findViewById(R.id.imgBtnSendMessage);

        imgBtnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText edtChatMessage = (EditText) findViewById(R.id.edtChatMessage);
                String message = edtChatMessage.getText().toString();
                if (message.equals("")) {
                    Toast.makeText(ChatRoom.this, "メーセージを入力してください！", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        DBHelper dbHelper = mWebSocketService.client.dbHelper;
                        String certifyPrivateKeyStr = dbHelper.getCertifyPrivateKey(dbHelper.getReadableDatabase());
                        FriendInfo friendInfo = dbHelper.getFriendInfo(dbHelper.getReadableDatabase(), md5Str);

                        //制作jsonMessage
                        Gson gson = new Gson();
                        SendTo sendto = new SendTo();
                        sendto.username = friendInfo.friendName;
                        sendto.host = friendInfo.host;
                        sendto.port = friendInfo.port;

                        SendFrom sendFrom = new SendFrom();
                        sendFrom.username = mWebSocketService.client.username;
                        sendFrom.host = mWebSocketService.client.host;
                        sendFrom.port = mWebSocketService.client.portStr;
                        sendFrom.uuid = friendInfo.my_uuid;
                        String sendFrom_json = gson.toJson(sendFrom);

                        Log.i("testDebug", "is java key:" + RSAHelper.is_Java_PublicKey(friendInfo.friend_certifyPublicKey));

                        if (RSAHelper.is_IOS_PublicKey(friendInfo.friend_certifyPublicKey)) {
                            //为IOS的公钥 做个转换
                            PublicKey ios_pk = RSAHelper.getPublicKey_from_ios(friendInfo.friend_certifyPublicKey);
                            friendInfo.friend_certifyPublicKey = RSAHelper.getStringFromKey(ios_pk);
                        }

                        Log.i("testDebug", "is java key:" + RSAHelper.is_Java_PublicKey(friendInfo.friend_certifyPublicKey));


                        String encrypted_sendFrom_json = RSAHelper.encryptJsonMessage(sendFrom_json, friendInfo.friend_certifyPublicKey);

                        Inside inside = new Inside();
                        inside.message = message;
                        String inside_json = gson.toJson(inside);

                        if (RSAHelper.is_IOS_PublicKey(friendInfo.friend_chat_publickey)) {
                            //为IOS的公钥 做个转换
                            PublicKey ios_pk = RSAHelper.getPublicKey_from_ios(friendInfo.friend_chat_publickey);
                            friendInfo.friend_chat_publickey = RSAHelper.getStringFromKey(ios_pk);
                        }

                        String encrypted_inside_json = RSAHelper.encryptJsonMessage(inside_json, friendInfo.friend_chat_publickey);

                        String signStr = sendFrom_json + inside_json;
                        String md5code = TypeChangeHelper.getMd5(signStr);
                        String signature = RSAHelper.sign(md5code, certifyPrivateKeyStr);

                        Contents contents = new Contents();
                        contents.inside = encrypted_inside_json;
                        contents.send_to = sendto;
                        contents.send_from = encrypted_sendFrom_json;
                        contents.signature = signature;

                        JsonMessage jsonMessage = new JsonMessage();
                        jsonMessage.type = "PostSendRequest";
                        jsonMessage.contents = contents;
                        String jsonMessage_json = gson.toJson(jsonMessage);
                        String encrypted_jsonMessage_json = RSAHelper.encryptJsonMessage(jsonMessage_json, mWebSocketService.client.server_publicKeyStr);
                        mWebSocketService.client.send(encrypted_jsonMessage_json.getBytes());
                        Log.i("testDebug", "send:" + message);
                        //将聊天保存到本地数据库
                        DBHelper.saveChatMessage(db, friend_uuid, message);

                        //FIXME 更新列表
                        ChatMessageTable chatMessageTable = new ChatMessageTable();
                        chatMessageTable.message = message;
                        chatMessageTable.isIn = false;
                        appendMessage(chatMessageTable);
                        edtChatMessage.setText("");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        checkNewMessage();
        updateTable(db,friend_uuid);

    }

    private void checkNewMessage(){
        Thread check = new Thread(){
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                        if (stopCheckMessage) {
                            break;
                        }
                        if (mWebSocketService.client.haveNewMessage) {
                            //得到了新消息
                            ArrayList<ChatMessageTable> chatMessageTables = DBHelper.getChatMessages(db, friend_uuid);
                            //不一致时候更新UI
                            if(adapter.getCount() != chatMessageTables.size()){
                                handler.post(upDateUI);
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        check.start();
    }

    @Override
    public void finish() {
        super.finish();
        stopCheckMessage = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (friend_uuid != null) {
            updateTable(db, friend_uuid);
        }
    }

    private void appendMessage(ChatMessageTable newChatMessageTable) {
        adapter.add(newChatMessageTable);
    }

    private void updateTable(SQLiteDatabase db, String friend_uuid) {
        ListView listChatMessage = (ListView) findViewById(R.id.listChatMessage);
        adapter = new ChatMessageAdapter(ChatRoom.this);

        ArrayList<ChatMessageTable> chatMessageTables = DBHelper.getChatMessages(db, friend_uuid);
        for (int i = 0; i < chatMessageTables.stream().count(); i++) {
            adapter.add(chatMessageTables.get(i));
        }
        listChatMessage.setAdapter(adapter);
    }

    class ChatMessageAdapter extends ArrayAdapter<ChatMessageTable> {

        ChatMessageAdapter(Context context) {
            super(context, R.layout.chat_message_table);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.chat_message_table, null);
                ChatMessageTable item = getItem(position);
                TextView txtInMessage = (TextView) convertView.findViewById(R.id.txtInMessage);
                TextView txtOutMessage = (TextView) convertView.findViewById(R.id.txtOutMessage);

                txtOutMessage.setVisibility(View.GONE);
                txtInMessage.setVisibility(View.GONE);

                if (item.isIn) {
                    txtInMessage.setText(item.message);
                    txtInMessage.setVisibility(View.VISIBLE);
                }else{
                    txtOutMessage.setText(item.message);
                    txtOutMessage.setVisibility(View.VISIBLE);
                }

           //  }
            return convertView;
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