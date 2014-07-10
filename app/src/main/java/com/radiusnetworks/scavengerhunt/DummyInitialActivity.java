package com.radiusnetworks.scavengerhunt;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by dyoung on 7/9/14.
 */
public class DummyInitialActivity extends android.app.Activity {
    private static final String TAG = "DummyInitialActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "starting dummy initial activity to see what real activity we should start");
        ScavengerHuntApplication application = (ScavengerHuntApplication) this.getApplication();
        // Dynamically start which view
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String code = settings.getString("code", null);
        // If we have everything we need to resume, just do so
        if (application.canResume() && code != null) {
            Log.d(TAG, "**** RESUMING FROM WHERE WE LEFT OFF");

            application.getHunt().start();
            application.startPk(code, true); // resume with the last code
            Intent i;
            if (application.getHunt().hasCustomStartScreen()) {
                Log.d(TAG, "this does not have a custom start screen");
                i = new Intent(this, InstructionActivity.class);
            }
            else {
                Log.d(TAG, "this has a custom start screen");
                i = new Intent(this, TargetCollectionActivity.class);
            }

            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
        else {
            Log.d(TAG, "**** NOT RESUMING FROM WHERE WE LEFT OFF");
            Intent i;
            i = new Intent(this, LoadingActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
        this.finish();
    }
}
