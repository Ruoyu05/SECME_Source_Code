package com.ruoyu.secme;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.ui.chattingroom.ChattingRoomFragment;

public class ChattingRoom extends AppCompatActivity {

    private WebSocketService mWebSocketService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting_room);
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.container, ChattingRoomFragment.newInstance())
//                    .commitNow();
//        }


    }
}