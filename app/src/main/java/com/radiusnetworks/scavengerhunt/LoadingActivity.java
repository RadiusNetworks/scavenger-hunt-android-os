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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * this Activity displays a loading spinner while downloading
 * dependencies from the network.
 *
 * Created by dyoung on 1/28/14.
 */
public class LoadingActivity extends Activity {
    public static final String TAG = "LoadingActivity";
    private ScavengerHuntApplication application = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sh_activity_loading);
        application = (ScavengerHuntApplication) this.getApplication();
        Log.d(TAG, "setting loading activity");
        application.onLoadingActivityCreated(this);
        /*
         Note: per the license of this project, if you are redistributing this software you must
         include an attribution on the first screen, and have it link to the URL below.
         The attribution must include the name "Radius Networks".  We request the sentence,
         "Powered by Radius Networks".
         */
        TextView textView = (TextView) this.findViewById(R.id.attribution);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://developer.radiusnetworks.com/scavenger_hunt"));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void failAndTerminate(final String title, final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        LoadingActivity.this.finish();
                    }

                });
                builder.show();

            }
        });
    }
}
