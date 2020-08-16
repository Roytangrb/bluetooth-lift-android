package com.binaryyard.elevatorcontrol.views.advertise;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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

import com.binaryyard.elevatorcontrol.R;
import com.binaryyard.elevatorcontrol.classes.Utils;

import java.util.UUID;
import java.util.zip.CRC32;

/*
 * @description
 *  1. check BLE advertise supported, enable BLE if necessary
 *  2. on 'advertise' button clicked, refresh service uuid then advertise for 2 seconds
 */
public class AdvertiseFragment extends Fragment {

    private static final String TAG = "AdvertiseFragment";
    private final int ADVERTISE_TIMEOUT_MS = 2000;
    private static final int REQUEST_ENABLE_BT = 11;

    private String mServiceUUID;
    private TextView mTvServiceUUID;
    private Button mBtnAdvertise;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_advertise, container, false);

        mTvServiceUUID = root.findViewById(R.id.tv_service_uuid);
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
                refreshServiceUUID();
                advertise();
            }
        });

        return root;
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

    private void refreshServiceUUID() {
        // Filter - 32 bits (4 bytes)
        String filter = getString(R.string.BLE_ADVERTISE_FILTER);

        // The Rest of 96 bits (12 bytes = uuid 8 bytes + sign key 4 bytes)
        String sitecode = getString(R.string.SITE_CODE);
        String uuid = "73F14D4D8E47C090";
        long timestamp = getFloorUnixSeconds();

        // encode Data
        byte[] uuidBytes = Utils.hexToBytes(uuid); // 8 bytes
        byte[] timestampBytes = Utils.longToBytes(timestamp); // 8 bytes
        byte[] data = new byte[8];
        for (int i = 0; i < data.length; i ++){
            data[i] = (byte) (uuidBytes[i] ^ timestampBytes[i]);
        }

        //encode Sign Key
        byte[] sitecodeBytes = Utils.hexToBytes(sitecode);
        byte[] temp = Utils.concatByteArr(data, sitecodeBytes);
        byte[] secretBytes = Utils.concatByteArr(temp, timestampBytes);

        CRC32 crc32 = new CRC32();
        crc32.update(secretBytes);
        String signKey = Long.toHexString(crc32.getValue()).toUpperCase();

        // combine to 128 bits service uuid
        String service_uuid = Utils.toUUIDFormatStr(
                filter + Utils.bytesToHex(data) + signKey
        );

        mServiceUUID = service_uuid;
        mTvServiceUUID.setText(mServiceUUID);
    }

    // Reference: https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
    private void advertise(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // bluetoothAdapter.setName("CHBLE");

        if (!bluetoothAdapter.isEnabled()){
            enableBLE();
            return;
        }

        if ( !bluetoothAdapter.isMultipleAdvertisementSupported() ) {
            Toast.makeText( getActivity(), R.string.advertisement_not_supported, Toast.LENGTH_SHORT ).show();
            return;
        }

        Toast.makeText( getActivity(), R.string.advertising, Toast.LENGTH_SHORT ).show();

        final BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setTimeout(ADVERTISE_TIMEOUT_MS)
                .setConnectable(false)
                .build();

        ParcelUuid pUuid = new ParcelUuid( UUID.fromString(mServiceUUID) );

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid( pUuid )
                // .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "onStartSuccess: "+settingsInEffect.toString());
                super.onStartSuccess(settingsInEffect);

                mBtnAdvertise.setEnabled(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBtnAdvertise.setEnabled(true);
                    }
                }, ADVERTISE_TIMEOUT_MS);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Advertising onStartFailure: " + errorCode );
                super.onStartFailure(errorCode);
            }
        };

        advertiser.startAdvertising( settings, data, advertisingCallback );
    }
    private long getFloorUnixSeconds(){
        long unixSeconds = System.currentTimeMillis() / 1000L;
        long floorSeconds = unixSeconds - (unixSeconds % 10);
        return floorSeconds;
    }
}