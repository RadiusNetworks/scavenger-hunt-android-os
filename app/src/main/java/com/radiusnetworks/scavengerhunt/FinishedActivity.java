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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;


/**
 * @author dyoung
 *
 * This activity displays the status of the scavenger hunt after completion, which can include
 * two few separate layouts:
 * - finished (user has found all the targets)
 * - redeemed (user has shown the completion code)
 */
public class FinishedActivity extends Activity  {
	public static final String TAG = "FinishedActivity";
    private ScavengerHuntApplication application;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        application = (ScavengerHuntApplication) this.getApplication();
        setContentView(R.layout.sh_activity_finished);

        //custom images
        ImageView imageView =  (ImageView) this.findViewById(R.id.imageView);
        Bitmap bitmap = application.getCustomAssetCache().getBitmapByName("finish_image");
        if (bitmap != null)
            imageView.setImageBitmap(bitmap);
        else Log.e(TAG, "custom finished screen logo == null when pulled from file.");

        //setting custom text and background color
        Map<String,String> customStartScreenData = application.getHunt().getCustomStartScreenData();

        //setting custom background color
        String colorHex = application.getHunt().getCustomStartScreenData().get("finish_background_color");
        colorHex = colorHex.replaceAll("0x", "#");
        int color = Color.parseColor(colorHex);
        getWindow().getDecorView().setBackgroundColor(color);

        ((TextView) this.findViewById(R.id.sh_textView1)).setText(customStartScreenData.get("finish_text_1"));
        ((Button) this.findViewById(R.id.sh_redeem_button)).setText(customStartScreenData.get("finish_button_name"));

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
        setContentView(R.layout.sh_activity_finished);
    }
	

	public void onResetClicked(View view) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	Hunt hunt = application.getHunt();
    	if (hunt.everythingFound()) {
    		builder.setTitle(getString(R.string.sh_finishedactivity_dialog_completed_title));
    		builder.setMessage(getString(R.string.sh_finishedactivity_dialog_completed_message));
    	}
    	else {
    		builder.setTitle(getString(R.string.sh_finishedactivity_dialog_reset_title));
    		builder.setMessage(getString(R.string.sh_finishedactivity_dialog_reset_message));
    	}
		builder.setPositiveButton(android.R.string.cancel, null);
		builder.setNegativeButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
                application.startOver(FinishedActivity.this);
			}
			
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		
	}

	public void onRedeemClicked(View view) {
		setContentView(R.layout.sh_activity_redeemed);

        //setting custom background color
        String colorHex = application.getHunt().getCustomStartScreenData().get("finish_background_color");
        colorHex = colorHex.replaceAll("0x", "#");
        int color = Color.parseColor(colorHex);
        getWindow().getDecorView().setBackgroundColor(color);

        ((TextView)findViewById(R.id.sh_redemption_text)).setText(
                getString(R.string.sh_finishedactivity_tv_redemptiontext_replacement) + application.getHunt().getDeviceUuid());


	}

	
}
