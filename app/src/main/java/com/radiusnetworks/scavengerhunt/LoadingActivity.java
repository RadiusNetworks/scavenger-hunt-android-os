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
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.radiusnetworks.ibeacon.BleNotAvailableException;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.licensing.LicensingException;
import com.radiusnetworks.proximity.licensing.PropertiesFile;

import java.io.FileNotFoundException;

/**
 * this Activity displays a loading spinner while downloading
 * dependencies from the network.
 *
 * Created by dyoung on 1/28/14.
 */
public class LoadingActivity extends Activity {
    public static final String TAG = "LoadingActivity";
    private ScavengerHuntApplication application = null;
    private boolean validatingCode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (ScavengerHuntApplication) this.getApplication();
        application.setLoadingActivity(this);
        if (application.getHunt() != null && application.getHunt().getElapsedTime() > 0) {
            Log.d(TAG, "hunt is ongoing");
            // user exited after starting a hunt.  resume where he or she left off
            Intent i = new Intent(this, TargetCollectionActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
            return;
        }
        else {
            // starting from scratch
            setContentView(R.layout.sh_activity_code);
            this.findViewById(R.id.code_dialog).setVisibility(View.INVISIBLE);
            this.findViewById(R.id.validating_dialog).setVisibility(View.INVISIBLE);
        }

        View startButton = (TextView) this.findViewById(R.id.sh_start_button);
        if (startButton != null) {
            startButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (application.isCodeNeeded()) {
                        setContentView(R.layout.sh_activity_code);
                        LoadingActivity.this.findViewById(R.id.code_dialog).setVisibility(View.VISIBLE);
                        LoadingActivity.this.findViewById(R.id.validating_dialog).setVisibility(View.INVISIBLE);
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(LoadingActivity.this);
                        ((EditText) LoadingActivity.this.findViewById(R.id.code)).setText(settings.getString("code", ""));
                        TextView helpView = (TextView) LoadingActivity.this.findViewById(R.id.help);
                        helpView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Log.d(TAG, "help tapped");
                                Intent i = new Intent(getApplicationContext(), HelpActivity.class);
                                startActivity(i);
                           }
                        });

                    }
                    else {
                        setContentView(R.layout.sh_activity_loading);
                        Log.d(TAG, "setting loading activity");
                        application.startPk(null);
                    }
                }
            });
        }

        /*
         Note: per the license of this project, if you are redistributing this software you must
         include an attribution on the first screen, and have it link to the URL below.
         The attribution must include the name "Radius Networks".  We request the sentence,
         "Powered by Radius Networks".
         */
        TextView attributionView = (TextView) this.findViewById(R.id.attribution);
        attributionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://developer.radiusnetworks.com/scavenger_hunt"));
                startActivity(browserIntent);
            }
        });

        checkPrerequisites();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void onCodeOkClicked(View v) {
        this.findViewById(R.id.code).setEnabled(false);
        this.findViewById(R.id.code_dialog).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.validating_dialog).setVisibility(View.VISIBLE);

        EditText codeEditText = (EditText) this.findViewById(R.id.code);
        String code = codeEditText.getText().toString();

        this.validatingCode = true;
        // The method below will change the credentials for pk, which will then try to sync
        // sync pass or fail will cause a callback to ScavengerHuntApplication,
        // which will call the codeValidationPassed or codeValidationFailed methods below
        Log.d(TAG, "restarting proximity kit with code" + code);
        application.startPk(code);
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

    public boolean isValidatingCode() {
        Log.d(TAG, "validatingCode is "+validatingCode);
        return validatingCode;
    }

    public void codeValidationPassed() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.putString("code", ((EditText) LoadingActivity.this.findViewById(R.id.code)).getText().toString());
        editor.commit();
        this.validatingCode = false;
        runOnUiThread(new Runnable() {
            public void run() {
                LoadingActivity.this.findViewById(R.id.code).setEnabled(true);
                final AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                setContentView(R.layout.sh_activity_loading);
                Log.d(TAG, "setting loading activity");
            }
        });
    }

    public void codeValidationFailed(final Exception e) {
        this.validatingCode = false;
        runOnUiThread(new Runnable() {
            public void run() {
                LoadingActivity.this.findViewById(R.id.code_dialog).setVisibility(View.VISIBLE);
                LoadingActivity.this.findViewById(R.id.validating_dialog).setVisibility(View.INVISIBLE);
                LoadingActivity.this.findViewById(R.id.code).setEnabled(true);
                final AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                if (e.getClass() == LicensingException.class || e.getClass() == FileNotFoundException.class) {
                    builder.setTitle("Invalid code");
                    builder.setMessage("Please check that your scavenger hunt code is valid and try again.");
                }
                else {
                    builder.setTitle("Network error");
                    builder.setMessage("Please check your internet connection and try again.");
                    Log.d(TAG, "code validation error: "+e);
                }
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            }
        });

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
						/*finish();*/
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
            String message = null;

            if (wifi.isWifiEnabled()) {
                if (Build.MODEL.equals("Nexus 4") || Build.MODEL.equals("Nexus 7")) {
                    message = "There is a known issue with the Nexus 4 and Nexus 7 devices where WiFi and Bluetooth can disrupt each other.  We recommend disabling WiFi while using the Scavenger Hunt.";
                }
                if (Build.MODEL.equals("Moto G") || Build.MODEL.equals("Moto X")) {
                    message = "There is a known issue with the Moto G and Moto X devices where WiFi and Bluetooth can disrupt each other.  We recommend disabling WiFi while using the Scavenger Hunt.";
                }
            }

            if (message != null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Please Note");
                builder.setMessage(message);
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
