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

import org.apache.commons.codec.binary.StringUtils;
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
    public LockedResource unLock(Identifiable resource, String userId) {
        return unLock(resource, userId, lockFactory);
    }

    public boolean couldBeLocked(LockedResource lockedResource, String userId) {
        if (userId != null && (lockedResource == null || StringUtils.equals(lockedResource.getUserId(), userId)
                || lockedResource.getExpirationTime() < Instant.now().getEpochSecond())) {
            return true;
        }
        return false;
    }

    public LockedResource tryLock(Identifiable object, String userId, LockFactory lockFactory) {
        if (object == null) {
            LOGGER.warn("A null resource cannot be locked...");
            throw new IllegalArgumentException("A null resource cannot be locked");
        }
        if (org.apache.commons.lang.StringUtils.isEmpty(userId)) {
            LOGGER.warn("A null user-identifier cannot lock a resource...");
            throw new IllegalArgumentException("A null user-identifier cannot be locked");
        }

        String resource = object.getId();
        DistributedLock lock = lockFactory.getLock(resource);
        LockedResource lockedResource;
        lock.lock();
        try {
            lockedResource = get(object);
            if (lockedResource == null) {
                lockedResource = add(object, userId);
            } else if (couldBeLocked(lockedResource, userId)) {
                remove(object);
                lockedResource = add(object, userId);
            } else {
                return lockedResource;
            }
        } finally {
            lock.unlock();
        }

        return lockedResource;
    }

    public LockedResource unLock(Identifiable resource, String userId, LockFactory lockFactory) {
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
            } else if (couldBeLocked(lockedResource, userId)) {
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
    public boolean retrieveLock(Identifiable object, String userId) throws Exception {
        LockedResource lockedResource = tryLock(object, userId);
        if (lockedResource != null) {
            if (StringUtils.equals(userId, lockedResource.getUserId())) {
                return true;
            } else {
                throw new Exception(lockedResource.getUserId());
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean retrieveUnLock(Identifiable object, String userId) throws Exception {
        LockedResource lockedResource = unLock(object, userId);
        if (lockedResource != null) {
            throw new Exception(lockedResource.getUserId());
        } else {
            return true;
        }
    }

    protected abstract LockedResource add(Identifiable resource, String userId);

}
