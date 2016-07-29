/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.files.ssh.filesystem.path.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.files.ssh.filesystem.StructrFileAttributes;
import org.structr.files.ssh.filesystem.StructrFileChannel;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import static org.structr.web.entity.AbstractFile.path;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class StructrFilePath extends StructrPath {

	private static final Logger logger = Logger.getLogger(StructrFilePath.class.getName());

	private AbstractFile cachedActualFile = null;

	public StructrFilePath(final StructrFilesystem fs, final StructrPath parent, final String name) {
		super(fs, parent, name);
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(DirectoryStream.Filter<? super Path> filter) {

		final Folder folder = (Folder)getActualFile();
		if (folder != null) {

			return new DirectoryStream() {

				boolean closed = false;

				@Override
				public Iterator iterator() {

					if (!closed) {

						final App app                 = StructrApp.getInstance(fs.getSecurityContext());
						final List<StructrPath> files = new LinkedList<>();

						try (final Tx tx = app.tx()) {


							for (final Folder folder : folder.getProperty(Folder.folders)) {

								files.add(new StructrFilePath(fs, StructrFilePath.this, folder.getName()));
							}

							for (final FileBase file : folder.getProperty(Folder.files)) {

								files.add(new StructrFilePath(fs, StructrFilePath.this, file.getName()));
							}

							tx.success();

						} catch (FrameworkException fex) {
							logger.log(Level.WARNING, "", fex);
						}

						return files.iterator();

					}

					return Collections.emptyIterator();
				}

				@Override
				public void close() throws IOException {
					closed = true;
				}
			};

		}

		return null;
	}

	@Override
	public FileChannel newFileChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {

		AbstractFile actualFile   = getActualFile();
		FileChannel channel       = null;

		final boolean create      = options.contains(StandardOpenOption.CREATE);
		final boolean createNew   = options.contains(StandardOpenOption.CREATE_NEW);
		final boolean write       = options.contains(StandardOpenOption.WRITE);
		final boolean truncate    = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
		final boolean append      = options.contains(StandardOpenOption.APPEND);

		if (write) {

			try (final Tx tx = StructrApp.getInstance(fs.getSecurityContext()).tx()) {

				// creation of a new file requested (=> create a new schema method)
				if (create || createNew) {

					// if CREATE_NEW, file must not exist, otherwise an error should be thrown
					if (createNew && actualFile != null) {
						throw new java.nio.file.FileAlreadyExistsException(toString());
					}

					// create a new file
					try {

						actualFile = createNewFile();
						setParentFolder(actualFile);

					} catch (FrameworkException fex) {
						fex.printStackTrace();
					}
				}

				if (actualFile != null && actualFile instanceof FileBase) {

					final FileBase file = (FileBase)actualFile;

					channel = new StructrFileChannel(file.getOutputStream(true, !truncate || append));
				}

				tx.success();

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to open file channel for writing of {0}: {1}", new Object[] { path, fex.getMessage() });
			}

		}

		if (actualFile != null && actualFile instanceof FileBase) {

			try (final Tx tx = StructrApp.getInstance(fs.getSecurityContext()).tx()) {

				channel = FileChannel.open(((FileBase)actualFile).getFileOnDisk().toPath(), options);

				tx.success();

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to open file channel for reading of {0}: {1}", new Object[] { path, fex.getMessage() });

			}

		} else {

			throw new FileNotFoundException("File " + path.toString() + " does not exist.");
		}

		return channel;
	}

	@Override
	public void createDirectory(FileAttribute<?>... attrs) throws IOException {

		final App app = StructrApp.getInstance(fs.getSecurityContext());
		try (final Tx tx = app.tx()) {

			final String name = getFileName().toString();
			final Folder newFolder = app.create(Folder.class, new NodeAttribute<>(AbstractNode.name, name));

			// set parent folder
			setParentFolder(newFolder);

			tx.success();

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to delete file {0}: {1}", new Object[] { path, fex.getMessage() } );
		}
	}

	@Override
	public void delete() throws IOException {

		final App app                 = StructrApp.getInstance(fs.getSecurityContext());
		final AbstractFile actualFile = getActualFile();

		try (final Tx tx = app.tx()) {

			// if a folder is to be deleted, check contents
			if (!actualFile.getProperty(AbstractFile.children).isEmpty()) {

				throw new DirectoryNotEmptyException(path.toString());

			} else {

				app.delete(actualFile);

				// remove cached version
				this.cachedActualFile = null;
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to delete file {0}: {1}", new Object[] { path, fex.getMessage() } );
		}
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) {
		return new StructrFilePath(fs, this, pathComponent);
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) throws IOException {

		final AbstractFile actualFile = getActualFile();
		if (actualFile != null) {

			return new StructrFileAttributes(fs.getSecurityContext(), actualFile).toMap(attributes);
		}

		throw new NoSuchFileException(toString());
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) throws IOException {

		final AbstractFile actualFile = getActualFile();
		if (actualFile != null) {

			return (T)new StructrFileAttributes(fs.getSecurityContext(), actualFile);
		}

		throw new NoSuchFileException(toString());
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(final Class<V> type, final LinkOption... options) throws IOException {
		return (V)getAttributes((Class)null, options);
	}

	@Override
	public void copy(final Path target, final CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void move(final Path target, final CopyOption... options) throws IOException {

		if (target instanceof StructrFilePath) {

			final App app                       = StructrApp.getInstance(fs.getSecurityContext());
			final StructrFilePath other    = (StructrFilePath)target;
			final AbstractFile otherFile        = other.getActualFile();
			final AbstractFile thisFile         = getActualFile();
			final String targetName             = target.getFileName().toString();

			try (final Tx tx = app.tx()) {

				final Path otherParent = other.getParent();

				if (otherParent instanceof StructrFilesPath) {

					// rename & move (parent is null: root path)
					thisFile.setProperty(AbstractFile.parent, null);
					thisFile.setProperty(AbstractNode.name, targetName);

					// this is a move operation, delete existing file
					if (otherFile != null) {
						app.delete(otherFile);
					}

				} else {

					final StructrFilePath parent  = (StructrFilePath)other.getParent();
					final Folder newParentFolder  = (Folder)parent.getActualFile();

					// rename & move
					thisFile.setProperty(AbstractFile.parent, newParentFolder);
					thisFile.setProperty(AbstractNode.name, targetName);

					// this is a move operation, delete existing file
					if (otherFile != null) {
						app.delete(otherFile);
					}
				}

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
			}
		}
	}

	@Override
	public void setAttribute(final String attribute, final Object value, final LinkOption... options) throws IOException {
		// ignore for now..
		//System.out.println("setAttribute(" + attribute + ", " + value + "): " + Arrays.asList(options));
	}

	@Override
	public boolean isSameFile(final Path path2) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public AbstractFile getActualFile() {

		if (cachedActualFile == null) {

			final String filePath = toString().substring(StructrPath.FILES_DIRECTORY.length() + 1);
			final App app         = StructrApp.getInstance(fs.getSecurityContext());

			try (final Tx tx = app.tx()) {

				// remove /files from path since it is a virtual directory
				cachedActualFile = app.nodeQuery(AbstractFile.class).and(AbstractFile.path, filePath).getFirst();

				tx.success();

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to load actual file for path {0}: {1}", new Object[] { toString(), fex.getMessage() } );
			}
		}

		return cachedActualFile;
	}

	public FileBase createNewFile() throws FrameworkException, IOException {

		final String name        = getFileName().toString();
		final byte[] data        = new byte[0];
		final String contentType = null;
		final FileBase file      = FileHelper.createFile(fs.getSecurityContext(), data, contentType, File.class, name);

		// cache newly created file
		this.cachedActualFile = file;

		return file;
	}

	@Override
	public boolean dontCache() {
		return true;
	}

	// ----- private methods -----
	private void setParentFolder(final AbstractFile file) throws FrameworkException {

		final Path parentPath = getParent();
		if (parentPath != null && parentPath instanceof StructrFilePath) {

			final StructrFilePath parentFilePath = (StructrFilePath)parentPath;
			final Folder parentFolder = (Folder)parentFilePath.getActualFile();
			if (parentFolder != null) {

				file.setProperty(AbstractFile.parent, parentFolder);
			}
		}
	}
}
