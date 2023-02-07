package com.ruoyu.secme.ui.chattingroom;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChattingRoomViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    private final MutableLiveData<String> _data = new MutableLiveData<>();

    final LiveData<String> data = _data;

    public void doSomeTest() {
        // LiveDataの更新
        _data.setValue("This is home ChatList changed!!");
    }
}