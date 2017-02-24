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

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;

class FileSystemVisitor extends SimpleFileVisitor<Path> {

    private final BiConsumer<Path, String> consumer;

    private final boolean skipPermanent;

    FileSystemVisitor(final BiConsumer<Path, String> consumer) {
        this(consumer, true);
    }

    FileSystemVisitor(final BiConsumer<Path, String> consumer, final boolean skipPermanent) {
        this.consumer = consumer;
        this.skipPermanent = skipPermanent;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        final String fileName = file.toFile().getName();
        // Ignore "." files (hidden files like MacOS).
        if (fileName.startsWith(".")) {
            return FileVisitResult.CONTINUE;
        }
        final String suffix = StringUtils.substringAfterLast(fileName, ".");
        // Ignore NFS files (may happen in local mode when NFS is used).
        if (suffix.startsWith("nfs")) {
            return FileVisitResult.CONTINUE;
        }
        if (this.skipPermanent && StringUtils.isEmpty(suffix)) {
            return FileVisitResult.CONTINUE;
        }
        consumer.accept(file, suffix);
        return super.visitFile(file, attrs);
    }
}
