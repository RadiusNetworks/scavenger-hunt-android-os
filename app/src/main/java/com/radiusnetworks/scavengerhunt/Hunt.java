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


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manages the state of the scavenger hunt, what targets are found, how long it has been in progress,
 * etc.
 */
public class Hunt {
    private static final String TAG = "Hunt";
    private long startTime = 0;
    private static final long NOTIF_RESTRICTED_PERIOD_MSECS = 60000;
    private long completedTime;
    private String deviceUuid;
    private List<TargetItem> targetList;
    private Map<String,String> customStartScreenData;

    private Hunt() {
    }

    public Hunt(Context context, List<TargetItem> targets) {
        this.targetList = targets;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.deviceUuid = settings.getString("sh_device_uuid", this.deviceUuid == null ? java.util.UUID.randomUUID().toString() : this.deviceUuid);
    }

    public Hunt(Context context, List<TargetItem> targets, Map<String,String> customStartScreenData) {
        this.targetList = targets;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.deviceUuid = settings.getString("sh_device_uuid", this.deviceUuid == null ? java.util.UUID.randomUUID().toString() : this.deviceUuid);
        this.customStartScreenData = customStartScreenData;
    }

    public Map<String,String> getCustomStartScreenData(){
        return this.customStartScreenData;
    }
    public void setCustomStartScreenData(Map<String,String>customStartScreenData){
        this.customStartScreenData =customStartScreenData;
    }

    public TargetItem getTargetById(String id) {
        for (TargetItem target : this.targetList) {
            if (target.getId().equals(id)) {
                return target;
            }
        }
        return null;
    }

