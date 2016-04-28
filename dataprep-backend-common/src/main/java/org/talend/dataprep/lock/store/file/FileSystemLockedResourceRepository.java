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

package org.talend.dataprep.lock.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.preparation.Identifiable;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.lock.LockFactory;
import org.talend.dataprep.lock.store.AbstractLockedResourceRepository;
import org.talend.dataprep.lock.store.LockedResource;
import org.talend.dataprep.lock.store.LockedResourceRepository;
import org.talend.dataprep.util.FilesHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * File system implementation of lock repository.
 */
@Component
@ConditionalOnProperty(name = "lock.resource.store", havingValue = "file")
public class FileSystemLockedResourceRepository extends AbstractLockedResourceRepository {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemLockedResourceRepository.class);

    /** The dataprep ready jackson mapper. */
    @Autowired
    private ObjectMapper mapper;

    /** Where to store the dataset metadata */
    @Value("${lock.resource.store.file.location}")
    private String storeLocation;

    /**
     * Make sure the root folder is there.
     */
    @PostConstruct
    private void init() {
        getRootFolder().mkdirs();
    }

    @Autowired
    public void setLockFactory(LockFactory lockFactory){
        this.lockFactory = lockFactory;
    }

    @Override
    public LockedResource add(Identifiable resource, String userId) {

        LockedResource lockedResource = new LockedResource(resource.getId(), userId);
        final File outputFile = getIdentifiableFile(LockedResource.class, resource.getId());

        try (GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(outputFile))) {
            mapper.writer().writeValue(output, lockedResource);
        } catch (IOException e) {
            LOGGER.error("Error saving {}", resource, e);
            throw new TDPException(CommonErrorCodes.UNABLE_TO_SAVE_PREPARATION, e,
                    ExceptionContext.build().put("id", resource.id()));
        }
        LOGGER.debug("Resource #{} of type {} saved", resource.id(), resource.getClass().getSimpleName());
        return lockedResource;
    }

   /**
     * @see AbstractLockedResourceRepository#add(Identifiable, String)
     */
    @Override
    public LockedResource get(Identifiable resource) {

        final File file = getIdentifiableFile(LockedResource.class, resource.getId());
        if (file.getName().startsWith(".")) {
            LOGGER.info("Ignore hidden file {}", file.getName());
            return null;
        }
        if (!file.exists()) {
            LOGGER.debug("Resource #{} of type {} not found in file system", resource.getId(), resource);
            return null;
        }
        return get(file);
    }

    @Override
    public Collection<LockedResource> listAll() {
        //@formatter:off
        File[] files = getRootFolder().listFiles();
        if(files == null) {
            LOGGER.error("error listing locked resources");
            files = new File[0];
        }
        Collection<LockedResource> result;
        try (final Stream<File> stream = Arrays.stream(files)) {
            result = stream.map(this::get)                   // read all files
                    .filter(r -> r != null)          // filter out null entries
                    .collect(Collectors.toSet());                              // and put it in a set
        }
        //@formatter:on
        LOGGER.debug("There are #{} resources locked by user with identifier {}", result.size());
        return result;
    }

    @Override
    public Collection<LockedResource> listByUser(String userId) {
        //@formatter:off
        File[] files = getRootFolder().listFiles();
        if(files == null) {
            LOGGER.error("error listing resources locked by user with identifier {}", userId);
            files = new File[0];
        }
        Collection<LockedResource> result;
        try (final Stream<File> stream = Arrays.stream(files)) {
            result = stream.map(this::get)                   // read all files
                    .filter(r -> r != null)          // filter out null entries
                    .filter(r -> StringUtils.equals(r.getUserId(), userId))
                    .collect(Collectors.toSet());                              // and put it in a set
        }
        //@formatter:on
        LOGGER.debug("There are #{} resources locked by user with identifier {}", result.size());
        return result;

    }

    /**
     * @see LockedResourceRepository#clear()
     */
    @Override
    public void clear() {

        // clear all files
        final File[] lockedResources = getRootFolder().listFiles();
        for (File file : lockedResources) {
            FilesHelper.deleteQuietly(file);
        }
        LOGGER.debug("Locked-resources repository cleared");
    }

    /**
     * @see LockedResourceRepository#remove(Identifiable)
     */
    @Override
    public void remove(Identifiable resource) {
        if (resource == null) {
            return;
        }
        final File file = getIdentifiableFile(LockedResource.class, resource.getId());
        FilesHelper.deleteQuietly(file);
        LOGGER.debug("locked resource #{} removed", resource.id());
    }

    private File getIdentifiableFile(Identifiable object) {
        return getIdentifiableFile(object.getClass(), object.id());
    }



    private LockedResource get(File file) {
        if (!file.exists()) {
            return null;
        }
        try (GZIPInputStream input = new GZIPInputStream(new FileInputStream(file))) {
            return mapper.readerFor(LockedResource.class).readValue(input);
        } catch (IOException e) {
            LOGGER.error("error reading locked resource file {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Return the file that matches the given identifiable id.
     *
     * @param clazz the identifiable class.
     * @param id the identifiable... id !
     * @return the file where to read/write the identifiable object.
     */
    private File getIdentifiableFile(Class clazz, String id) {
        return new File(storeLocation + '/' + clazz.getSimpleName() + '-' + stripOptionalPrefix(clazz, id));
    }

    /**
     * Remove the optional classname prefix if the given id already has it.
     *
     * For instance : "Preparation-a99a05a862c6a220d7977f97cd9cb3f71d640592" returns
     * "a99a05a862c6a220d7977f97cd9cb3f71d640592" "a99a05a862c6a220d7977f97cd9cb3f71d640592" returns
     * "a99a05a862c6a220d7977f97cd9cb3f71d640592"
     *
     * @param clazz the class of the wanted object.
     * @param id the object id.
     * @return the id striped of the classname prefix if needed.
     */
    private String stripOptionalPrefix(Class clazz, String id) {

        if (StringUtils.isBlank(id)) {
            return null;
        }

        final String className = clazz.getSimpleName();
        if (id.startsWith(className)) {
            return id.substring(className.length() + 1);
        }
        return id;
    }

    /**
     * Return the root folder where the preparations are stored.
     *
     * @return the root folder.
     */
    private File getRootFolder() {
        return new File(storeLocation);
    }
}
