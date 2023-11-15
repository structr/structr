/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class FileCleanupVisitor implements FileVisitor<Path> {

	private static final Logger logger      = LoggerFactory.getLogger(FileCleanupVisitor.class.getName());
	private Map<String, Object> metadata    = null;
	private Path basePath                   = null;

	public FileCleanupVisitor(final Path basePath, final Map<String, Object> metadata) {

		this.basePath        = basePath;
		this.metadata        = metadata;
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

		if (!basePath.equals(dir)) {

			deletePathIfNotInMetadata(dir);
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

		if (attrs.isRegularFile()) {

			deletePathIfNotInMetadata(file);
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {

		logger.warn("Exception while running cleanup at {}: {}", new Object[] { file.toString(), exc.getMessage() });
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	// ----- private methods -----
	private void deletePathIfNotInMetadata (final Path path) {

		final String absolutePath = harmonizeFileSeparators("/", basePath.relativize(path).toString());

		if (!metadata.containsKey(absolutePath)) {

			try {

				logger.info("Removing {}", path);
				deleteRecursively(path);

			} catch (IOException ioex) {

				logger.warn("Error occurred while trying to clean up {}", path);
			}
		}
	}

	private void deleteRecursively(final Path path) throws IOException {

		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {

			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {

				for (Path entry : entries) {

					deleteRecursively(entry);
				}
			}
		}

		Files.delete(path);
	}


	private String harmonizeFileSeparators(final String... sources) {

		final StringBuilder buf = new StringBuilder();

		for (final String src : sources) {
			buf.append(src);
		}

		int pos = buf.indexOf("\\");

		while (pos >= 0) {

			buf.replace(pos, pos+1, "/");
			pos = buf.indexOf("\\");
		}

		return buf.toString();
	}
}
