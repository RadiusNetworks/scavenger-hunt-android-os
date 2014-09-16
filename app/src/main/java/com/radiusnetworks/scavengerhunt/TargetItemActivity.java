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

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconData;

import java.text.DecimalFormat;

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
            String title = extras.getString("title");
            String description = extras.getString("description");
            Log.d(TAG, "Activity started with passed hunt id of "+huntId+". "+title+". "+description);

            ((TextView) findViewById(R.id.sh_title)).setText(title);
            ((TextView) findViewById(R.id.sh_description)).setText(description);

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
	
	public void updateDistance(final Beacon beacon, final BeaconData data) {
        if (data == null || !data.get("hunt_id").equals(huntId)) {
            // This beacon isn't the one we are showing.  Ignore it.
            return;
        }
        final double distance = beacon.getDistance();

    	runOnUiThread(new Runnable() {
   	     public void run() {
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
