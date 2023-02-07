package com.ruoyu.secme.ui.chatlist;

import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChatListViewModel extends ViewModel {

    private final MutableLiveData<String> _data = new MutableLiveData<>();

    final LiveData<String> data = _data;

    public void doSomeTest() {
        // LiveDataの更新
        _data.setValue("This is home ChatList changed!!");
    }
}
