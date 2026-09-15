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
package org.structr.core.script.polyglot.filesystem;

import org.graalvm.polyglot.io.FileSystem;
import org.structr.common.Permission;
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
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The file system a scripting context sees: paths are resolved against the file area in the database,
 * so a script can {@code import('/lib/util.js')} and every JavaScript snippet is a module, which means
 * every script has import() available.
 *
 * <p>Ticket 1592: this used to resolve every path through {@code StructrApp.getInstance()}, and an App
 * without a security context is the superuser instance - so import() read any file in the instance no
 * matter who ran the script, and {@code checkAccess} caught FrameworkException and returned normally,
 * which is a granted access. The file area is ACL-controlled like everything else in Structr, and there
 * is no reason for the way in to change that, so access is resolved in the context of whoever is
 * running the script: {@link #bind} publishes it for the duration of an evaluation, and a script
 * running inside {@code $.doPrivileged()} sees the superuser context because that is what the
 * ActionContext carries there.</p>
 *
 * <p>Failure is denial throughout. A file the caller may not read is indistinguishable from one that
 * does not exist, because the node query in the caller's context does not return it either way.</p>
 */
public class PolyglotFilesystem implements FileSystem {

	/**
	 * The evaluation currently running on this thread. Set around every polyglot evaluation, because
	 * the Context builders - and with them this file system - are static and shared by all of them,
	 * while the caller is not. Read lazily on each call rather than captured, so a $.doPrivileged()
	 * block, which swaps the security context on the ActionContext, is seen while it is in effect.
	 */
	private static final ThreadLocal<ActionContext> currentEvaluation = new ThreadLocal<>();

	/**
	 * Publishes the given evaluation to the file system and answers the one it replaced, which the
	 * caller passes back to {@link #unbind} - scripts call scripts, so these nest.
	 */
	public static ActionContext bind(final ActionContext actionContext) {

		final ActionContext previous = currentEvaluation.get();

		currentEvaluation.set(actionContext);

		return previous;
	}

	public static void unbind(final ActionContext previous) {

		if (previous != null) {

			currentEvaluation.set(previous);

		} else {

			currentEvaluation.remove();
		}
	}

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

		final SecurityContext securityContext = getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface abstractFile = findByPath(app, StructrTraits.ABSTRACT_FILE, path);

			tx.success();

			if (abstractFile == null) {

				throw new NoSuchFileException("No file or folder found for path: " + path.toString());
			}

			if (modes.contains(AccessMode.WRITE) && !abstractFile.isGranted(Permission.write, securityContext)) {

				throw new AccessDeniedException(path.toString());
			}

		} catch (FrameworkException ex) {

			throw new IOException("Could not check access for path: " + path.toString(), ex);
		}
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

		final SecurityContext securityContext = getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = findByPath(app, StructrTraits.FOLDER, dir);
			if (folder == null) {

				FileHelper.createFolderPath(securityContext, dir.toString());

			} else {

				throw new FileAlreadyExistsException("Folder already exists for path: " + dir.toString());
			}

			tx.success();

		} catch (FrameworkException ex) {

			throw new IOException("Could not create folder for path: " + dir.toString(), ex);
		}
	}

	@Override
	public void delete(Path path) throws IOException {

		final App app = StructrApp.getInstance(getSecurityContext());

		try (final Tx tx = app.tx()) {

			final NodeInterface file = findByPath(app, StructrTraits.ABSTRACT_FILE, path);
			if (file != null) {

				app.delete(file);

			} else {

				throw new NoSuchFileException("Cannot delete file or folder. No entity found for path: " + path.toString());
			}

			tx.success();

		} catch (FrameworkException ex) {

			throw new IOException("Could not delete file or folder for path: " + path.toString(), ex);
		}
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {

		final SecurityContext securityContext = getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final Traits traits                        = Traits.of(StructrTraits.ABSTRACT_FILE);
			final PropertyKey<NodeInterface> parentKey = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
			NodeInterface file = findByPath(app, StructrTraits.FILE, path);

			if (file == null && (options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW))) {

				if (path.getParent() != null) {

					NodeInterface  parent = FileHelper.createFolderPath(securityContext, path.getParent().toString());

					file = app.create(StructrTraits.FILE,
						new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), path.getFileName().toString()),
						new NodeAttribute<>(parentKey, parent)
					);

				} else {

					file = app.create(StructrTraits.FILE, new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), path.getFileName().toString()));
				}

			} else if (file != null && options.contains(StandardOpenOption.CREATE_NEW)) {

				throw new FileAlreadyExistsException("Cannot open file with CREATE_NEW option. File already exists at path: " + path.toString());
			}

			if (file == null) {

				throw new NoSuchFileException("Cannot open file. No file found or created for path: " + path.toString());
			}

			// The channel writes bytes through the storage provider, which knows nothing about the node
			// it belongs to, so a write permission the node query cannot express has to be checked here.
			if (isWriting(options) && !file.isGranted(Permission.write, securityContext)) {

				throw new AccessDeniedException(path.toString());
			}

			tx.success();

			return StorageProviderFactory.getStorageProvider(file.as(File.class)).getSeekableByteChannel(options);

		} catch (FrameworkException ex) {

			throw new IOException("Could not open byte channel for path: " + path.toString(), ex);
		}
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {

		final SecurityContext securityContext = getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = findByPath(app, StructrTraits.FOLDER, dir);

			tx.success();

			if (folder == null) {

				throw new NotDirectoryException("No directory found for path: " + dir.toString());
			}

			return new VirtualDirectoryStream(securityContext, dir, filter);

		} catch (FrameworkException ex) {

			throw new IOException("Could not open directory stream for path: " + dir.toString(), ex);
		}
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

		final NodeInterface file = FileHelper.getFileByAbsolutePath(getSecurityContext(), path.toString());
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
				case "isDirectory" -> attributeMap.put("isDirectory", (file.is(StructrTraits.FOLDER)));
				case "creationTime" -> attributeMap.put("creationTime", FileTime.fromMillis(file.getCreatedDate().getTime()));
				case "lastModifiedTime" -> attributeMap.put("lastModifiedTime", FileTime.fromMillis(file.getLastModifiedDate().getTime()));
				case "lastAccessTime" -> attributeMap.put("lastAccessTime", FileTime.fromMillis(file.getLastModifiedDate().getTime()));
				case "isSymbolicLink" -> attributeMap.put("isSymbolicLink", false);
				case "isRegularFile" -> attributeMap.put("isRegularFile", (file.is(StructrTraits.FILE)));
				case "size" -> attributeMap.put("size", (file.is(StructrTraits.FILE) ? FileHelper.getSize(file.as(File.class)) : 0));
			}
		}

		return attributeMap;
	}

	// ----- private methods -----
	/**
	 * The security context every path in this file system is resolved in. There is no fallback: this
	 * file system is reachable from a running script and nowhere else, so an unbound thread is a path
	 * into the file area that nobody is answerable for, and answering it as the superuser is the bug
	 * this class was carrying.
	 */
	private SecurityContext getSecurityContext() throws IOException {

		final ActionContext actionContext = currentEvaluation.get();
		if (actionContext == null) {

			throw new IOException("File system access outside of a running script is not allowed.");
		}

		return actionContext.getSecurityContext();
	}

	private NodeInterface findByPath(final App app, final String type, final Path path) throws FrameworkException {

		final PropertyKey<String> pathKey = Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY);

		return app.nodeQuery(type).key(pathKey, path.toString()).getFirst();
	}

	private boolean isWriting(final Set<? extends OpenOption> options) {

		return options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)
			|| options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW);
	}
}
