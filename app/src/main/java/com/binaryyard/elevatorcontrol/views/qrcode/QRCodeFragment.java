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

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class QRCodeFragment extends Fragment {

    private QRCodeViewModel QRCodeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        QRCodeViewModel = new ViewModelProvider(this).get(QRCodeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_qrcode, container, false);

        final TextView textView = root.findViewById(R.id.text_dashboard);
        QRCodeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        final ImageView qrCodeContainer = root.findViewById(R.id.iv_qrcode_container);

        qrCodeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(Calendar.getInstance().getTime());
                Bitmap myBitmap = QRCode
                        .from(timestamp)
                        .withCharset("UTF-8")
                        .withSize(250, 250)
                        .bitmap();
                qrCodeContainer.setImageBitmap(myBitmap);
            }
        });

        return root;
    }
}
