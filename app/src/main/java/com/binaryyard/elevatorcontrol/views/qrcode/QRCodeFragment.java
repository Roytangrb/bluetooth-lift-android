package com.binaryyard.elevatorcontrol.views.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
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

import net.glxn.qrgen.android.QRCode;

import java.util.zip.CRC32;

public class QRCodeFragment extends Fragment {

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
        long timestamp = getFloorUnixSeconds();
        String uuid = "73F14D4D8E47C090";

        CRC32 crc32 = new CRC32();
        crc32.update(String.valueOf(timestamp).getBytes());
        String data = Long.toHexString(crc32.getValue());

        QRCodeViewModel.setTitle(
                QRCodeViewModel.formatTitle(getString(R.string.SITE_CODE), timestamp, uuid, data, "")
        );
        Bitmap myBitmap = QRCode
                .from(String.valueOf(timestamp))
                .withCharset("UTF-8")
                .withSize(250, 250)
                .bitmap();
        ivQRCode.setImageBitmap(myBitmap);
    }

    private long getFloorUnixSeconds(){
        long unixSeconds = System.currentTimeMillis() / 1000L;
        long floorSeconds = unixSeconds / 10L * 10L;
        return floorSeconds;
    }
}
