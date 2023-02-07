package com.ruoyu.secme.ui.dashboard;

import android.widget.ListView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> _data = new MutableLiveData<>();

    final LiveData<String> data = _data;
    final ListView friendList = null;

    public void doSomeTest() {
        // LiveDataの更新
        _data.setValue("This is home dashboard changed!!");

    }

}