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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.radiusnetworks.ibeacon.IBeaconManager;

/**
 * Activity displays all the badge icons in the scavenger hunt and allows the
 * user to tap on them to display estimated distance
 */
public class TargetCollectionActivity extends Activity  {
	public static final String TAG = "TargetCollectionActivity";

	private GridView gridView = null;
	private BaseAdapter adapter = null;
	private IBeaconManager iBeaconService = IBeaconManager.getInstanceForApplication(this);
    private ScavengerHuntApplication application;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
        application = (ScavengerHuntApplication) this.getApplication();

		setContentView(R.layout.sh_activity_target_collection);
		gridView = (GridView) findViewById(R.id.sh_grid_view);

        Log.d(TAG, "getting image");
        ImageView view = application.getRemoteAssetCache().getImageByName("target1");

		adapter = new TargetImageCollectionAdapter(this);
		gridView.setAdapter(adapter);
		
		gridView.setOnItemClickListener(itemClickListener);

        application.setCollectionActivity(this);
        application.getHunt().sortTargetList();

        View button = (TextView) this.findViewById(R.id.start_over_button);
        button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(TargetCollectionActivity.this);
                builder.setTitle("Are you sure?");	// Are you sure you are finished?  You will not be allowed to return to this page
                builder.setMessage("All found locations will be cleared.");
                builder.setPositiveButton(android.R.string.cancel, null);
                builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        application.startOver(TargetCollectionActivity.this);
                    }

                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
	}

	@Override
    public void onBackPressed() {
        finish();
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
      			Toast toast = Toast.makeText(TargetCollectionActivity.this, getString(R.string.sh_targetcollectionactivity_found_line1) +//"You've received badge "+
                        application.getHunt().getFoundCount()+  getString(R.string.sh_targetcollectionactivity_found_line2) + //" of "+
                        application.getHunt().getTargetList().size(), Toast.LENGTH_SHORT);
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
		public Object getItem(int position) {
            return application.getHunt().getTargetList().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView != null) {
                Log.d(TAG, "convertview height is "+convertView.getHeight());
            }
			TargetItem target = application.getHunt().getTargetList().get(position);

            ImageView view;
            Double borderSize = 10.0;

			if (!target.isFound()) {
                view = application.getRemoteAssetCache().getImageByName("target"+target.getId(), (double) (gridView.getColumnWidth()-borderSize));
			} 
			else {
                view =application.getRemoteAssetCache().getImageByName("target"+target.getId()+"_found", (double) (gridView.getColumnWidth()-borderSize));
			}
            // image layout adjustment
            view.setMinimumHeight(gridView.getColumnWidth());
            view.setMinimumWidth(gridView.getColumnWidth());

            return view;


		}
		
	}

}
