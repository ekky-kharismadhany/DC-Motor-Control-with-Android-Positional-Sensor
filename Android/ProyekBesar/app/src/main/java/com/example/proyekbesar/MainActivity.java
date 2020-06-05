package com.example.proyekbesar;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    final Context context = this;
    //call  sensor manager instances
    private SensorManager sensorManager;
    //call accelerometer and magnetometer
    private Sensor sensorAccelerometer;
    private Sensor sensorMagenetometer;
    //getting data from those sensor
    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];

    //TextView for display
    private TextView textSensorAzimuth;
    private TextView textSensorPitch;
    private TextView textSensorRoll;

    private Display mDisplay;

    //your ip
    private EditText editTextIPAddress;

    //we dont want to data to be too shaky, so we give
    //threshold to be considered as acceptable movement
    private static final float VALUE_DRIFT = 0.05f;

    float azimuth, roll, pitch;

    String movementStatus = "";
    String serverAdress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textSensorAzimuth = findViewById(R.id.value_azimuth);
        textSensorPitch = findViewById(R.id.value_pitch);
        textSensorRoll = findViewById(R.id.value_roll);

        //get accelerometer and magnetometer data
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagenetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //get display
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();

        editTextIPAddress = findViewById(R.id.ipaddress);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(sensorAccelerometer != null) {
            sensorManager.registerListener(this, sensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (sensorMagenetometer != null) {
            sensorManager.registerListener(this, sensorMagenetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerData = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetometerData = event.values.clone();
            default:
                return;
        }

        //compute rotation matrix
        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, accelerometerData, magnetometerData);
        //remap matrix based current device/actitvity rotation
        float[] rotationMatrixAdjusted = new float[9];
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Y,
                        rotationMatrixAdjusted
                );
                break;
            case Surface.ROTATION_180:
                 SensorManager.remapCoordinateSystem(
                         rotationMatrix,
                         SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                         rotationMatrixAdjusted
                 );
                 break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                        rotationMatrixAdjusted
                );
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mDisplay.getRotation());
        }
        //get orientation of device
        //output : rad
        float[] orientationValues = new float[3];

        if (rotationOK)
            SensorManager.getOrientation(rotationMatrixAdjusted, orientationValues);

        //take value of array
        azimuth = orientationValues[0];
        pitch = orientationValues[1];
        roll = orientationValues[2];

        //threshold pitch and roll so the movement are not too shaky
        if (Math.abs(pitch) < VALUE_DRIFT)
            pitch = 0.0f;
        if (Math.abs(roll) < VALUE_DRIFT)
            roll = 0.0f;

        //fill string
        textSensorAzimuth.setText(getResources().getString(R.string.value_format, azimuth));
        textSensorPitch.setText(getResources().getString(R.string.value_format, pitch));
        textSensorRoll.setText(getResources().getString(R.string.value_format, roll));

        if (roll < -0.4)
            movementStatus = "Backward";
        else if (roll > 0.4)
            movementStatus = "Forward";
        else if (pitch > 0.3)
            movementStatus = "Left";
        else if (pitch < -0.3)
            movementStatus = "Right";
        else
            movementStatus = "Stationary";
        }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onClick(View view) {
        if (editTextIPAddress.getText().toString().equals(""))
            Toast.makeText(MainActivity.this, "Please Insert IP Address", Toast.LENGTH_SHORT).show();
        else {
            serverAdress = editTextIPAddress.getText().toString() + ":" + "80";
            HttpRequestTask requestTask = new HttpRequestTask(serverAdress);
            requestTask.execute(movementStatus);
        }
    }

    public class HttpRequestTask extends AsyncTask<String, Void, String>{

        private String serverAddress;
        private String serverResponse = "";
        private AlertDialog alertDialog;

        public HttpRequestTask(String serverAddress){
            this.serverAddress = serverAddress;

            alertDialog = new AlertDialog.Builder(context)
                    .setTitle("HTTP Response from IP Address : ")
                    .setCancelable(true)
                    .create();
        }

        @Override
        protected String doInBackground(String... strings) {

            if (!alertDialog.isShowing())
                alertDialog.show();

            String val  = strings[0];
            final String url = "http://" + serverAddress + "/control/" + val;

            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet getRequest = new HttpGet();
                getRequest.setURI(new URI(url));
                HttpResponse response = client.execute(getRequest);

                InputStream inputStream;
                inputStream = response.getEntity().getContent();
                BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(inputStream));
                serverResponse = bufferedReader.readLine();
                inputStream.close();
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                serverResponse = e.getMessage();
            }
            return serverResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            alertDialog.setMessage(serverResponse);

            if (!alertDialog.isShowing())
                alertDialog.show();
        }

        @Override
        protected void onPreExecute() {
            alertDialog.setMessage("Sending data to server, please wait ...");

            if(!alertDialog.isShowing())
                alertDialog.show();
        }
    }
}
//App
//TODO: make app constantly updating value of pitch and roll;
//TODO: make app look more pleasent
//TODO: optimize code
//ESP
//TODO: try to not close connection. Perhaps adding button for turn connection on and off?
//TODO: code for motor
//mech
//TODO: literally whole mech
