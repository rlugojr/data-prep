// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.lock.store;

import java.time.Instant;

/**
 * Represents a user locked-Resource. The lock will be released at a specified epoch time or a after a
 * default delay time of 10 minutes has expired.
 */
public class LockedResource {

    /**
     * The default delay before a lock is silently released
     */
    public static final long DEFAULT_LOCK_DELAY = 1L * 60;

    /**
     * The identifier of the user who is locking the resource
     */
    private String userId;

    /**
     * The identifier of the locked resource
     */
    private String resourceId;

    /**
     * The time when the lock will be released
     */
    private long expirationTime;

    /**
     * Constructs a locked resource.
     * 
     * @param resourceId the specified resource identifier
     * @param userId the specified user identifier
     */
    public LockedResource(String resourceId, String userId) {
        this.resourceId = resourceId;
        this.userId = userId;
        this.expirationTime = Instant.now().getEpochSecond() + DEFAULT_LOCK_DELAY;
    }

    /**
     * Constructs a locked resource with the specified lock delay
     *
     * @param resourceId the specified resource identifier
     * @param userId the specified user identifier
     * @param delay the specified lock delay
     */
    public LockedResource(String resourceId, String userId, long delay) {
        this.resourceId = resourceId;
        this.userId = userId;
        this.expirationTime = Instant.now().getEpochSecond() + delay;
    }

    /**
     * Constructs a locked resource from the specified one with the default lock delay
     * 
     * @param lockedResource the specified locked resource
     */
    public LockedResource(LockedResource lockedResource) {
        this.resourceId = lockedResource.getResourceId();
        this.userId = lockedResource.getUserId();
        this.expirationTime = Instant.now().getEpochSecond() + DEFAULT_LOCK_DELAY;
    }

    /**
     * Constructs a locked resource from the specified one with the default lock delay
     * 
     * @param lockedResource the specified locked resource
     * @param delay the specified lock delay
     */
    public LockedResource(LockedResource lockedResource, long delay) {
        this.resourceId = lockedResource.getResourceId();
        this.userId = lockedResource.getUserId();
        this.expirationTime = delay;
    }

    /**
     * NO argument constructor for Jackson
     */
    public LockedResource(){
        
    }

    /**
     *
     * @return the identifier of the user locking the resource
     */
    public String getUserId() {
        return userId;
    }

    /**
     *
     * @return the identifier of the locked resource
     */
    public String getResourceId() {
        return resourceId;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LockedResource that = (LockedResource) o;

        if (expirationTime != that.expirationTime) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        return resourceId != null ? resourceId.equals(that.resourceId) : that.resourceId == null;

    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (resourceId != null ? resourceId.hashCode() : 0);
        result = 31 * result + (int) (expirationTime ^ (expirationTime >>> 32));
        return result;
    }
}
