package com.binaryyard.elevatorcontrol.views.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel {

    private MutableLiveData<String> mServiceUUID;

    public NotificationsViewModel() {
        mServiceUUID = new MutableLiveData<>();
        mServiceUUID.setValue("62944f1d5399ab5fbbded89f3f582988");
    }

    public LiveData<String> getServiceUUID() {
        return mServiceUUID;
    }

    public void setServiceUUID(String newUUID){
        mServiceUUID.postValue(newUUID);
    }
}