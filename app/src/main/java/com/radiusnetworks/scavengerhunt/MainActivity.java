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
 */
public class MainActivity extends Activity  {
	public static final String TAG = "MainActivity";
    private ScavengerHuntApplication application;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        application = (ScavengerHuntApplication) this.getApplication();
		updateViewsForHuntState();	
		checkPrerequisites();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
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
		// Do not actually give a "Force start" option to users.  This is here just 
		// for testing on emulators
		builder.setNegativeButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
                application.getHunt().reset();
				updateViewsForHuntState();	
			}
			
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		
	}

	public void onRedeemClicked(View view) {
		setContentView(R.layout.sh_activity_redeemed);	    		
		((TextView)findViewById(R.id.sh_redemption_text)).setText("Your completion code is: "+application.getHunt().getDeviceUuid());

	}
	
	
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

	private boolean checkPrerequisites() {
		IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);

		try {
			if (!iBeaconManager.checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Bluetooth not enabled");			
				builder.setMessage("The scavenger hunt requires that Bluetooth be turned on.  Please enable bluetooth in settings.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						finish();
					}
					
				});
				builder.show();
				return false;
			}			
		}
		catch (BleNotAvailableException e) {
			return false;
		}

        try {
            WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled() && Build.MODEL.equals("Nexus 4") || Build.MODEL.equals("Nexus 7")){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Please Note");
                builder.setMessage("There is a known issue with the Nexus 4 and Nexus 7 devices where WiFi and Bluetooth can disrupt each other.  We recommend disabling WiFi while using the Scavenger Hunt.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                    }

                });
                builder.show();
                return false;

            }
        }
        catch (Exception e) {
            Log.e(TAG, "Can't access wifi manager due to exception", e);
        }
		return true;
	}
    
	
}
