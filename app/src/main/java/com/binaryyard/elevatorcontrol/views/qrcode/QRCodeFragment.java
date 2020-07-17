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

import net.glxn.qrgen.android.QRCode;

import java.io.ByteArrayOutputStream;
import java.util.zip.CRC32;

public class QRCodeFragment extends Fragment {
    private static final String TAG = "QRCodeFragment";
    private QRCodeViewModel QRCodeViewModel;
    private ImageView ivQRCode;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        QRCodeViewModel = new ViewModelProvider(this).get(QRCodeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_qrcode, container, false);

        final TextView textView = root.findViewById(R.id.text_dashboard);
        QRCodeViewModel.getTitle().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        ivQRCode = root.findViewById(R.id.iv_qrcode_container);

        ivQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshQRCode();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshQRCode();
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
        Bitmap myBitmap = QRCode
                .from(qrcode)
                .withCharset("UTF-8")
                .withSize(250, 250)
                .bitmap();
        ivQRCode.setImageBitmap(myBitmap);
    }

    private long getFloorUnixSeconds(){
        long unixSeconds = System.currentTimeMillis() / 1000L;
        long floorSeconds = unixSeconds - (unixSeconds % 10);
        return floorSeconds;
    }
}
