package com.example.bluetoothserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

public class MainActivity extends Activity implements
        SensorEventListener {

    private static final int DISCOVERABLE_REQUEST_CODE = 0x1;
    private boolean CONTINUE_READ_WRITE = true;

    private static final long UPDATE_THRESHOLD = (long) 0.0000002;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    public TextView mXValueView, mYValueView, mZValueView;
    private long mLastUpdate;
    private long myLastUpdate;
    public float x,y,z;
    public String newX, newY, newZ;

    public static int index = 0;

    private OutputStreamWriter os;
    private BluetoothSocket socket;
    private InputStream is;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Always make sure that Bluetooth server is discoverable during listening...
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverableIntent, DISCOVERABLE_REQUEST_CODE);

        // Get reference to SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get reference to Accelerometer
        if (null == (mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)))
            finish();

        mXValueView = (TextView) findViewById(R.id.x_value_view);
        mYValueView = (TextView) findViewById(R.id.y_value_view);
        mZValueView = (TextView) findViewById(R.id.z_value_view);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        UUID uuid = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66");
        try {
            BluetoothServerSocket serverSocket = adapter.listenUsingRfcommWithServiceRecord("BLTServer", uuid);
            android.util.Log.e("TrackingFlow", "Listening...");
            socket = serverSocket.accept();
            android.util.Log.e("TrackingFlow", "Socket accepted...");
            is = socket.getInputStream();
            os = new OutputStreamWriter(socket.getOutputStream());
        } catch (IOException e) {e.printStackTrace();}
    }

    // Register listener
    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI);

        mLastUpdate = System.currentTimeMillis();

    }

    // Unregister listener
    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    // Process new reading
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long actualTime = System.currentTimeMillis();

            if (actualTime - mLastUpdate > UPDATE_THRESHOLD) {

                mLastUpdate = actualTime;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                newX = String.format("%.2f", x);
                newY = String.format("%.2f", y);
                newZ = String.format("%.2f", z);

                mXValueView.setText(String.valueOf(x));
                mYValueView.setText(String.valueOf(y));
                mZValueView.setText(String.valueOf(z));

                if (index < 150) {
                    try {
                        os.write(index + ", " + newX + ", " + newY + ", " + newZ + " \n ");
                        os.flush();
                        index++;
                    } catch (IOException e) {e.printStackTrace();}
                } else {
                    try {
                        socket.close();
                        finish();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.e("TrackingFlow", "Creating thread to start listening...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(socket != null){
            try{
                is.close();
                os.close();
                socket.close();
            }catch(Exception e){}
            CONTINUE_READ_WRITE = false;
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // N/A
    }
}