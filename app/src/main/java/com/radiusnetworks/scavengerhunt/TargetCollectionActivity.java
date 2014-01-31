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

import com.radiusnetworks.ibeacon.IBeaconManager;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.Toast;

/**
 * Activity displays all the badge icons in the scavenger hunt and allows the
 * user to tap on them to display estimated distance
 */
public class TargetCollectionActivity extends Activity  {
	public static final String TAG = "TargetCollectionActivity";
	public static final String NOT_FOUND_SUFFIX = "_grey";
	
	private GridView gridView = null;
	private BaseAdapter adapter = null;
	private IBeaconManager iBeaconService = IBeaconManager.getInstanceForApplication(this);
	private String screenSizeSuffix = ""; // blank for phones, "_tablet" for big screens
    private ScavengerHuntApplication application;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
        application = (ScavengerHuntApplication) this.getApplication();

		setContentView(R.layout.sh_activity_target_collection);		
		gridView = (GridView) findViewById(R.id.sh_grid_view);
		adapter = new TargetImageCollectionAdapter(this);
		gridView.setAdapter(adapter);
		
		gridView.setOnItemClickListener(itemClickListener);

		if (getScreenWidthInDps() >= 600) {
			screenSizeSuffix = "_tablet";
		}
        application.setCollectionActivity(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	@Override 
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
        application.setCollectionActivity(null);
	}


	private OnItemClickListener itemClickListener = new OnItemClickListener() {
	    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	    	Log.d(TAG,"Position tapped ["+position+"]");
			Intent i = new Intent(getApplicationContext(), TargetItemActivity.class);
			TargetItem target = application.getHunt().getTargetList().get(position);

            i.putExtra("hunt_id", target.getId());
		    startActivity(i);
	    }
	};
	
	public void showItemFound() {
    	runOnUiThread(new Runnable() {
      	     public void run() {
      			Log.i(TAG, "force refresh");
      			adapter.notifyDataSetChanged();
      			gridView.invalidateViews();
      			Toast toast = Toast.makeText(TargetCollectionActivity.this, "You've received badge "+application.getHunt().getFoundCount()+" of "+application.getHunt().getTargetList().size(), Toast.LENGTH_SHORT);
      			toast.show();      	    	 
      	     }
    	});

	}


	private class TargetImageCollectionAdapter extends  BaseAdapter {

	    private Context mContext;

	    public TargetImageCollectionAdapter(Context c) {
	        mContext = c;
	    }	    
		@Override
		public int getCount() {
			return application.getHunt().getTargetList().size();
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TargetItem target = application.getHunt().getTargetList().get(position);

			if (!target.isFound()) {
                return application.getRemoteAssetCache().getImageByName("target"+target.getId());
			} 
			else {
                return application.getRemoteAssetCache().getImageByName("target"+target.getId()+"_found");
			}
		}
		
	}

	private int getScreenWidthInDps() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float dp_w = ( metrics.widthPixels * 160 ) / metrics.xdpi;        
        return (int) dp_w;		
	}
	
}
