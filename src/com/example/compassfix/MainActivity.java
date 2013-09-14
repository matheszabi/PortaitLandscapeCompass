package com.example.compassfix;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL;// SensorManager.SENSOR_DELAY_UI;

	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private AccelerometerAndMagnetometerListener mSensorListener;
	
	private TextView tvInfo;

	// ------ ACTIVITY METHODS ----------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		tvInfo = (TextView)findViewById(R.id.tvInfo);

		int screenRotation = getWindowManager().getDefaultDisplay().getRotation();
		tvInfo.setText("initial screenRotation:" + (screenRotation * 90) + " degree");
		Log.v(TAG, "initial screenRotation:" + (screenRotation * 90) + " degree");
		

		TextView tvAzimuth = (TextView)findViewById(R.id.tvAzimuth);
		TextView tvPitch = (TextView)findViewById(R.id.tvPitch);
		TextView tvRoll = (TextView)findViewById(R.id.tvRoll);
		TextView tvInclination = (TextView)findViewById(R.id.tvInclination);
		MagneticOrientationRendererText renderer = new MagneticOrientationRendererText(tvAzimuth, tvPitch, tvRoll, tvInclination);
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorListener = new AccelerometerAndMagnetometerListener( screenRotation, renderer);
		

		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		Log.e(TAG, "max heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");// will start a session filter ins DDMS
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		int screenRotation = getWindowManager().getDefaultDisplay().getRotation();
		Log.v(TAG, "onConfigurationChanged(): screenRotation:" + (screenRotation * 90) + " degree");

		tvInfo.setText("screenRotation:" + (screenRotation * 90) + " degree");
		mSensorListener.setScreenRotation(screenRotation);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume()");

		if (accelerometer != null && magnetometer != null) {
			// register to sensors
			boolean registeredForAccerometer = mSensorManager.registerListener(mSensorListener, accelerometer, SENSOR_DELAY);
			boolean registeredForMagnetometer = mSensorManager.registerListener(mSensorListener, magnetometer, SENSOR_DELAY);

			if (!registeredForAccerometer || !registeredForMagnetometer) {
				createRestartDialog();
			}
		} else {
			createSensorListDialog();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v(TAG, "onPause()");
		mSensorManager.unregisterListener(mSensorListener);
	}

	// ------ END OF ACTIVITY METHODS ----------

	private void createSensorListDialog() {
		List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		StringBuilder sbSensorList = new StringBuilder();
		int sensorCount = 0;
		for (Sensor sensor : sensors) {
			String name = sensor.getName();
			int type = sensor.getType();
			sbSensorList.append(name).append(" type:").append(type).append('\n');
			sensorCount++;
		}
		Log.e(TAG, sbSensorList.toString());
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Missing a required sensor!");
		builder.setMessage("Accelerometer + Megnetometer needed.\nYou have " + sensorCount + " sensors:\n" + sbSensorList);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void createRestartDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Can't listen required sensors!");
		builder.setMessage("Can't listen sensors\n Try restart phone or buy a new one...");
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

}
