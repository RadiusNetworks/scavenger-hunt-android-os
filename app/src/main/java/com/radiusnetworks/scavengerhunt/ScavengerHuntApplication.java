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
import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.client.DataProviderException;
import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.ProximityKitNotifier;
import com.radiusnetworks.proximity.ibeacon.powersave.BackgroundPowerSaver;
import com.radiusnetworks.proximity.licensing.PropertiesFile;
import com.radiusnetworks.proximity.model.KitIBeacon;
import com.radiusnetworks.scavengerhunt.assets.AssetFetcherCallback;
import com.radiusnetworks.scavengerhunt.assets.CustomAssetCache;
import com.radiusnetworks.scavengerhunt.assets.RemoteAssetCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dyoung on 1/24/14.
 *
 * This is the central application class for the Scavenger Hunt.  It is responsible for:
 * 1. Initializing ProxomityKit, which downloads all the iBeacons associated with the hunt
 *    along with their configured hunt_id values and image_url values.  It then starts
 *    ranging and monitoring for these iBeacons, and will continue to do so even across
 *    a reboot.
 * 2. Downloads all the scavenger hunt badges ("target images") needed for both the found and
 *    not found states of each target.
 * 3. Updates the LoadingActivity with the status of the above download state.
 * 4. Once loading completes, launches the TargetCollectionActivity which is the main screen for the
 *    hunt.
 * 5. Handles all ranging and monitoring callbacks for iBeacons.  When an iBeacon is ranged,
 *    this class checks to see if it matches a scavenger hunt target and awards a badge if it is
 *    close enough.  If an iBeacon comes into view, it sends a notification that a target is nearby.
 */


public class ScavengerHuntApplication extends Application implements ProximityKitNotifier {

    private static final String TAG = "ScavengerHuntApplication";
    private static final Double MINIMUM_TRIGGER_DISTANCE_METERS = 10.0;
    private static final long REPEAT_NOTIF_RESTRICTED_PERIOD_MSECS = 300000;
    private ProximityKitManager manager;
    private Hunt hunt;
    private final long[] VIBRATOR_PATTERN = {0l, 500l, 0l}; // start immediately, vigrate for 500ms, sleep for 0ms
    @SuppressWarnings("unused")
    private BackgroundPowerSaver backgroundPowerSaver;
    private TargetCollectionActivity collectionActivity = null;
    private TargetItemActivity itemActivity = null;
    private LoadingActivity loadingActivity = null;
    private InstructionActivity instructionActivity = null;
    private RemoteAssetCache remoteAssetCache;
    private CustomAssetCache customAssetCache;
    private String loadingFailedTitle;
    private String loadingFailedMessage;
    private boolean codeNeeded;
    private boolean ignoreSync = true;
    int startCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate.  Hunt is "+hunt);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        manager = ProximityKitManager.getInstanceForApplication(this);
        manager.setNotifier(this);

        // Include IBeacon logs and shorten background polling
        manager.getIBeaconManager().setDebug(true);
        manager.getIBeaconManager().setBackgroundBetweenScanPeriod(30000);

