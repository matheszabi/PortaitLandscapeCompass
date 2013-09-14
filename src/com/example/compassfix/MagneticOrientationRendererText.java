package com.example.compassfix;

import java.text.DecimalFormat;

import android.widget.TextView;

public class MagneticOrientationRendererText implements MagneticOrientationRenderer {


	private DecimalFormat df = new DecimalFormat();
	
	private TextView tvAzimuth;
	private TextView tvPitch;
	private TextView tvRoll;
	private TextView tvInclination;

	public MagneticOrientationRendererText(TextView tvAzimuth, TextView tvPitch, TextView tvRoll, TextView tvInclination) {
		this.tvAzimuth = tvAzimuth;
		this.tvPitch = tvPitch;
		this.tvRoll = tvRoll;		
		this.tvInclination = tvInclination;

		df.setMaximumFractionDigits(1);
		df.setPositivePrefix("+");
	}

	@Override
	public void setRotationValues(float azimuth, float pitch, float roll, float inclination) {
		tvAzimuth.setText("Azim: "+df.format(azimuth)+" -to North, if device is on the ground");
		tvPitch.setText("Pitch: "+df.format(pitch));
		tvRoll.setText("Roll: "+df.format(roll));	
		tvInclination.setText("Incl: "+inclination);	
	}

}
