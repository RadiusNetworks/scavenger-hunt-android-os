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
import android.os.Bundle;
import android.util.Log;

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
                        // terminate this application, and not just this acdtivity
                        // TODO: is this kosher?
                        System.exit(0);
                    }

                });
                builder.show();

            }
        });
    }
}
