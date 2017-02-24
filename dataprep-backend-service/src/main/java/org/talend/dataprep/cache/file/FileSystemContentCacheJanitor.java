// ============================================================================
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

package org.talend.dataprep.cache.file;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

@Component
@ConditionalOnProperty(name = "service.cache", havingValue = "file")
@EnableScheduling
public class FileSystemContentCacheJanitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemContentCacheJanitor.class);

    private final String location;

    @Autowired
    public FileSystemContentCacheJanitor(@Value("${service.cache.file.location}") String location) {
        if (!location.endsWith("/")) {
            location += "/";
        }
        this.location = location + "cache/";
        try {
            final File directory = new File(location);
            if (!directory.exists()) {
                FileUtils.forceMkdir(directory);
            }
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    /**
     * A clean up process that starts a minute after the previous ended.
     */
    @Scheduled(fixedDelay = 60000)
    public void janitor() {
        if (!Paths.get(location).toFile().exists()) {
            LOGGER.debug("No cache content to clean.");
            return;
        }
        final long start = System.currentTimeMillis();
        final AtomicLong deletedCount = new AtomicLong();
        final AtomicLong totalCount = new AtomicLong();
        LOGGER.debug("Janitor process started @ {}.", start);
        try {
            final BiConsumer<Path, String> deleteOld = (file, suffix) -> {
                try {
                    final long time = Long.parseLong(suffix);
                    if (time < start) {
                        try {
                            Files.delete(file);
                            deletedCount.incrementAndGet();
                        } catch (NoSuchFileException e) {
                            LOGGER.debug("Ignored delete issue for '{}'.", file.getFileName(), e);
                        } catch (IOException e) {
                            LOGGER.warn("Unable to delete '{}'.", file.getFileName());
                            LOGGER.debug("Unable to delete '{}'.", file.getFileName(), e);
                        }
                    }
                } catch (NumberFormatException e) {
                    LOGGER.debug("Ignore file '{}'", file);
                }
                totalCount.incrementAndGet();
            };
            Files.walkFileTree(Paths.get(location), new FileSystemVisitor(deleteOld));
        } catch (IOException e) {
            LOGGER.error("Unable to clean up cache", e);
        }
        LOGGER.debug("Janitor process ended @ {} ({}/{} files successfully deleted).", System.currentTimeMillis(), deletedCount,
                totalCount);
    }

}
