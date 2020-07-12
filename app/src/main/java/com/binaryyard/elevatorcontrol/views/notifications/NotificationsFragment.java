package com.binaryyard.elevatorcontrol.views.notifications;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.binaryyard.elevatorcontrol.R;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private final int SERVICE_UUID_REFRESH_INTERVAL = 10000;
    private final int ADVERTISE_TIMEOUT_MS = 5000;
    private static final int REQUEST_ENABLE_BT = 11;
    private NotificationsViewModel notificationsViewModel;

    private TextView mTvServiceUUID;
    private Button mBtnAdvertise;
    private Timer mRefreshTimer;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        mTvServiceUUID = root.findViewById(R.id.tv_service_uuid);
        mTvServiceUUID.setText(getString(R.string.ble_advertise_test_uuid));
        /*
        notificationsViewModel.getServiceUUID().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mTvServiceUUID.setText(s);
            }
        });
        */

        mBtnAdvertise = root.findViewById(R.id.btn_advertise);

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText( getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            mBtnAdvertise.setEnabled(false);
        } else{
            enableBLE();
        }

        mBtnAdvertise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advertise();
            }
        });

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

    private void enableBLE() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == Activity.RESULT_OK){
                mBtnAdvertise.setEnabled(true);
            }
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

    //Reference: https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
    private void advertise(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()){
            enableBLE();
            return;
        }

        if ( !bluetoothAdapter.isMultipleAdvertisementSupported() ) {
            Toast.makeText( getActivity(), "Advertisement not supported", Toast.LENGTH_SHORT ).show();
            return;
        }

        Toast.makeText( getActivity(), "Advertising", Toast.LENGTH_SHORT ).show();

        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setTimeout(ADVERTISE_TIMEOUT_MS)
                .setConnectable(false)
                .build();

        ParcelUuid pUuid = new ParcelUuid( UUID.fromString( getString( R.string.ble_advertise_test_uuid ) ) );

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid( pUuid )
//                .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "onStartSuccess: "+settingsInEffect.toString());
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
                super.onStartFailure(errorCode);
            }
        };

        advertiser.startAdvertising( settings, data, advertisingCallback );
    }
}