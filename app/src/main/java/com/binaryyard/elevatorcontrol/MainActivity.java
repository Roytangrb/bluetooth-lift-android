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
import android.text.SpannableStringBuilder;
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
    private static final int REQUEST_ENABLE_BT = 11;
    private Activity mActivity = MainActivity.this;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mTargetDevice;

    private enum Connected {
        False,
        Pending,
        True
    }

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mActivity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        if(mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mActivity.bindService(new Intent(mActivity, SerialService.class), this, Context.BIND_AUTO_CREATE);
        }

        receiveText = findViewById(R.id.tv_receive_text);

        findViewById(R.id.btn_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(getString(R.string.SIGNAL_TEXT_LIGHT_ON));
            }
        });
        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(getString(R.string.SIGNAL_TEXT_LIGHT_OFF));
            }
        });
        findViewById(R.id.btn_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (connected != Connected.False) {
            disconnect();
        }

        try {
            mActivity.unbindService(this);
        } catch(Exception ignored) {
            Log.d(TAG, "onDestroy: unbind service failed");
        }

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(service != null) {
            service.attach(this);
        } else {
            // prevents service destroy on unbind from recreated activity caused by orientation change
            mActivity.bindService(new Intent(mActivity, SerialService.class), this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        if(service != null && !mActivity.isChangingConfigurations()) {
            service.detach();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mBluetoothAdapter == null) {
            Toast.makeText(mActivity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        } else if(!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(mActivity, R.string.ble_is_disabled, Toast.LENGTH_SHORT).show();
        }

        refresh();
    }

    private void refresh(){
        if(mBluetoothAdapter != null) {
            for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                    Log.d(TAG, "onResume: Device name: " + device.getName() + " ｜ Address: " + device.getAddress());
                    if (device.getName().equals(getString(R.string.TARGET_BLUETOOTH_DEVICE_NAME))){
                        mTargetDevice = device;
                    }
                }
            }
        }

        if(initialStart && service != null) {
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
            BluetoothDevice device = mTargetDevice;
            if (device != null){
                String deviceName = device.getName() != null ? device.getName() : device.getAddress();
                status("connecting...");
                connected = Connected.Pending;
                socket = new SerialSocket();
                service.connect(this, "Connected to " + deviceName);
                socket.connect(this, service, device);
            } else {
                Toast.makeText(mActivity, "<no bluetooth devices found>", Toast.LENGTH_SHORT).show();
            }
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
        if(connected != Connected.True) {
            Toast.makeText(mActivity, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
            receiveText.append(spn);
            byte[] data = (str + "\n").getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
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
     * Message and log helpers
     */
    private void receive(byte[] data) {
        receiveText.append(new String(data));
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        receiveText.append(spn);
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
