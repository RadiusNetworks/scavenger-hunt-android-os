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

import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.client.DataProviderException;
import com.radiusnetworks.proximity.ProximityKitNotifier;
import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.ibeacon.powersave.BackgroundPowerSaver;
import com.radiusnetworks.proximity.ibeacon.startup.RegionBootstrap;
import com.radiusnetworks.proximity.model.KitIBeacon;
import com.radiusnetworks.scavengerhunt.assets.AssetFetcherCallback;
import com.radiusnetworks.scavengerhunt.assets.RemoteAssetCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dyoung on 1/24/14.
 */
public class ScavengerHuntApplication extends Application implements ProximityKitNotifier {

    private static final String TAG = "ScavengerHuntApplication";
    private static final Double MINIMUM_TRIGGER_DISTANCE_METERS = 10.0;
    private ProximityKitManager manager;
    private Hunt hunt;
    private final long[] VIBRATOR_PATTERN = {0l, 500l, 0l}; // start immediately, vigrate for 500ms, sleep for 0ms
    @SuppressWarnings("unused")
    private RegionBootstrap regionBootstrap;
    @SuppressWarnings("unused")
    private BackgroundPowerSaver backgroundPowerSaver;
    private TargetCollectionActivity collectionActivity = null;
    private TargetItemActivity itemActivity = null;
    private LoadingActivity loadingActivity = null;
    private RemoteAssetCache remoteAssetCache;


