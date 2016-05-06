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

import java.util.Collection;

import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.preparation.Identifiable;
import org.talend.dataprep.api.preparation.Preparation;

/**
 * Base interface of user locked-resources repositories (mongodb, file-system and in memory).
 *
 * A user can lock multiple resources at the same time, whereas a resource can only be locked by a unique user. After a
 * locked on a resource is released by a user another one can lock it.
 *
 * This repository keeps track of user locked-resources that could be any valid {@link Identifiable} object e.g.
 * {@link Preparation}, {@link DataSetMetadata} but
 *
 */
public interface LockedResourceRepository {

    /**
     * Locks the specified resource and add it to the collection of locked objects.
     * 
     * @param resource the specified identifiable object
     */
    LockedResource tryLock(Identifiable resource, String userId);

    LockedResource tryUnlock(Identifiable resource, String userId);

    /**
     * Returns the {@link Identifiable} with the specified identifier and of the specified class.
     * 
     * @param resource the resource
     * @return the {@link Identifiable} with the specified identifier and of the specified class
     */
    LockedResource get(Identifiable resource);

    /**
     * Returns the collection of locked-resources currently managed by the repository.
     * 
     * @return the collection of resources of the specified class managed by the repository
     */
    Collection<LockedResource> listAll();

    /**
     * Returns the collection of resources locked by the user with the specified identifier.
     *
     * @param userId the specified user identifier
     * @return the collection of resources locked by the user with the specified identifier
     */
    Collection<LockedResource> listByUser(String userId);

    /**
     * Clears the locks and their associated resources
     */
    void clear();

    /**
     * Removes locks
     * 
     * @param resource the resource to remove from this repository
     */
    void remove(Identifiable resource);

    boolean lockOwned(LockedResource lockedResource, String userId);
    boolean lockReleased(LockedResource lockedResource, String userId);

}