    public void reset() {
        this.startTime = 0;
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            target.setFound(false);
            target.setTimeNotifLastSent(0l);
        }
    }

    public void start() {
        this.startTime = (new Date()).getTime();
    }

    public long getElapsedTime() {
        if (this.startTime == 0) {
            return 0;
        }
        return (new Date()).getTime() - this.startTime;
    }

    public int getFoundCount() {
        int count = 0;
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            if (target.isFound()) {
                count++;
            }
        }
        return count;
    }

    public boolean everythingFound() {
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            if (!target.isFound()) {
                return false;
            }
        }
        return true;
    }

    public boolean allowNotification() {
        long currentTimeMsecs = System.currentTimeMillis();
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            if ((currentTimeMsecs - target.getTimeNotifLastSent()) < NOTIF_RESTRICTED_PERIOD_MSECS) {
                return false;
            }
        }
        return true;
    }


    public List<TargetItem> getTargetList() {
        return this.targetList;
    }

    public void sortTargetList() {
        Collections.sort(this.targetList);
    }

    public String getDeviceUuid() {
        return this.deviceUuid;
    }

    // make a string csv of target ids found for easy saving to preferences
    private String getTargetIdsFound() {
        StringBuilder idsFound = new StringBuilder();
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            if (target.isFound()) {
                if (idsFound.length() != 0) {
                    idsFound.append("|");
                }
                idsFound.append(target.getId());
            }
        }
        return idsFound.toString();
    }

    // make a string csv of target ids for easy saving to preferences
    private String getTargetIds() {
        StringBuilder ids = new StringBuilder();
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            if (ids.length() != 0) {
                ids.append("|");
            }
            ids.append(target.getId());
        }
        return ids.toString();
    }

    // make a string csv of target ids for easy saving to preferences
    private String getTargetTitles() {
        StringBuilder ids = new StringBuilder();
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            if (ids.length() != 0) {
                ids.append("|");
            }
            ids.append(target.getTitle());
        }
        return ids.toString();
    }

    // make a string csv of target ids for easy saving to preferences
    private String getTargetDescriptions() {
        StringBuilder ids = new StringBuilder();
        Iterator<TargetItem> i = this.targetList.iterator();
        while (i.hasNext()) {
            TargetItem target = i.next();
            if (ids.length() != 0) {
                ids.append("|");
            }
            ids.append(target.getDescription());
        }
        return ids.toString();
    }

    public void saveToPreferences(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("sh_start_time", this.startTime);
        editor.putString("sh_device_uuid", this.deviceUuid);
        editor.putString("sh_target_ids", getTargetIds());
        editor.putString("sh_target_ids_found", getTargetIdsFound());
        editor.putString("sh_target_titles", getTargetTitles());
        editor.putString("sh_target_descriptions", getTargetDescriptions());

        if (hasCustomStartScreen()){
            editor.putString("instruction_background_color", this.customStartScreenData.get("instruction_background_color"));
            editor.putString("instruction_image_url", this.customStartScreenData.get("instruction_image_url"));
            editor.putString("instruction_start_button_name", this.customStartScreenData.get("instruction_start_button_name"));
            editor.putString("instruction_text_1", this.customStartScreenData.get("instruction_text_1"));
            editor.putString("instruction_title", this.customStartScreenData.get("instruction_title"));
            editor.putString("splash_url", this.customStartScreenData.get("splash_url"));
            editor.putString("finish_background_color", this.customStartScreenData.get("finish_background_color"));
            editor.putString("finish_image_url", this.customStartScreenData.get("finish_image_url"));
            editor.putString("finish_button_name", this.customStartScreenData.get("finish_button_name"));
            editor.putString("finish_text_1", this.customStartScreenData.get("finish_text_1"));

            //note images should already be saved to file
        }

        editor.commit();
    }

    public static Hunt loadFromPreferences(Context context) {
        Hunt hunt = new Hunt();
        hunt.targetList = new ArrayList<TargetItem>();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        hunt.startTime = settings.getLong("sh_start_time", hunt.startTime);
        hunt.deviceUuid = settings.getString("sh_device_uuid", hunt.deviceUuid == null ? java.util.UUID.randomUUID().toString() : hunt.deviceUuid);

        String targetIdsFound = settings.getString("sh_target_ids_found", "");
        String targetIds = settings.getString("sh_target_ids", "");
        if (targetIds.equals("")) {
            Log.d(TAG, "there are no target ids in preferences, so we will not consider the hunt to exist");
        }
        String targetTitles = settings.getString("sh_target_titles", "");
        String targetDescriptions = settings.getString("sh_target_descriptions", "");
        List<String> foundTargetIdList = Arrays.asList(targetIdsFound.split(","));
        Log.d(TAG, "device uuid is "+hunt.deviceUuid);

        hunt.targetList = new ArrayList<TargetItem>();
        String[] titlesStrings = targetTitles.split("\\|");
        String[] descriptionsStrings = targetDescriptions.split("\\|");
        int i = 0;
        Log.d(TAG, "targetIds:"+targetIds);
        for (String targetId : targetIds.split("\\|")) {
            Log.d(TAG, "processing targetId: "+targetId);
            String title = null;
            String description = null;
            if (titlesStrings.length > i) {
                title = titlesStrings[i];
            }
            if (descriptionsStrings.length > i) {
                description = descriptionsStrings[i];
            }

            hunt.targetList.add(new TargetItem(targetId, title, description));
            i++;
        }

        for (String foundTargetId : foundTargetIdList) {
            for (TargetItem target : hunt.targetList) {
                if (target.getId().equals(foundTargetId)) {
                    target.setFound(true);
                    Log.d(TAG, "Setting target " + target.getId() + " to found based on saved setting");
                }
            }
        }


        Map<String,String> loadedCustomData = new HashMap<String, String>();
        loadedCustomData.put("instruction_background_color",settings.getString("instruction_background_color",""));
        loadedCustomData.put("instruction_image_url",settings.getString("instruction_image_url",""));
        loadedCustomData.put("instruction_start_button_name",settings.getString("instruction_start_button_name",""));
        loadedCustomData.put("instruction_text_1",settings.getString("instruction_text_1",""));
        loadedCustomData.put("instruction_title",settings.getString("instruction_title",""));
        loadedCustomData.put("splash_url",settings.getString("splash_url",""));
        loadedCustomData.put("finish_background_color",settings.getString("finish_background_color",""));
        loadedCustomData.put("finish_image_url",settings.getString("finish_image_url",""));
        loadedCustomData.put("finish_button_name",settings.getString("finish_button_name",""));
        loadedCustomData.put("finish_text_1",settings.getString("finish_text_1",""));
        //Note: custom images should already be saved to file, and will be loaded when needed

        if (loadedCustomData.get("instruction_background_color") != "")
            hunt.setCustomStartScreenData(loadedCustomData);

        return hunt;
    }

    public boolean hasCustomStartScreen() {
        if (customStartScreenData != null && (!customStartScreenData.isEmpty())) {
            Log.e(TAG, "This hunt has a custom start screen because customStartScreenData.isEmpty(): " + customStartScreenData.isEmpty());
            return true;
        }
        return false;
    }

}
