/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.maintenance.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class HtmlFileImporter {

    private static final Logger logger = LoggerFactory.getLogger(HtmlFileImporter.class.getName());

    public void processFolderContentsSorted(final Path folder) throws IOException {

        Files.list(folder).sorted().forEach((Path path) -> {

            try {

                final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

                if (attrs.isRegularFile()) {

                    final String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".html")) {

                        processFile(path, fileName);
                    }

                } else {
                    logger.warn("Unexpected directory '{}' found in '{}' directory, ignoring", path.getFileName().toString(), folder.getFileName().toString());
                }

            } catch (IOException ioe) {

                logger.warn("Exception while importing file '{}': {}", path.getFileName().toString(), ioe.toString());
            }
        });
    }

    abstract void processFile(final Path file, final String fileName) throws IOException;
}
