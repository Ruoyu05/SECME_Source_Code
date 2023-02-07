package com.ruoyu.secme.ui.chattingroom;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ruoyu.secme.HelperFile.ChatMessageTable;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.R;
import com.ruoyu.secme.databinding.FragmentChatlistBinding;

public class ChattingRoomFragment extends Fragment {

    private ChattingRoomViewModel mViewModel;

    private FragmentChatlistBinding binding;

    private WebSocketService mWebSocketService = null;

    private ChatMessageAdapter adapter;

    private SQLiteDatabase db = null;

    private String friend_uuid = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ChattingRoomViewModel.class);
        // TODO: Use the ViewModel

        ChattingRoomViewModel chattingRoomViewModel =  new ViewModelProvider(this).get(ChattingRoomViewModel.class);

        chattingRoomViewModel.data.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String data) {
                // viewModelのLiveDataを監視して　更新された時にここが呼ばれる！

                Log.i("debugTest", "onChanged");

            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chatroom, container, false);
    }


    class ChatMessageAdapter extends ArrayAdapter<ChatMessageTable> {

        ChatMessageAdapter(Context context) {
            super(context, R.layout.chat_message_table);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.chat_message_table, null);
            }
            ChatMessageTable item = getItem(position);

            TextView txtChatMessage = (TextView) convertView.findViewById(R.id.txtInMessage);
            txtChatMessage.setText(item.message);

            return convertView;
        }
    }

}