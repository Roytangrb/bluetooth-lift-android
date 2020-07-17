package com.binaryyard.elevatorcontrol.views.qrcode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class QRCodeViewModel extends ViewModel {

    private MutableLiveData<String> mTitle;

    public QRCodeViewModel() {
        mTitle = new MutableLiveData<>();
        mTitle.setValue("");
    }

    public LiveData<String> getTitle() {
        return mTitle;
    }

    public void setTitle(String s){
        mTitle.postValue(s);
    }

    public String formatTitle(String sitecode, long timestamp, String uuid, String data, String signkey, String qrcode) {
        return "Site code: " + sitecode + "\n" +
                "Timestamp:  " + timestamp + "(" + getLocaleDate(timestamp) + ")\n" +
                "UUID: " + uuid + "\n" +
                "Data: " + data + "\n" +
                "Sign Key: " + signkey + "\n" +
                "QR Code: "+ qrcode + "\n";
    }

    private String getLocaleDate(long seconds){
        Date date = new Date(seconds * 1000L);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        String formattedDate = dateFormat.format(date);

        return formattedDate;
    }
}