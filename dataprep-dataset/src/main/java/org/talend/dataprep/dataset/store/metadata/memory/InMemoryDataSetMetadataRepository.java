//  ============================================================================
//
//  Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.dataset.store.metadata.memory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.DataSetMetadataBuilder;
import org.talend.dataprep.dataset.store.metadata.DataSetMetadataRepository;
import org.talend.dataprep.dataset.store.metadata.DataSetMetadataRepositoryAdapter;
import org.talend.dataprep.lock.DistributedLock;

import com.google.common.base.Defaults;

/**
 * In memory implementation of the DataSetMetadataRepository.
 */
@Component
@ConditionalOnProperty(name = "dataset.metadata.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryDataSetMetadataRepository extends DataSetMetadataRepositoryAdapter {

    /** This class' logger. */
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDataSetMetadataRepository.class);

    @Autowired
    private DataSetMetadataBuilder metadataBuilder;

    /** Where the DatasetMetadata is actually stored. */
    private final Map<String, DataSetMetadata> store = new ConcurrentHashMap<>();

    /**
     * @see DataSetMetadataRepository#list()
     */
    @Override
    public Iterable<DataSetMetadata> list() {
        final Collection<DataSetMetadata> values = store.values();
        LOG.debug("list {} dataset metadata", values.size());
        return values;
    }

    @Override
    public Iterable<DataSetMetadata> listCompatible(String id) {
        LOG.info("Looking for data set #{} in the system", id);
        final DataSetMetadata metadata = get(id);

        if (metadata == null) {
            LOG.info("Similar schemas could not be found for data set #{}", id);
            return Collections.emptyList();
        }
        final Collection<DataSetMetadata> values = store.values().stream()
                .filter(m -> (m != null && !metadata.equals(m) && metadata.compatible(m))).collect(Collectors.toList());
        LOG.debug("list similar {} data set metadata", values.size());
        return values;
    }

    /**
     * @see DataSetMetadataRepository#add(DataSetMetadata)
     */
    @Override
    public void add(DataSetMetadata dataSetMetadata) {
        store.put(dataSetMetadata.getId(), dataSetMetadata);
    }

    /**
     * this nullifies and resets transient values that are supposed not to be stored
     *
     * @param zeObject The object where non transient fields will be nullified.
     */
    void resetTransientValues(@Nullable Object zeObject) {
        if (zeObject != null) {
            Field[] fields = zeObject.getClass().getDeclaredFields();
            for (Field field : fields) {
                // ignore jacoco injected field
                if (Modifier.isTransient(field.getModifiers()) && !field.getName().endsWith("jacocoData")) { //$NON-NLS-1$
                    Object defaultValue = Defaults.defaultValue(field.getType());
                    field.setAccessible(true);
                    try {
                        field.set(zeObject, defaultValue);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        LOG.error("failed to reset the transient field [" + field + "] before storage", e);
                    }
                }
            }
        } // else null so do nothing
    }

    /**
     * @see DataSetMetadataRepository#clear()
     */
    @Override
    public void clear() {
        // Remove all data set (but use lock for remaining asynchronous processes).
        final List<DataSetMetadata> list = IteratorUtils.toList(list().iterator());
        for (DataSetMetadata metadata : list) {
            final DistributedLock lock = createDatasetMetadataLock(metadata.getId());
            try {
                lock.lock();
                remove(metadata.getId());
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * @see DataSetMetadataRepository#size()
     */
    @Override
    public int size() {
        return store.size();
    }

    /**
     * @see DataSetMetadataRepository#get(String)
     */
    @Override
    public DataSetMetadata get(String id) {
        DataSetMetadata dataSetMetadata = store.get(id);
        if (dataSetMetadata == null) {
            LOG.info("data set metadata #{} not found in the system", id);
            return null;
        }
        resetTransientValues(dataSetMetadata);
        return metadataBuilder.metadata().copy(dataSetMetadata).build();
    }

    /**
     * @see DataSetMetadataRepository#remove(String)
     */
    @Override
    public void remove(String id) {
        store.remove(id);
    }

}
