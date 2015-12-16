/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.recents.model;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;

import java.util.ArrayList;
import java.util.Objects;


/**
 * A task represents the top most task in the system's task stack.
 */
public class Task {
    /* Task callbacks */
    public interface TaskCallbacks {
        /* Notifies when a task has been bound */
        public void onTaskDataLoaded(Task task);
        /* Notifies when a task has been unbound */
        public void onTaskDataUnloaded();
        /* Notifies when a task's stack id has changed. */
        public void onTaskStackIdChanged();
    }

    /* The Task Key represents the unique primary key for the task */
    public static class TaskKey {
        public final int id;
        public int stackId;
        public final Intent baseIntent;
        public final int userId;
        public long firstActiveTime;
        public long lastActiveTime;

        public TaskKey(int id, int stackId, Intent intent, int userId, long firstActiveTime,
                long lastActiveTime) {
            this.id = id;
            this.stackId = stackId;
            this.baseIntent = intent;
            this.userId = userId;
            this.firstActiveTime = firstActiveTime;
            this.lastActiveTime = lastActiveTime;
        }

        public ComponentName getComponent() {
            return this.baseIntent.getComponent();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TaskKey)) {
                return false;
            }
            TaskKey otherKey = (TaskKey) o;
            return id == otherKey.id && stackId == otherKey.stackId && userId == otherKey.userId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, stackId, userId);
        }

        @Override
        public String toString() {
            return "Task.Key: " + id + ", "
                    + "s: " + stackId + ", "
                    + "u: " + userId + ", "
                    + "lat: " + lastActiveTime + ", "
                    + getComponent().getPackageName();
        }
    }

    public TaskKey key;
    public TaskGrouping group;
    // The taskAffiliationId is the task id of the parent task or itself if it is not affiliated with any task
    public int taskAffiliationId;
    public int taskAffiliationColor;
    public boolean isLaunchTarget;
    public Drawable applicationIcon;
    public Drawable activityIcon;
    public String contentDescription;
    public String activityLabel;
    public int colorPrimary;
    public boolean useLightOnPrimaryColor;
    public Bitmap thumbnail;
    public boolean lockToThisTask;
    public boolean lockToTaskEnabled;
    public boolean isHistorical;
    public Bitmap icon;
    public String iconFilename;
    public Rect bounds;

    private ArrayList<TaskCallbacks> mCallbacks = new ArrayList<>();

    public Task() {
        // Do nothing
    }

    public Task(TaskKey key, int taskAffiliation, int taskAffiliationColor,
                String activityTitle, String contentDescription, Drawable activityIcon,
                int colorPrimary, boolean lockToThisTask, boolean lockToTaskEnabled,
                boolean isHistorical, Bitmap icon, String iconFilename, Rect bounds) {
        boolean isInAffiliationGroup = (taskAffiliation != key.id);
        boolean hasAffiliationGroupColor = isInAffiliationGroup && (taskAffiliationColor != 0);
        this.key = key;
        this.taskAffiliationId = taskAffiliation;
        this.taskAffiliationColor = taskAffiliationColor;
        this.activityLabel = activityTitle;
        this.contentDescription = contentDescription;
        this.activityIcon = activityIcon;
        this.colorPrimary = hasAffiliationGroupColor ? taskAffiliationColor : colorPrimary;
        this.useLightOnPrimaryColor = Utilities.computeContrastBetweenColors(this.colorPrimary,
                Color.WHITE) > 3f;
        this.lockToThisTask = lockToTaskEnabled && lockToThisTask;
        this.lockToTaskEnabled = lockToTaskEnabled;
        this.isHistorical = isHistorical;
        this.icon = icon;
        this.iconFilename = iconFilename;
        this.bounds = bounds;
    }

    /** Copies the other task. */
    public void copyFrom(Task o) {
        this.key = o.key;
        this.taskAffiliationId = o.taskAffiliationId;
        this.taskAffiliationColor = o.taskAffiliationColor;
        this.activityLabel = o.activityLabel;
        this.contentDescription = o.contentDescription;
        this.activityIcon = o.activityIcon;
        this.colorPrimary = o.colorPrimary;
        this.useLightOnPrimaryColor = o.useLightOnPrimaryColor;
        this.lockToThisTask = o.lockToThisTask;
        this.lockToTaskEnabled = o.lockToTaskEnabled;
        this.isHistorical = o.isHistorical;
        this.icon = o.icon;
        this.iconFilename = o.iconFilename;
        this.bounds = o.bounds;
    }

    /**
     * Add a callback.
     */
    public void addCallback(TaskCallbacks cb) {
        if (!mCallbacks.contains(cb)) {
            mCallbacks.add(cb);
        }
    }

    /**
     * Remove a callback.
     */
    public void removeCallback(TaskCallbacks cb) {
        mCallbacks.remove(cb);
    }

    /** Set the grouping */
    public void setGroup(TaskGrouping group) {
        if (group != null && this.group != null) {
            throw new RuntimeException("This task is already assigned to a group.");
        }
        this.group = group;
    }

    /**
     * Updates the stack id of this task.
     */
    public void setStackId(int stackId) {
        key.stackId = stackId;
        int callbackCount = mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            mCallbacks.get(i).onTaskStackIdChanged();
        }
    }

    /**
     * Returns whether this task is on the freeform task stack.
     */
    public boolean isFreeformTask() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        return ssp.hasFreeformWorkspaceSupport() && ssp.isFreeformStack(key.stackId);
    }

    /** Notifies the callback listeners that this task has been loaded */
    public void notifyTaskDataLoaded(Bitmap thumbnail, Drawable applicationIcon) {
        this.applicationIcon = applicationIcon;
        this.thumbnail = thumbnail;
        int callbackCount = mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            mCallbacks.get(i).onTaskDataLoaded(this);
        }
    }

    /** Notifies the callback listeners that this task has been unloaded */
    public void notifyTaskDataUnloaded(Bitmap defaultThumbnail, Drawable defaultApplicationIcon) {
        applicationIcon = defaultApplicationIcon;
        thumbnail = defaultThumbnail;
        int callbackCount = mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            mCallbacks.get(i).onTaskDataUnloaded();
        }
    }

    /**
     * Returns whether this task is affiliated with another task.
     */
    public boolean isAffiliatedTask() {
        return key.id != taskAffiliationId;
    }

    @Override
    public boolean equals(Object o) {
        // Check that the id matches
        Task t = (Task) o;
        return key.equals(t.key);
    }

    @Override
    public String toString() {
        String groupAffiliation = "no group";
        if (group != null) {
            groupAffiliation = Integer.toString(group.affiliation);
        }
        return "Task (" + groupAffiliation + "): " + key +
                " [" + super.toString() + "]";
    }
}
