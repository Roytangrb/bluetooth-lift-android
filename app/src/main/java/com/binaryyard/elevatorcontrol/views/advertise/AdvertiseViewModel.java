package com.binaryyard.elevatorcontrol.views.advertise;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AdvertiseViewModel extends ViewModel {

    private MutableLiveData<String> mServiceUUID;

    public AdvertiseViewModel() {
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