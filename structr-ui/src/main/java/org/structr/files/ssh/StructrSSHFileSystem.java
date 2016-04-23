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
package org.structr.files.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.common.session.Session;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;

/**
 *
 */
public class StructrSSHFileSystem extends FileSystem {

	private static final Logger logger = Logger.getLogger(StructrSSHFileSystem.class.getName());

	private StructrSSHFile rootFolder       = null;
	private Session        session          = null;
	private SecurityContext securityContext = null;

	public StructrSSHFileSystem(final SecurityContext securityContext, final Session session) {
		this.securityContext = securityContext;
		this.session         = session;
		this.rootFolder      = new StructrSSHFile(securityContext);

		this.rootFolder.setFileSystem(this);
	}

	@Override
	public FileSystemProvider provider() {

		return new FileSystemProvider() {
			
			@Override
			public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
				
				OutputStream os = null;

				FileBase actualFile = (FileBase) ((StructrSSHFile) path).getActualFile();
				
				try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

					if (actualFile == null) {
						
						actualFile = (FileBase) create(path);
					}

					if (actualFile != null) {
						os = ((FileBase) actualFile).getOutputStream();
					}

					tx.success();

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "", fex);
					throw new IOException(fex);
				}

				return os;
			}

			@Override
			public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
				// Remote file => file node in Structr
				
				InputStream inputStream = null;
				
				try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {
		
					final FileBase fileNode = (FileBase) ((StructrSSHFile) path).getActualFile();
					inputStream = fileNode.getInputStream();
					
					tx.success();

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "", fex);
					throw new IOException(fex);
				}
				
				return inputStream;
			}

			@Override
			public String getScheme() {
				logger.log(Level.INFO, "Method not implemented yet");
				return null;
			}

			@Override
			public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
				return null;
			}

			@Override
			public FileSystem getFileSystem(URI uri) {
				logger.log(Level.INFO, "Method not implemented yet");
				return null;
			}

			@Override
			public Path getPath(URI uri) {
				logger.log(Level.INFO, "Method not implemented yet");
				return null;
			}

			@Override
			public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {

				SeekableByteChannel channel = null;

				final FileBase fileNode = (FileBase) ((StructrSSHFile) path).getActualFile();
				
				if (fileNode != null) {

					try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
						final Path filePath = FileHelper.getPath(fileNode);
						channel = Files.newByteChannel(filePath);

						tx.success();
					} catch (FrameworkException fex) {
						logger.log(Level.SEVERE, "", fex);
						throw new IOException(fex);
					}
				}

				return channel;
			}

			@Override
			public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
				
				return new DirectoryStream() {
					
					boolean closed = false;
					
					@Override
					public Iterator iterator() {
						
						if (!closed) {
							
							final App app = StructrApp.getInstance(securityContext);
							final List<StructrSSHFile> files = new LinkedList<>();
							
							final StructrSSHFile thisDir = (StructrSSHFile) dir;

							try (final Tx tx = app.tx()) {

								for (final Folder child : thisDir.getFolders()) {
									files.add(new StructrSSHFile(thisDir, child.getName(), child));
								}

								for (final FileBase child : thisDir.getFiles()) {
									files.add(new StructrSSHFile(thisDir, child.getName(), child));
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

			@Override
			public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
				
				final StructrSSHFile parent = (StructrSSHFile) dir.getParent();

				final App app = StructrApp.getInstance(securityContext);
				try (final Tx tx = app.tx()) {

					app.create(Folder.class,
						new NodeAttribute(AbstractNode.name, dir.getFileName().toString()),
						new NodeAttribute(AbstractFile.parent, parent != null ? parent.getActualFile() : null)
					);
					tx.success();

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "", fex);
					throw new IOException(fex);
				}

			}

			@Override
			public void delete(Path path) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
			}

			@Override
			public void copy(Path source, Path target, CopyOption... options) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
			}

			@Override
			public void move(Path source, Path target, CopyOption... options) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
			}

			@Override
			public boolean isSameFile(Path path, Path path2) throws IOException {
				return path != null && path.equals(path);
			}

			@Override
			public boolean isHidden(Path path) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
				return false;
			}

			@Override
			public FileStore getFileStore(Path path) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
				return null;
			}

			@Override
			public void checkAccess(Path path, AccessMode... modes) throws IOException {
				logger.log(Level.INFO, "Checking access", new Object[]{path, modes});
			}

			@Override
			public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... options) {

				return (V) new PosixFileAttributeView() {

					@Override
					public String name() {
						return "posix";
					}

					@Override
					public PosixFileAttributes readAttributes() throws IOException {
						return new StructrPosixFileAttributes((StructrSSHFile) path);
					}

					@Override
					public void setPermissions(Set<PosixFilePermission> set) throws IOException {
						logger.log(Level.INFO, "Method not implemented yet");
					}

					@Override
					public void setGroup(GroupPrincipal gp) throws IOException {
						logger.log(Level.INFO, "Method not implemented yet");
					}

					@Override
					public void setTimes(FileTime ft, FileTime ft1, FileTime ft2) throws IOException {
						logger.log(Level.INFO, "Method not implemented yet");
					}

					@Override
					public UserPrincipal getOwner() throws IOException {
						logger.log(Level.INFO, "Method not implemented yet");
						return null;
					}

					@Override
					public void setOwner(UserPrincipal up) throws IOException {
						logger.log(Level.INFO, "Method not implemented yet");
					}

				};
			}

			@Override
			public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {

				if (path != null) {
					
					if (path instanceof StructrSSHFile) {
						
						final StructrSSHFile sshFile = (StructrSSHFile) path;
						
						if (sshFile.getActualFile() == null) {
							throw new NoSuchFileException("SSH file doesn't exist");
						}
	
						BasicFileAttributes attrs = new StructrPosixFileAttributes((StructrSSHFile) path);
	
						return (A) attrs;
					}
				}
				
				throw new IOException("Unable to read attributes: Path is null");
			}

			@Override
			public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
				return null;
			}

			@Override
			public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");;
			}
			
			private AbstractFile create(final Path path) throws IOException {
				
				final StructrSSHFile parent = (StructrSSHFile) path.getParent();

				AbstractFile newFile = null;

				final App app = StructrApp.getInstance(securityContext);
				try (final Tx tx = app.tx()) {
					
					final String fileName = path.getFileName().toString();
					
					final Folder parentFolder = (Folder) parent.getActualFile();

					newFile = app.create(File.class,
						new NodeAttribute(AbstractNode.name, fileName),
						new NodeAttribute(AbstractFile.parent, parentFolder)
					);

					tx.success();

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "", fex);
					throw new IOException(fex);
				}

				return newFile;
			}

		};

	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public String getSeparator() {
		return "/";
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		logger.log(Level.INFO, "Method not implemented yet");
		return null;
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		logger.log(Level.INFO, "Method not implemented yet");
		return null;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {

		final Set<String> views = new HashSet<>();

		views.add("posix");

		return views;
	}

	@Override
	public Path getPath(String string, String... strings) {

		if ("/".equals(string)) {
			return rootFolder;
		}

		return rootFolder.findFile(string);
	}

	@Override
	public PathMatcher getPathMatcher(String string) {
		logger.log(Level.INFO, "Method not implemented yet");
		return null;
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		logger.log(Level.INFO, "Method not implemented yet");
		return null;
	}

	@Override
	public WatchService newWatchService() throws IOException {
		logger.log(Level.INFO, "Method not implemented yet");
		return null;
	}

}
