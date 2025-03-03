/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.script.polyglot.filesystem;

import org.graalvm.polyglot.io.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PolyglotFilesystem implements FileSystem {
	private static final Logger logger = LoggerFactory.getLogger(PolyglotFilesystem.class);

	@Override
	public Path parsePath(URI uri) {

		if (uri != null) {

			return parsePath(uri.getPath());
		} else {

			return null;
		}
	}

	@Override
	public Path parsePath(String path) {

		if (path != null) {

			return Path.of(path);
		}
		return null;
	}

	@Override
	public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> pathKey = Traits.of(StructrTraits.ABSTRACT_FILE).key("path");
			final NodeInterface abstractFile  = app.nodeQuery(StructrTraits.ABSTRACT_FILE).and(pathKey, path.toString()).getFirst();

			tx.success();

			if (abstractFile == null) {
				throw new NoSuchFileException("No file or folder found for path: " + path.toString());
			}

		} catch (FrameworkException ex) {

			logger.error("Could not open directory stream for dir: {}.", path.toString(), ex);
		}
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> pathKey = Traits.of(StructrTraits.ABSTRACT_FILE).key("path");
			final NodeInterface folder        = app.nodeQuery(StructrTraits.FOLDER).and(pathKey, dir.toString()).getFirst();

			if (folder == null) {
				FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), dir.toString());
			}

			tx.success();

			throw new FileAlreadyExistsException("Folder already exists for path: " + dir.toString());

        } catch (FrameworkException ex) {

			logger.error("Unexpected exception while trying to create folder", ex);
        }
    }

	@Override
	public void delete(Path path) throws IOException {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final PropertyKey<String> pathKey = Traits.of(StructrTraits.ABSTRACT_FILE).key("path");
			final NodeInterface file          = app.nodeQuery(StructrTraits.ABSTRACT_FILE).and(pathKey, path.toString()).getFirst();

			if (file != null) {

				app.delete(file);

			} else {

				throw new NoSuchFileException("Cannot delete file or folder. No entity found for path: " + path.toString());
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.error("Unexpected exception while trying to delete file", ex);
		}
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final Traits traits                        = Traits.of(StructrTraits.ABSTRACT_FILE);
			final PropertyKey<String> pathKey          = traits.key("path");
			final PropertyKey<NodeInterface> parentKey = traits.key("parent");

			NodeInterface file = app.nodeQuery(StructrTraits.FILE).and(pathKey, path.toString()).getFirst();

			if (file == null) {

				if (path.getParent() != null) {

					NodeInterface  parent = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), path.getParent().toString());

					file = app.create(StructrTraits.FILE,
						new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), path.getFileName().toString()),
						new NodeAttribute<>(parentKey, parent)
					);

				} else {

					file = app.create(StructrTraits.FILE, new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), path.getFileName().toString()));
				}
			}

			tx.success();

			if (options.contains(StandardOpenOption.CREATE_NEW)) {

				throw new FileAlreadyExistsException("Cannot open file with CREATE_NEW option. File already exists at path: " + path.toString());
			}

			return StorageProviderFactory.getStorageProvider(file.as(File.class)).getSeekableByteChannel(options);

		} catch (FrameworkException ex) {

			logger.error("Unexpected exception while trying to open new bytechannel", ex);
			return null;
		}
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> path = Traits.of(StructrTraits.ABSTRACT_FILE).key("path");
			final NodeInterface folder     = app.nodeQuery(StructrTraits.FOLDER).and(path, dir.toString()).getFirst();

			if (folder != null) {
				return new VirtualDirectoryStream(dir, filter);
			}

			tx.success();

			throw new NotDirectoryException("No directory found for path: " + dir.toString());

		} catch (FrameworkException ex) {

                        logger.error("Could not open directory stream for dir: {}.", dir.toString(), ex);
		}

		throw new NotDirectoryException(dir.toString());
	}

	@Override
	public Path toAbsolutePath(Path path) {
		return path;
	}

	@Override
	public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
		return path;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String rawattributes, LinkOption... options) throws IOException {

		final NodeInterface file = FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), path.toString());

		if (file == null && rawattributes.equals("isDirectory")) {
			return Map.of("isDirectory", false);
		}

		final int viewIndex = rawattributes.indexOf(':');
		String view = "basic";
		String attributes = rawattributes;

		if (viewIndex != -1) {
			view = rawattributes.substring(0, viewIndex);
			attributes = rawattributes.substring(viewIndex + 1, rawattributes.length());
		}

		if (!view.equals("basic")) {
			throw new UnsupportedOperationException("View \"%s\" is not supported by PolyglotFilesystem.".formatted(view));
		}

		Map<String, Object> attributeMap = new HashMap<>();
		if (attributes.isEmpty()) {
			return attributeMap;
		}

		if (file == null) {
			throw new IOException("File or folder does not exist for requested path: " + path.toString());
		}


		for (String attr : attributes.split(",")) {
			switch (attr) {
				case "isDirectory" -> attributeMap.put("isDirectory", (file.is("folder")));
				case "creationTime" -> attributeMap.put("creationTime", FileTime.fromMillis(file.getCreatedDate().getTime()));
				case "lastModifiedTime" -> attributeMap.put("lastModifiedTime", FileTime.fromMillis(file.getLastModifiedDate().getTime()));
				case "lastAccessTime" -> attributeMap.put("lastAccessTime", FileTime.fromMillis(file.getLastModifiedDate().getTime()));
				case "isSymbolicLink" -> attributeMap.put("isSymbolicLink", false);
				case "isRegularFile" -> attributeMap.put("isRegularFile", (file.is(StructrTraits.FILE)));
				case "size" -> attributeMap.put("size", (file.is(StructrTraits.FILE) ? FileHelper.getSize((File)file) : 0));
			}
		}

		return attributeMap;
	}
}
