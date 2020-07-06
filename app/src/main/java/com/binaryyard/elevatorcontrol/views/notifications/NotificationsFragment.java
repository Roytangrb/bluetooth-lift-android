package com.binaryyard.elevatorcontrol.views.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.binaryyard.elevatorcontrol.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private final int SERVICE_UUID_REFRESH_INTERVAL = 10000;
    private NotificationsViewModel notificationsViewModel;

    private TextView mTvServiceUUID;
    private Button mBtnAdvertise;
    private Timer mRefreshTimer;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        mTvServiceUUID = root.findViewById(R.id.tv_service_uuid);
        notificationsViewModel.getServiceUUID().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mTvServiceUUID.setText(s);
            }
        });

        mBtnAdvertise = root.findViewById(R.id.btn_advertise);

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mRefreshTimer == null) {
            mRefreshTimer = new Timer();
            mRefreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateServiceUUID();
                }
            }, 0, SERVICE_UUID_REFRESH_INTERVAL);
        }
    }

    private void updateServiceUUID() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String needle = df.format(c.getTime());

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(needle.getBytes());
            byte[] digest = md.digest();

            String hash = bytesToHex(digest);
            notificationsViewModel.setServiceUUID(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}