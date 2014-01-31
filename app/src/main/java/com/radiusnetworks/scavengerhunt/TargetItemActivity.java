/**
 *
 * Copyright (c) 2013,2014 RadiusNetworks. All rights reserved.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Attribution Assurance License (AAL)
 * (adapted from the original BSD license) See the LICENSE file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 */
package com.radiusnetworks.scavengerhunt;

import java.text.DecimalFormat;
import java.util.List;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.IBeaconConsumer;

import android.os.Bundle;
import android.os.RemoteException;
import android.app.Activity;
import android.content.pm.ActivityInfo;

import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

/**
 * Displays the estimated distance in meters to the target
 */
public class TargetItemActivity extends Activity {
	public static final String TAG = "TargetItemActivity";
	private String huntId;

    private ScavengerHuntApplication application;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
        application = (ScavengerHuntApplication) this.getApplication();
		setContentView(R.layout.sh_activity_target_item);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
		    huntId = extras.getString("hunt_id");
		    Log.d(TAG, "Activity started with passed hunt id of "+huntId);
		}
        application.setItemActivity(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	@Override 
	protected void onDestroy() {
		super.onDestroy();
        application.setItemActivity(null);
    }
	
	public void updateDistance(final IBeacon iBeacon, final IBeaconData data) {
        if (data == null || !data.get("hunt_id").equals(huntId)) {
            // This iBeacon isn't the one we are showing.  Ignore it.
            return;
        }
        final double distance = iBeacon.getAccuracy();

    	runOnUiThread(new Runnable() {
   	     public void run() {
   			findViewById(R.id.sh_progressBar1);
   			TextView distanceView = (TextView) findViewById(R.id.sh_distance);
   			if (distance < 0) {
   				distanceView.setText("--");
   			}
   			else {
   				Log.i(TAG, "Making distance displayed be "+distance);
   				if (distance < 10) {				
   					DecimalFormat df = new DecimalFormat("#.#");
   					distanceView.setText(df.format(distance));													
   				}
   				else {
   					DecimalFormat df = new DecimalFormat("#");
   					distanceView.setText(df.format(distance));																					
   				}
   			}		
		    		    	    	 
   	     }
    	});
				
	}
	
}
