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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.common.session.Session;
import org.structr.common.SecurityContext;
import org.structr.dynamic.File;
import org.structr.web.entity.AbstractFile;


/**
 *
 */


public class StructrSSHFileSystem extends FileSystem {
	
	private static final Logger logger = Logger.getLogger(StructrSSHFileSystem.class.getName());

	private StructrSSHFile rootFolder = null;
	private Session session           = null;

	public StructrSSHFileSystem(final Session session) {

		this.rootFolder = new StructrSSHFile(SecurityContext.getSuperUserInstance());
		this.session    = session;

		this.rootFolder.setFileSystem(this);
	}
	
	@Override
	public FileSystemProvider provider() {
		
		return new FileSystemProvider() {

			// TODO: implement
			
			@Override
			public String getScheme() {
				logger.log(Level.INFO, "Method not implemented yet"); return null;
			}

			@Override
			public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet"); return null;
			}

			@Override
			public FileSystem getFileSystem(URI uri) {
				logger.log(Level.INFO, "Method not implemented yet"); return null;
			}

			@Override
			public Path getPath(URI uri) {
				logger.log(Level.INFO, "Method not implemented yet"); return null;
			}

			@Override
			public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
				
				final AbstractFile file = ((StructrSSHFile) path).getActualFile();
				
				return new SeekableByteChannel() {
					@Override
					public int read(ByteBuffer bb) throws IOException {
						return ((File) file).getInputStream().read(bb.array());
					}

					@Override
					public int write(ByteBuffer bb) throws IOException {
						((File) file).getOutputStream().write(bb.array());
						return bb.array().length;
						
					}

					@Override
					public long position() throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public SeekableByteChannel position(long l) throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public long size() throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public SeekableByteChannel truncate(long l) throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public boolean isOpen() {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public void close() throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}
				};
			}

			@Override
			public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet"); return null;
			}

			@Override
			public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");
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
				logger.log(Level.INFO, "Method not implemented yet"); return false;
			}

			@Override
			public FileStore getFileStore(Path path) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet"); return null;
			}

			@Override
			public void checkAccess(Path path, AccessMode... modes) throws IOException {
				logger.log(Level.INFO, "Checking access", new Object[] { path, modes });
			}

			@Override
			public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... options) {
				
				return (V) new PosixFileAttributeView()  {
					
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
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public void setGroup(GroupPrincipal gp) throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public void setTimes(FileTime ft, FileTime ft1, FileTime ft2) throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public UserPrincipal getOwner() throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}

					@Override
					public void setOwner(UserPrincipal up) throws IOException {
						throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
					}
					
				};
			}

			@Override
			public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
				
				logger.log(Level.INFO, "Read attributes", new Object[] { path, type, options });
				
				return (A) new StructrPosixFileAttributes((StructrSSHFile) path);
			}

			@Override
			public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet"); return null;
			}

			@Override
			public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
				logger.log(Level.INFO, "Method not implemented yet");;
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
		logger.log(Level.INFO, "Method not implemented yet"); return null;
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		logger.log(Level.INFO, "Method not implemented yet"); return null;
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
		logger.log(Level.INFO, "Method not implemented yet"); return null;
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		logger.log(Level.INFO, "Method not implemented yet"); return null;
	}

	@Override
	public WatchService newWatchService() throws IOException {
		logger.log(Level.INFO, "Method not implemented yet"); return null;
	}
	
}
