package com.binaryyard.elevatorcontrol.views.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.binaryyard.elevatorcontrol.R;
import com.binaryyard.elevatorcontrol.classes.Utils;
import com.google.zxing.EncodeHintType;

import net.glxn.qrgen.android.QRCode;

import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.CRC32;

public class QRCodeFragment extends Fragment {

    private static final String TAG = "QRCodeFragment";
    private final int QRCODE_REFRESH_INTERVAL = 1000; //check every second
    private QRCodeViewModel QRCodeViewModel;
    private ImageView ivQRCode;
    private Timer mRefreshTimer;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        QRCodeViewModel = new ViewModelProvider(this).get(QRCodeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_qrcode, container, false);

        /* debug
        final TextView textView = root.findViewById(R.id.text_dashboard);
        QRCodeViewModel.getTitle().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        */

        ivQRCode = root.findViewById(R.id.iv_qrcode_container);

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
                    getActivity().runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            refreshQRCode();
                        }
                    });
                }
            }, 0, QRCODE_REFRESH_INTERVAL);
        }
    }

    private void refreshQRCode(){
        String sitecode = getString(R.string.SITE_CODE);
        String uuid = "73F14D4D8E47C090";
        long timestamp = getFloorUnixSeconds();

        // encode QRCode Data
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

        // combine to qrcode
        String qrcode = Utils.bytesToHex(data) + signKey;

        /* debug
        QRCodeViewModel.setTitle(
                QRCodeViewModel.formatTitle(
                        sitecode,
                        timestamp,
                        Utils.bytesToHex(uuidBytes),
                        Utils.bytesToHex(data),
                        signKey,
                        qrcode
                )
        );
         */

        Bitmap myBitmap = QRCode
                .from(qrcode)
                .withCharset("UTF-8")
                .withSize(250, 250)
                .withHint(EncodeHintType.MARGIN, 0)
                .bitmap();
        ivQRCode.setImageBitmap(myBitmap);
    }

    private long getFloorUnixSeconds(){
        long unixSeconds = System.currentTimeMillis() / 1000L;
        long floorSeconds = unixSeconds - (unixSeconds % 10);
        return floorSeconds;
    }
}
