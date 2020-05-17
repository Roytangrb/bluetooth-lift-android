package com.binaryyard.elevatorcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.binaryyard.elevatorcontrol.classes.SerialListener;
import com.binaryyard.elevatorcontrol.classes.SerialSocket;
import com.binaryyard.elevatorcontrol.services.SerialService;

public class MainActivity extends AppCompatActivity implements ServiceConnection, SerialListener {

    private TextView receiveText;

    private static final String TAG = "MainActivity";
    private Activity mActivity = MainActivity.this;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice mTempTargetDevice;

    private enum Connected { False, Pending, True }

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiveText = findViewById(R.id.tv_receive_text);

        if(mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("1");
            }
        });
        findViewById(R.id.btn_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("0");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(bluetoothAdapter != null) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                    Log.d(TAG, "onResume: " + device.getName() + " address " + device.getAddress());
                    if (device.getName().equals("HC-05")){
                        mTempTargetDevice = device;
                        refresh();
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (connected != Connected.False)
            disconnect();
        mActivity.stopService(new Intent(mActivity, SerialService.class));
    }

    private void refresh(){
        mActivity.bindService(new Intent(mActivity, SerialService.class), this, Context.BIND_AUTO_CREATE);

        if(initialStart && service !=null) {
            initialStart = false;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            });
        }
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = mTempTargetDevice;
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status("connecting...");
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Connected to " + deviceName);
            socket.connect(this, service, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    private void send(String str) {
        Log.d(TAG, "send: " + str);
        if(connected != Connected.True) {
            Toast.makeText(mActivity, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
//            spn.setSpan(new ForegroundColorSpan(0, 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            byte[] data = (str + "\n").getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        receiveText.append(new String(data));
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
//        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart) {
            initialStart = false;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            });
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }
}
