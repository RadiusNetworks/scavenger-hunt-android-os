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
import java.util.Iterator;
import java.util.List;

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

    public Hunt(Context context, List<TargetItem> targets) {
        this.targetList = targets;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.deviceUuid = settings.getString("sh_device_uuid", this.deviceUuid == null ? java.util.UUID.randomUUID().toString() : this.deviceUuid);
    }

    private Hunt() {
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

        editor.commit();
    }

    public static Hunt loadFromPreferences(Context context) {
        Hunt hunt = new Hunt();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        hunt.startTime = settings.getLong("sh_start_time", hunt.startTime);
        hunt.deviceUuid = settings.getString("sh_device_uuid", hunt.deviceUuid == null ? java.util.UUID.randomUUID().toString() : hunt.deviceUuid);

        String targetIdsFound = settings.getString("sh_target_ids_found", "");
        String targetIds = settings.getString("sh_target_ids", "");
        String targetTitles = settings.getString("sh_target_titles", "");
        String targetDescriptions = settings.getString("sh_target_descriptions", "");
        List<String> foundTargetIdList = Arrays.asList(targetIdsFound.split(","));
        Log.d(TAG, "device uuid is "+hunt.deviceUuid);

        hunt.targetList = new ArrayList<TargetItem>();
        String[] titlesStrings = targetTitles.split("|");
        String[] descriptionsStrings = targetDescriptions.split("|");
        int i = 0;
        for (String targetId : targetIds.split("|")) {
            hunt.targetList.add(new TargetItem(targetId, titlesStrings[i], descriptionsStrings[i]));
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
        return hunt;
    }

}
