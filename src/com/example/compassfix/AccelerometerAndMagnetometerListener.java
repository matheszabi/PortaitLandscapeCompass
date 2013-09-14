package com.example.compassfix;

import java.text.DecimalFormat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;

public class AccelerometerAndMagnetometerListener implements SensorEventListener {

	private static final String TAG = "AccelerometerAndMagnetometerListener";

	// private SensorManager mSensorManager;
	private int mScreenRotation;

	private MagneticOrientationRenderer renderer;
	
	private DecimalFormat df = new DecimalFormat();

	AccelerometerAndMagnetometerListener(int screenRotation, MagneticOrientationRenderer renderer) {
		// this.mSensorManager = mSensorManager;
		this.mScreenRotation = screenRotation;
		this.renderer = renderer;

		df.setMaximumFractionDigits(1);
		df.setPositivePrefix("+");
	}

	void setScreenRotation(int screenRotation) {
		this.mScreenRotation = screenRotation;
		
	}

	private String frm(float sensorValue) {

		return df.format(sensorValue);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

		Log.v(TAG, "onAccuracyChanged() accuracy:" + accuracy);
	}

	
	// onSensorChanged cached values for performance, not all needed to be declared here.
	private float[] mGravity = new float[3];
	private float[] mGeomagnetic = new float[3];
	private boolean mGravityUsed, mGeomagneticUsed;

	private float azimuth; // View to draw a compass 2D represents North
	private float pitch, roll; // used to show id the device is horizontal
	private float inclination;// Magnetic north and real North
	private float alpha = 0.09f;// low pass filter factor
	private boolean useLowPassFilter = false; // set to true if you have a GUI implementation of compass!

	private float mOrientation[] ;//= new float[3];

	private int i = 0;
	private int iLimit = 1;

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mGravityUsed = false;
			// apply a low pass filter: output = alpha*input + (1-alpha)*previous output;
			if (!useLowPassFilter) {
				mGravity[0] = alpha * event.values[0] + (1f - alpha) * mGravity[0];
				mGravity[1] = alpha * event.values[1] + (1f - alpha) * mGravity[1];
				mGravity[2] = alpha * event.values[2] + (1f - alpha) * mGravity[2];
			} else {
				mGravity = event.values.clone();
			}
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mGeomagneticUsed = false;
			// apply a low pass filter: output = alpha*input + (1-alpha)*previous output;
			if (useLowPassFilter) {
				mGeomagnetic[0] = alpha * event.values[0] + (1f - alpha) * mGeomagnetic[0];
				mGeomagnetic[1] = alpha * event.values[1] + (1f - alpha) * mGeomagnetic[1];
				mGeomagnetic[2] = alpha * event.values[2] + (1f - alpha) * mGeomagnetic[2];
			} else {
				mGeomagnetic = event.values.clone();
			}

		}

		if (!mGravityUsed && !mGeomagneticUsed) {
			float R[] = new float[9];
			// X (product of Y and Z) and roughly points East
			// Y: points to Magnetic NORTH and tangential to ground
			// Z: points to SKY and perpendicular to ground
			float I[] = new float[9];

			// see axis_device.png
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

			if (success) {
				
				// here need to use, but don't know which axes needed to be mapped:
				//boolean remapped = SensorManager.remapCoordinateSystem(R, axisX, axisY, R);
				// I will fix the result later

				mOrientation = new float[3];
				// see axis_globe_inverted.png
				SensorManager.getOrientation(R, mOrientation);
				inclination = SensorManager.getInclination(I);
				
				// not need to store values here, since they aren't fixed
//				azimuth = orientation[0]; 
//				pitch = orientation[1];  
//				roll = orientation[2];   

				// use values and wait for update for both values
				mGravityUsed = true;
				mGeomagneticUsed = true;	
				
				i++;
			}
		}

		if (i == iLimit) {
			i = 0;
			
			Log.d("CompassAngles", "Before Fix: azimuth: " + frm(mOrientation[0]) + ", pitch: " + frm(mOrientation[1]) + ", roll: " + frm(mOrientation[2]));
			// expecting airplane rotation http://en.wikipedia.org/wiki/File:Rollpitchyawplain.png
			switch (mScreenRotation) {
			case Surface.ROTATION_0:
				//Log.v("SurfaceRemap", "0 degree");
				fixRotation0(mOrientation);
				break;

			case Surface.ROTATION_90:
				//Log.v("SurfaceRemap", "90 degree");
				fixRotation90(mOrientation);
				break;

			case Surface.ROTATION_180:
				//Log.v("SurfaceRemap", "180 degree");
				fixRotation180(mOrientation);
				break;

			case Surface.ROTATION_270:
				//Log.v("SurfaceRemap", "270 degree");
				fixRotation270(mOrientation);
				break;

			default:
				Log.e("SurfaceRemap", "don't know the mScreenRotation value: " + mScreenRotation + " you should never seen this message!");
				break;
			}
			
			// expecting airplane rotation http://en.wikipedia.org/wiki/File:Rollpitchyawplain.png
			Log.d("CompassAngles", "After Fix: azimuth: " + frm(mOrientation[0]) + ", pitch: " + frm(mOrientation[1]) + ", roll: " + frm(mOrientation[2]));
			
			azimuth = mOrientation[0];
			pitch = mOrientation[1];
			roll = mOrientation[2];
			
			// separate sensor reference and maybe on new thread too is time consuming:			
			renderer.setRotationValues(azimuth, pitch, roll, inclination);
		}
	}

	public static final void fixRotation0(float[] orientation) {//azimuth, pitch, roll
		orientation[1] = -orientation[1];// pitch = -pitch
	}

	public static final  void fixRotation90(float[] orientation) {//azimuth, pitch, roll
		orientation[0] += Math.PI/2f; // offset
		float tmpOldPitch = orientation[1];
		orientation[1] = -orientation[2]; //pitch = -roll
		orientation[2] = -tmpOldPitch;	 // roll  = -pitch	
	}

	public static final  void fixRotation180(float[] orientation) {//azimuth, pitch, roll
		orientation[0] = (float) (orientation[0] > 0f ? (orientation[0] - Math.PI): (orientation[0] + Math.PI));// offset
		orientation[2] = -orientation[2];// roll = -roll
	}

	public static final  void fixRotation270(float[] orientation) {//azimuth, pitch, roll
		orientation[0] -= Math.PI/2;// offset
		float tmpOldPitch = orientation[1];
		orientation[1] = orientation[2]; //pitch = roll
		orientation[2] = tmpOldPitch;	 // roll  = pitch	
	}

}
