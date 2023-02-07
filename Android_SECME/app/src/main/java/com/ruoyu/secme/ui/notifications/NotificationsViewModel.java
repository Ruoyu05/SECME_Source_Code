package com.ruoyu.secme.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel {
    private final MutableLiveData<String> _data = new MutableLiveData<>();
    final LiveData<String> data = _data;

    public void doSomeTest() {
        // LiveDataの更新
        _data.setValue("changed!!");
    }
}