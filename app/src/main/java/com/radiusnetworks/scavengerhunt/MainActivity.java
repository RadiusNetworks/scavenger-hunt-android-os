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


import com.radiusnetworks.ibeacon.BleNotAvailableException;
import com.radiusnetworks.ibeacon.IBeaconManager;

import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;

import android.util.Log;
import android.view.View;
import android.widget.TextView;


/**
 * @author dyoung
 *
 * This activity displays the status of the scavenger hunt, which can include four separate layouts:
 * - unstarted  (user has not clicked the hunt start button yet)
 * - started (user has cliecked the start button but has not founda ll the targets)
 * - finished (user has found all the targets)
 * - redeemed (user has shown the completion code)
 */
public class MainActivity extends Activity  {
	public static final String TAG = "MainActivity";
    private ScavengerHuntApplication application;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        application = (ScavengerHuntApplication) this.getApplication();
		updateViewsForHuntState();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        // This is needed for the case of hitting back on the loading screen after a restart
        if (application.getHunt() == null) {
            finish();
            return;
        }
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		updateViewsForHuntState();
	}
	
		
	public void onStartClicked(View view) {
		if (application.getHunt().getElapsedTime() == 0) {
            application.getHunt().start();
            application.getHunt().saveToPreferences(this);
		}
		Intent i = new Intent(getApplicationContext(), TargetCollectionActivity.class);
	    startActivity(i);		
	}
	
	public void onResetClicked(View view) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	Hunt hunt = application.getHunt();
    	if (hunt.everythingFound()) {
    		builder.setTitle("Are you sure you are finished?");		
    		builder.setMessage("You will not be allowed to return to this page.");    		
    	}
    	else {
    		builder.setTitle("Are you sure?");	// Are you sure you are finished?  You will not be allowed to return to this page		
    		builder.setMessage("All found locations will be cleared.");    		
    	}
		builder.setPositiveButton(android.R.string.cancel, null);
		builder.setNegativeButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
                application.startOver(MainActivity.this);
			}
			
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		
	}

	public void onRedeemClicked(View view) {
		setContentView(R.layout.sh_activity_redeemed);	    		
		((TextView)findViewById(R.id.sh_redemption_text)).setText("Your completion code is: "+application.getHunt().getDeviceUuid());

	}
	
	/*
	  This method figures out the state of the hunt, and shows the proper layout view
	 */
    private void updateViewsForHuntState() {
    	Hunt hunt = application.getHunt();
    	if (hunt.everythingFound()) {
			setContentView(R.layout.sh_activity_finished);	
            
    	}
    	else if (hunt.getElapsedTime() > 0) {
			setContentView(R.layout.sh_activity_started);	    		
    		TextView textView = ((TextView)findViewById(R.id.sh_hunt_status_text));
    		if (textView != null) {
    			textView.setText("You have found "+hunt.getFoundCount()+" of "+hunt.getTargetList().size()+" items in the scavenger hunt, and have been at it for "+hunt.getElapsedTime()/60000+" minutes.");
    		}
       	}
    	else {
    		setContentView(R.layout.sh_activity_unstarted);		
    	}
    }


	
}