        if (!new PropertiesFile().exists()) {
            this.codeNeeded = true;
            return;
        }
        else {
            Log.d(TAG,"code not needed.");
        }
    }

    public void startPk(String code, boolean resume) {
        Log.d(TAG, "startPk called with code "+code );

        if (code != null) {
            if (resume) {
              ignoreSync = true;
            }
            else {
                ignoreSync = false;
            }
            manager.restart(code);
        }
        else {
            ignoreSync = false;
            manager.start(); // This starts ranging and monitoring for iBeacons defined in ProximityKit\
        }
    }
    public boolean canResume() {
        hunt = Hunt.loadFromPreferences(this);
        remoteAssetCache = new RemoteAssetCache(this);
        Log.d(TAG, "hunt loaded from preferences at boot: "+hunt);
        return (hunt!= null && hunt.getTargetList().size() > 0 && validateRequiredImagesPresent());
    }

    public void startOver(Activity activity, boolean forceCodeReentry) {
        if (!new PropertiesFile().exists()) {
        }
        else {
            Log.d(TAG, "starting over");
            hunt.reset();
            hunt.saveToPreferences(this);
        }

        cancelAllNotifications();

        if (this.collectionActivity != null) {
            this.collectionActivity.finish();  // do this so it won't show up again on back press
            this.collectionActivity = null;
        }

        Intent intent;
        if (forceCodeReentry) {
            this.codeNeeded = true;
            Log.d(TAG, "clearing shared preferences");
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            String code = settings.getString("code", null);
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            editor.putString("code", code);
            editor.commit();
            hunt = null;
            remoteAssetCache = new RemoteAssetCache(this);
            remoteAssetCache.clear();
            customAssetCache = new CustomAssetCache(this);
            customAssetCache.clear();
            this.codeNeeded = true;
            intent = new Intent(activity, LoadingActivity.class);
        }
        else {
            hunt.reset();
            hunt.start();
            intent = new Intent(activity, TargetCollectionActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void setLoadingActivity(LoadingActivity activity) {
        this.loadingActivity = activity;
        if (this.loadingFailedTitle != null) {
            showFailedErrorMessage();
        }
    }

    public void setInstructionActivity(InstructionActivity activity) {
        this.instructionActivity = activity;
    }


    // if loading dependencies fails, we want to display a message to the user
    // we can only do so if the loading activity has already been created
    // otherwise, we store the messages for display a second or so later
    // when that acdtivity finally launches
    private void dependencyLoadingFailed(String title, String message) {
        Log.d(TAG, "dependencyLoadingFailed");
        this.loadingFailedTitle = title;
        this.loadingFailedMessage = message;
        if (this.loadingActivity != null) {
            showFailedErrorMessage();
        }
    }

    // actually get the loading activity to display an error message to the user
    private void showFailedErrorMessage() {
        Log.d(TAG, "showFailedErrorMesage");
        loadingActivity.failAndTryAgain(loadingFailedTitle, loadingFailedMessage);
        loadingFailedTitle = null;
    }


    @Override
    public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData iBeaconData, DataProviderException e) {
        // Called every second with data from ProximityKit when an iBeacon defined in ProximityKit is nearby
        Log.d(TAG, "iBeaconDataUpdate: " + iBeacon.getProximityUuid() + " " + iBeacon.getMajor() + " " + iBeacon.getMinor());
        String huntId = null;
        Double triggerDistanceMeters = MINIMUM_TRIGGER_DISTANCE_METERS;
        if (iBeaconData != null) {
            huntId = iBeaconData.get("hunt_id");
            if (iBeaconData.get("trigger_distance")!= null) {
                triggerDistanceMeters = Double.parseDouble(iBeaconData.get("trigger_distance"));
            }
        }


        if (huntId == null) {
            Log.d(TAG, "The iBeacon I just saw is not part of the scavenger hunt, according to ProximityKit");
            return;
        }
        if (hunt == null) {
            // Hunt has not been initialized from PK yet.  Ignoring all iBeacons
            return;
        }
        TargetItem target = hunt.getTargetById(huntId);
        if (target == null) {
            Log.w(TAG, "The iBeacon I just saw has a hunt_id of " + huntId + ", but it was not part of the scavenger hunt when this app was started.");
            return;
        }


        if (hunt.getElapsedTime() != 0) {

            long timeTargetNotifLastSent = target.getTimeNotifLastSent();
            long currentTimeMsecs = System.currentTimeMillis();

            // Logic to determine when a local notification will be sent
            if ((currentTimeMsecs - timeTargetNotifLastSent) > REPEAT_NOTIF_RESTRICTED_PERIOD_MSECS
                    && hunt.allowNotification() && (isApplicationSentToBackground(this.getApplicationContext())) && !target.isFound()) {
                Log.d(TAG, "Sending notification");
                sendNotification();
                target.setTimeNotifLastSent(currentTimeMsecs);
            }

            if (iBeacon.getAccuracy() < triggerDistanceMeters && !target.isFound()) {
                Log.d(TAG, "Found an item. iBeacon.getAccuracy(): " + iBeacon.getAccuracy());
                target.setFound(true);
                hunt.saveToPreferences(this);
                if (collectionActivity != null) {
                    collectionActivity.showItemFound();
                    if (hunt.everythingFound()) {
                        // switch to FinishedActivity to show player he/she has won
                        Log.d(TAG, "game is won");
                        cancelAllNotifications();
                        Intent i = new Intent(collectionActivity, FinishedActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                    }
                }
            }
            else {
                Log.d(TAG, "null collection activity");
            }
        } else {
            Log.d(TAG, "hunt hasn't started, so all ibeacon detections are being ignored");
        }
        if (itemActivity != null) {
            itemActivity.updateDistance(iBeacon, iBeaconData);
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        // Called when one of the iBeacons defined in ProximityKit first appears
        Log.d(TAG, "didEnterRegion");
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
        if (ignoreSync) {
            Log.d(TAG, "ignoring sync");
            return;
        }
        ignoreSync = true;

        ArrayList<TargetItem> targets = new ArrayList<TargetItem>();
        Map<String, String> urlMap = new HashMap<String, String>();
        Map<String,String> customStartScreenData = new HashMap <String,String>();
        Map<String, String> customAssetUrlMap = new HashMap<String, String>();

        for (KitIBeacon iBeacon : manager.getKit().getIBeacons()) {
            String huntId = iBeacon.getAttributes().get("hunt_id");
            if (huntId != null) {
                TargetItem target = new TargetItem(huntId);

                String title = iBeacon.getAttributes().get("title");
                if (title != null) {
                    target.setTitle(title);
                    Log.d(TAG, "title = "+title);
                } else {                     Log.d(TAG, "title = null");}

                String description = iBeacon.getAttributes().get("description");
                if (description != null) {
                    target.setDescription(description);
                    Log.d(TAG, "description = "+description);
                } else {                     Log.d(TAG, "description = null");}

                targets.add(target);
                String imageUrl = iBeacon.getAttributes().get("image_url");
                if (imageUrl == null) {
                    Log.e(TAG, "ERROR: No image_url specified in ProximityKit for item with hunt_id=" + huntId);
                    loadingActivity.codeValidationFailed(new RuntimeException("No targets configured for the entered code.  At least one beacon must be configured in ProximityKit with a hunt_id key."));
                    return;
                }
                else {
                    urlMap.put("target" + huntId + "_found", variantTargetImageUrlForBaseUrlString(imageUrl, true));
                    urlMap.put("target" + huntId, variantTargetImageUrlForBaseUrlString(imageUrl, false));
                }

                String splashUrl = iBeacon.getAttributes().get("splash_url");
                Log.e(TAG, "splashUrl = "+splashUrl);



                //custom splash screen and instructions screen metadata

                String instruction_background_color = iBeacon.getAttributes().get("instruction_background_color");


                if (instruction_background_color != null){
                    Log.d(TAG,"------This hunt has a custom instruction screen because the instruction_background_color is set to "+ instruction_background_color );
                    //save custom splash screen and instructions screen for later use

                    try {
                        customStartScreenData.put("instruction_background_color", iBeacon.getAttributes().get("instruction_background_color"));
                        customStartScreenData.put("instruction_image_url", iBeacon.getAttributes().get("instruction_image_url"));
                        customStartScreenData.put("instruction_start_button_name", iBeacon.getAttributes().get("instruction_start_button_name"));
                        customStartScreenData.put("instruction_text_1", iBeacon.getAttributes().get("instruction_text_1"));
                        customStartScreenData.put("instruction_title", iBeacon.getAttributes().get("instruction_title"));
                        customStartScreenData.put("splash_url", iBeacon.getAttributes().get("splash_url"));
                        customStartScreenData.put("finish_background_color", iBeacon.getAttributes().get("finish_background_color"));
                        customStartScreenData.put("finish_image_url", iBeacon.getAttributes().get("finish_image_url"));
                        customStartScreenData.put("finish_button_name", iBeacon.getAttributes().get("finish_button_name"));
                        customStartScreenData.put("finish_text_1", iBeacon.getAttributes().get("finish_text_1"));

                        customAssetUrlMap.put("instruction_image", iBeacon.getAttributes().get("instruction_image_url"));
                        customAssetUrlMap.put("finish_image", iBeacon.getAttributes().get("finish_image_url"));
                        customAssetUrlMap.put("splash", iBeacon.getAttributes().get("splash_url"));


                    }catch(Exception e){
                        e.printStackTrace();
                        customStartScreenData = null;}

                }
            }
        }

        if (targets.size() != 0) {
            if (loadingActivity != null && loadingActivity.isValidatingCode()) {
                loadingActivity.codeValidationPassed();
            }
        }
        else {
            if (loadingActivity != null && loadingActivity.isValidatingCode()) {
                loadingActivity.codeValidationFailed(new RuntimeException("No targets configured for the entered code.  At least one beacon must be configured in ProximityKit with a hunt_id key."));
                return;
            }
        }

        // load the saved state of the hunt from the phone's persistent
        // storage
        hunt = Hunt.loadFromPreferences(this);
        boolean targetListChanged = hunt.getTargetList().size() != targets.size();
        for (TargetItem target : hunt.getTargetList() ) {
            boolean itemFound = false;
            for (TargetItem targetFromPk : targets) {
                if (targetFromPk.getId().equals(target.getId())) itemFound = true;
            }
            if (itemFound == false) {
                targetListChanged = true;
                Log.d(TAG, "Target with hunt_id="+target.getId()+" is no longer in PK.  Target list has changed.");
            }
        }
        if (customStartScreenData != null && customStartScreenData.equals(hunt.getCustomStartScreenData())){
            targetListChanged = true;
            Log.d(TAG, "customStartScreenData.equals(savedCustomData)");
        } else{                Log.d(TAG, "customStartScreenData DOES NOT EQUAL savedCustomData"); }

        if (targetListChanged) {
            Log.w(TAG, "the targets in the hunt has changed from what we have in the settings.  starting over");
            this.hunt = (customStartScreenData == null)? new Hunt(this,targets) : new Hunt(this,targets,customStartScreenData) ;
            this.hunt.saveToPreferences(this);
        }

        customAssetCache = new CustomAssetCache(this);
        customAssetCache.downloadCustomAssets(customAssetUrlMap, new AssetFetcherCallback() {
            @Override
            public void requestComplete() {
                Log.i(TAG,"custom assets downloaded successfully");
            }

            @Override
            public void requestFailed(Integer responseCode, Exception e) {
                Log.e(TAG, "Failed to download the custom assets.");
            }
        });
        // After we have all our data from ProximityKit, we need to download the images and cache them
        // for display in the app.  We do this every time, so that the app can update the images after
        // later, and have users get the update if they restart the app.  This takes time, so if you
        // don't want to do this, then only execute this code if validateRequiredImagesPresent()
        // returns false.
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
        if (loadingActivity != null && loadingActivity.isValidatingCode()) {
            Log.w(TAG, "proximityKit didFailSync due to " + e + "  bad code entered?");
            loadingActivity.codeValidationFailed(e);
            return;
        }
        Log.w(TAG, "proximityKit didFailSync due to " + e + "  We may be offline.");
        hunt = Hunt.loadFromPreferences(this);
        this.dependencyLoadFinished();
    }

    // This method is called when we have tried to download all dependencies (ProximityKit data and
    // all target images.)  This may or may not have failed, so we check that everything loaded
    // properly.
    public void dependencyLoadFinished() {
        Log.d(TAG, "all dependencies loaded");
        if (ProximityKitManager.getInstanceForApplication(this).getKit() == null || hunt.getTargetList().size() == 0) {
            dependencyLoadingFailed("Network error", "Can't access scavenger hunt data.  Please verify your network connection and try again.");
            return;
        }

        if (validateRequiredImagesPresent()) {
            // Yes, we have everything we need to start up.
            this.hunt.start();
            Intent i;

            if (hunt.hasCustomStartScreen()) {
                i = new Intent(this, InstructionActivity.class);
            }
            else {
                i = new Intent(this, TargetCollectionActivity.class);
            }

            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            this.loadingActivity.finish(); // do this so that if we hit back, the loading activity won't show up again
            return;
        } else {
            dependencyLoadingFailed("Network error", "Can't download images.  Please verify your network connection and try again.");
            return;
        }
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

    public boolean isCodeNeeded() { return this.codeNeeded; }


    public RemoteAssetCache getRemoteAssetCache() {
        return remoteAssetCache;
    }

    public CustomAssetCache getCustomAssetCache() {
        return customAssetCache;
    }

    // Checks to see that one found and one not found image has been downloaded for each target
    private boolean validateRequiredImagesPresent() {
        Log.d(TAG, "Validating required images are present");
        boolean missing = false;
        for (TargetItem target : hunt.getTargetList()) {
            if (remoteAssetCache.getImageByName("target" + target.getId()) == null) {
                missing = true;
            }
            if (remoteAssetCache.getImageByName("target" + target.getId() + "_found") == null) {
                missing = true;
            }
        }
        return !missing;
    }

    /*
     Sends a notification to the user when a scavenger hunt beacon is nearby.
     */
    private void sendNotification() {
        try {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setContentTitle(getString(R.string.sh_notification_title))
                            .setContentText(getString(R.string.sh_notification_text))
                            .setVibrate(VIBRATOR_PATTERN)
                            .setSmallIcon(R.drawable.sh_notification_icon)
                            .setAutoCancel(true);


            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            Intent notificationIntent = new Intent(this, TargetCollectionActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            stackBuilder.addNextIntent(notificationIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            builder.setContentIntent(resultPendingIntent);
            NotificationManager notificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void cancelAllNotifications(){
        try {
            NotificationManager notificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /*
     Converts a target image URL to a variant needed for this platform.  The variant URL might
     add a suffix indicating a larger size for the image.  A suffix is also added if the target
     is found.

     So a URL like this:

     http://mysite.com/target1.jpg

     might become:

     http://mysite.com/target1_found_624.jpg

     */
    private String variantTargetImageUrlForBaseUrlString(String baseUrlString, boolean found) {

        int extensionIndex = baseUrlString.lastIndexOf(".");
        if (extensionIndex == -1) {
            return null;
        }
        Log.d(TAG, "Extension Index of " + baseUrlString + " is " + extensionIndex);

        String extension = baseUrlString.substring(extensionIndex);
        String prefix = baseUrlString.substring(0, extensionIndex);
        String suffix;
        if (found) {
            suffix = "_found";
        } else {
            suffix = "";
        }
        float screenWidthPixels = getResources().getDisplayMetrics().widthPixels;
        Log.d(TAG, "Screen width is "+screenWidthPixels+" and density is "+getResources().getDisplayMetrics().density);

        if (screenWidthPixels > 1040*2*1.2) {
            suffix += "_1040";
        }
        else if (screenWidthPixels > 624*2*1.2) {
            suffix += "_624";
        }
        else if (screenWidthPixels > 438*2*1.2) {
            suffix += "_438";
        }
        else if (screenWidthPixels > 312*2*1.2) {
            suffix += "_312";
        }
        else if (screenWidthPixels > 260*2*1.2) {
            suffix += "_260";
        }


        return prefix + suffix + extension;
    }


    public static boolean isApplicationSentToBackground(final Context context) {
        if (context != null){
            ActivityManager am = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (!tasks.isEmpty()) {
                ComponentName topActivity = tasks.get(0).topActivity;
                if (!topActivity.getPackageName().equals(context.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
