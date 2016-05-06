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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.preparation.Identifiable;
import org.talend.dataprep.lock.DistributedLock;
import org.talend.dataprep.lock.LockFactory;

public abstract class AbstractLockedResourceRepository implements LockedResourceRepository {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLockedResourceRepository.class);

    protected LockFactory lockFactory;

    @Override
    public LockedResource tryLock(Identifiable resource, String userId) {
        return tryLock(resource, userId, lockFactory);
    }

    @Override
    public LockedResource tryUnlock(Identifiable resource, String userId) {
        return tryUnlock(resource, userId, lockFactory);
    }

    public LockedResource tryLock(Identifiable resource, String userId, LockFactory lockFactory) {
        if (resource == null) {
            LOGGER.warn("A null resource cannot be locked...");
            throw new IllegalArgumentException("A null resource cannot be locked");
        }
        if (org.apache.commons.lang.StringUtils.isEmpty(userId)) {
            LOGGER.warn("A null user-identifier cannot lock a resource...");
            throw new IllegalArgumentException("A null user-identifier cannot be locked");
        }

        String resourceId = resource.getId();
        DistributedLock lock = lockFactory.getLock(resourceId);
        LockedResource lockedResource;
        lock.lock();
        try {
            lockedResource = get(resource);
            if (lockedResource == null) {
                lockedResource = add(resource, userId);
            } else if (lockOwned(lockedResource, userId) || lockExpired(lockedResource)) {
                remove(resource);
                lockedResource = add(resource, userId);
            } else {
                return lockedResource;
            }
        } finally {
            lock.unlock();
        }

        return lockedResource;
    }

    public LockedResource tryUnlock(Identifiable resource, String userId, LockFactory lockFactory) {
        if (resource == null) {
            LOGGER.warn("A null resource cannot be locked...");
            throw new IllegalArgumentException("A null resource cannot be locked");
        }
        if (org.apache.commons.lang.StringUtils.isEmpty(userId)) {
            LOGGER.warn("A null user-identifier cannot lock a resource...");
            throw new IllegalArgumentException("A null user-identifier cannot be locked");
        }

        String resourceId = resource.getId();
        DistributedLock lock = lockFactory.getLock(resourceId);
        final LockedResource result;
        lock.lock();
        try {
            LockedResource lockedResource = get(resource);
            if (lockedResource == null) {
                result = null;
            } else if (lockOwned(lockedResource, userId) || lockExpired(lockedResource)) {
                remove(resource);
                result = null;
            } else {
                result = lockedResource;
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    @Override
    public boolean lockOwned(LockedResource lockedResource, String userId){
        final long now = Instant.now().getEpochSecond();
        if (lockedResource != null && StringUtils.isNotEmpty(userId) && StringUtils.equals(userId, lockedResource.getUserId())
                && now <= lockedResource.getExpirationTime()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean lockExpired(LockedResource lockedResource){
        final long now = Instant.now().getEpochSecond();
        if (lockedResource != null && lockedResource.getExpirationTime() < now){
            return true;
        }
        return false;
    }

    @Override
    public boolean lockReleased(LockedResource lockedResource, String userId) {
        if (lockedResource == null) {
            return true;
        } else {
            return false;
        }
    }

    protected abstract LockedResource add(Identifiable resource, String userId);

}