    @Override
    public void onCreate() {
        super.onCreate();

        // TODO: make bootstrapping work with PK
        // regionBootstrap = new RegionBootstrap(this, region);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    // method gets called after the LoadingActivity starts.  We wait to start up ProximityKit until
    // our activity is displayed, that way we will be able to display notifications to the user if
    // there is a problem reaching the server
    public void onLoadingActivityCreated(LoadingActivity activity) {
        this.loadingActivity = activity;
        Log.d(TAG, "loadingActivity is now"+loadingActivity);
        manager = ProximityKitManager.getInstanceForApplication(this);
        manager.setNotifier(this);
        manager.start(); // This starts ranging and monitoring for iBeacons defined in ProximityKit
    }

    @Override
    public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData iBeaconData, DataProviderException e) {
        // Called every second with data from ProximityKit when an iBeacon defined in ProximityKit is nearby
        Log.d(TAG, "iBeaconDataUpdate: "+iBeacon.getProximityUuid()+" "+iBeacon.getMajor()+" "+iBeacon.getMinor());
        String huntId = null;
        if (iBeaconData != null) {
            huntId = iBeaconData.get("hunt_id");
        }
        if (huntId == null) {
            Log.d(TAG, "The iBeacon I just saw is not part of the scavenger hunt, according to ProximityKit");
            return;
        }
        if (hunt == null) {
            Log.d(TAG, "Hunt has not been initialized from PK yet.  Ignoring all iBeacons");
            return;
        }
        TargetItem target = hunt.getTargetById(huntId);
        if (target == null) {
            Log.d(TAG, "The iBeacon I just saw has a hunt_id of "+huntId+", but it was not part of the scavenger hunt when this app was started.");
            return;
        }


            if (hunt.getElapsedTime() != 0) {
                        if (iBeacon.getAccuracy() < MINIMUM_TRIGGER_DISTANCE_METERS && !target.isFound()) {
                            Log.d(TAG,"found an item");
                            target.setFound(true);
                            hunt.saveToPreferences(collectionActivity);
                            if (collectionActivity != null) {
                                Log.i(TAG, "calling showItemFound on collection activity");
                                collectionActivity.showItemFound();
                                if (hunt.everythingFound()) {
                                    // switch to MainActivity to show player he/she has won
                                    Log.d(TAG,"game is won");
                                    Intent i = new Intent(collectionActivity, MainActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(i);
                                }
                            }
                            else {
                                Log.i(TAG, "NOT calling showItemFound on collection activity -- because it is null");
                            }
                        }
                        else {
                            Log.d(TAG, "item "+iBeacon.getMinor()+"is not near: "+iBeacon.getProximity()+", accuracy="+iBeacon.getAccuracy());
                        }

                    }
            else {
                Log.d(TAG, "hunt hasn't started, so I am ignoring everything");
            }
            if (itemActivity != null) {
                Log.i(TAG,"got ranging info that might be for the minor being shown");
                Log.i(TAG,"updating "+itemActivity+" with accuracy value of "+iBeacon.getAccuracy());
                itemActivity.updateDistance(iBeacon, iBeaconData);

            }
    }


    @Override
    public void didEnterRegion(Region region) {
        // Called when one of the iBeacons defined in ProximityKit first appears
        Log.d(TAG, "didEnterRegion");
        if (hunt.getElapsedTime() == 0) {
            Log.d(TAG, "Not sending notification because the hunt has not been started yet.  Elapsed time is "+hunt.getElapsedTime());
        }
        else {
            Log.d(TAG, "Sending notification.");
            sendNotification();
        }
    }

    @Override
    public void didExitRegion(Region region) {
        // Called when one of the iBeacons defined in ProximityKit first disappears
        Log.d(TAG, "didExitRegion");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        // Called when one of the iBeacons defined in ProximityKit first appears or disappears
        Log.d(TAG, "didExitRegion");
    }

    @Override
    public void didSync() {
        // Called when ProximityKit data are updated from the server
        Log.d(TAG, "proximityKit didSync.  kit is " + manager.getKit());

        ArrayList<TargetItem> targets = new ArrayList<TargetItem>();
        Map<String,String> urlMap = new HashMap<String,String>();

        for (KitIBeacon iBeacon : manager.getKit().getIBeacons()) {
            String huntId = iBeacon.getAttributes().get("hunt_id");
            if (huntId != null) {
                TargetItem target = new TargetItem(huntId);
                targets.add(target);
                String imageUrl = iBeacon.getAttributes().get("image_url");
                if (imageUrl == null) {
                    Log.e(TAG, "ERROR: No image_url specified in ProximityKit for item with hunt_id="+ huntId);
                }
                urlMap.put("target"+huntId+"_found", variantTargetImageUrlForBaseUrlString(imageUrl, true));
                urlMap.put("target"+huntId, variantTargetImageUrlForBaseUrlString(imageUrl, false));
            }
        }

        // The line below will load the saved state of the hunt from the phone's preferences
        hunt = Hunt.loadFromPreferneces(this);
        if (hunt.getTargetList().size() != targets.size()) {
            Log.w(TAG, "the number of targets in the hunt has changed from what we have in the settings.  starting over");
            this.hunt = new Hunt(targets);
            this.hunt.saveToPreferences(this);
        }

        remoteAssetCache = new RemoteAssetCache(this);
        remoteAssetCache.downloadAssets(urlMap, new AssetFetcherCallback() {
            @Override
            public void requestComplete() {
                dependencyLoadFinished();
            }

            @Override
            public void requestFailed(Integer responseCode, Exception e) {
                dependencyLoadFinished();
            }
        });


    }

    @Override
    public void didFailSync(Exception e) {
        // called when ProximityKit data are requested from the server, but the request fails
        Log.w(TAG, "proximityKit didFailSync due to "+e+"  We may be offline.");
        hunt = Hunt.loadFromPreferneces(this);
        this.dependencyLoadFinished();
    }

    public void dependencyLoadFinished() {
        Log.d(TAG, "all dependencies loaded");
        if (ProximityKitManager.getInstanceForApplication(this).getKit() == null || hunt.getTargetList().size() == 0) {
            if (loadingActivity != null && !loadingActivity.isDestroyed()) {
                loadingActivity.failAndTerminate("Network error", "Can't access scavenger hunt data.  Please verify your network connection and try again.");
            }
            else {
                Log.e(TAG, "loading activity is null.  can't tell the user we are exiting.");
            }
            // TODO:  do something here to make the iBeacon service terminate

            return;
        }

        if (validateRequiredImagesPresent()) {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }
        else {
            loadingActivity.failAndTerminate("Network error", "Can't download images.  Please verify your network connection and try again.");
            return;
        }
    }

    private boolean validateRequiredImagesPresent() {
        boolean missing = false;
        for (TargetItem target: hunt.getTargetList() ) {
            if (remoteAssetCache.getImageByName("target"+target.getId()) == null) {
                missing = true;
            }
            if (remoteAssetCache.getImageByName("target"+target.getId()+"_found") == null) {
                missing = true;
            }
        }
        return !missing;
    }
    public Hunt getHunt() {
        return hunt;
    }

    public TargetCollectionActivity getCollectionActivity() {
        return collectionActivity;
    }

    public void setCollectionActivity(TargetCollectionActivity collectionActivity) {
        this.collectionActivity = collectionActivity;
    }

    public TargetItemActivity getItemActivity() {
        return itemActivity;
    }

    public void setItemActivity(TargetItemActivity itemActivity) {
        this.itemActivity = itemActivity;
    }


    public RemoteAssetCache getRemoteAssetCache() { return remoteAssetCache; }

    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Scavenger Hunt")
                        .setContentText("A scavenger hunt location is nearby.")
                        .setVibrate(VIBRATOR_PATTERN)
                        .setSmallIcon(R.drawable.sh_notification_icon);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        stackBuilder.addNextIntent(new Intent(this, TargetCollectionActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private String variantTargetImageUrlForBaseUrlString(String baseUrlString, boolean found) {

        int extensionIndex = baseUrlString.lastIndexOf(".");
        if (extensionIndex == -1) {
            return null;
        }
        Log.d(TAG, "Extension Index of "+baseUrlString+" is "+extensionIndex);

        String extension = baseUrlString.substring(extensionIndex);
        String prefix = baseUrlString.substring(0,extensionIndex);
        String suffix;
        if (found) {
            suffix = "_found";
        }
        else {
            suffix = "";
        }
        float screenWidthDp = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;

        if (screenWidthDp > 600) {
            suffix += "_tablet";
        }
        double density = getResources().getDisplayMetrics().density;
        String densityString = "_ldpi";
        if (density > 1.0) {
            densityString = "_mdpi";
        }
        if (density > 1.5) {
            densityString = "_hdpi";
        }
        if (density > 2.0) {
            densityString = "_xhdpi";
        }
        if (density > 3.0) {
            densityString = "_xxhdpi";
        }

        suffix += densityString;
        return prefix+suffix+extension;
    }

}
