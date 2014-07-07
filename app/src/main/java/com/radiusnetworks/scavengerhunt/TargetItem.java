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

/**
 * A target item in the scavenger hunt,
 * basically just a wrapper around the ProximityKit hunt_id attibute
 * value and a flag to whether or not this target has been found
 */
public class TargetItem implements Comparable<TargetItem> {
    private String id;
    private String title;
    private String description;
    private long timeNotifLastSent;
	private boolean found;

	public TargetItem(String id) {
        this.id=id;
        this.title = "";
        this.description = "";
        this.timeNotifLastSent = 0;
	}
    public TargetItem(String id, String title, String description) {
        this.id=id;
        this.title = title;
        this.description = description;
        this.timeNotifLastSent = 0;
    }

	public String getId() {
		return id;
	}
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }
    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public long getTimeNotifLastSent() {
        return timeNotifLastSent;
    }

	public boolean isFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}

    public void setTimeNotifLastSent(long newTimeNotifLastSent) {
        this.timeNotifLastSent = newTimeNotifLastSent;
    }

    @Override
    public int compareTo(TargetItem targetItem) {
        return this.id.compareTo(targetItem.id);
    }
}
