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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.sshd.common.file.SshFile;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;

/**
 *
 *
 */
public class StructrSSHFile implements SshFile {

	protected static final NullFile nullFile     = new NullFile();
	protected static final NullFolder nullFolder = new NullFolder();

	protected SecurityContext securityContext = null;
	protected StructrSSHFile parent           = null;
	protected AbstractFile actualFile         = null;
	protected String name                     = null;

	public StructrSSHFile(final SecurityContext securityContext) {

		this(null, "/", null);

		this.securityContext = securityContext;
	}

	public StructrSSHFile(final StructrSSHFile parent, final String name, final AbstractFile actualFile) {

		this.actualFile = actualFile;
		this.parent     = parent;
		this.name       = name;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + name + ")";
	}

	public SecurityContext getSecurityContext() {

		if (parent != null) {
			return parent.getSecurityContext();
		}

		return securityContext;
	}

	public AbstractFile getActualFile() {
		return actualFile;
	}

	// ----- interface SshFile -----
	@Override
	public boolean isDirectory() {

		if (parent == null) {
			return true;
		}

		if (actualFile != null) {
			return actualFile instanceof Folder;
		}

		return false;
	}

	@Override
	public boolean isFile() {

		if (actualFile != null) {
			return actualFile instanceof FileBase;
		}

		return false;
	}

	@Override
	public boolean mkdir() {

		if (actualFile == null) {

			final App app = StructrApp.getInstance(parent.getSecurityContext());
			try (final Tx tx = app.tx()) {

				actualFile = app.create(Folder.class,
					new NodeAttribute(AbstractNode.name, name),
					new NodeAttribute(AbstractFile.parent, parent != null ? parent.getActualFile() : null)
				);
				tx.success();

				return true;

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return false;
	}

	@Override
	public boolean create() throws IOException {

		if (actualFile == null) {

			final App app = StructrApp.getInstance(parent.getSecurityContext());
			try (final Tx tx = app.tx()) {

				actualFile = app.create(File.class,
					new NodeAttribute(AbstractNode.name, name),
					new NodeAttribute(AbstractFile.parent, parent != null ? parent.getActualFile() : null)
				);

				tx.success();

				return true;

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return false;
	}

	@Override
	public void truncate() throws IOException {
	}

	@Override
	public boolean move(final SshFile destination) {
		return false;
	}

	@Override
	public List<SshFile> listSshFiles() {

		final App app             = StructrApp.getInstance(parent.getSecurityContext());
		final List<SshFile> files = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (final Folder child : getFolders()) {
				files.add(new StructrSSHFile(this, child.getName(), child));
			}

			for (final FileBase child : getFiles()) {
				files.add(new StructrSSHFile(this, child.getName(), child));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return files;
	}

	public StructrSSHFile findFile(final String path) {

		final App app              = StructrApp.getInstance(getRootFolder().getSecurityContext());
		final boolean isAbsolute   = path.startsWith("/");
		final String localPath     = isAbsolute ? path.substring(1) : path;
		final String[] parts       = localPath.split("[/]+");
		final String localPart     = parts[0];

		if (".".equals(path)) {
			return this;
		}

		if ("..".equals(path)) {
			return parent;
		}

		if (isAbsolute && parent != null) {

			return getRootFolder().findFile(path);

		} else {

			// look for files/folders without parent
			try (final Tx tx = app.tx()) {

				for (final Folder folder : getFolders()) {

					final String folderName = folder.getName();
					if (localPart.equals(folderName)) {

						final StructrSSHFile matchingFolder = new StructrSSHFile(this, folderName, folder);
						if (parts.length > 1) {

							return matchingFolder.findFile(localPath.substring(folderName.length() + 1));

						} else {

							// match found
							return matchingFolder;
						}
					}
				}

				for (final FileBase file : getFiles()) {

					final String fileName = file.getName();
					if (localPart.equals(fileName)) {

						if (parts.length > 1) {

							throw new IllegalStateException("Found file where folder was expected, aborting.");
						}

						return new StructrSSHFile(this, fileName, file);
					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return new StructrSSHFile(this, path, null);
	}

	private StructrSSHFile getRootFolder() {

		if (parent != null) {
			return parent;
		}

		return this;
	}

	private List<Folder> getFolders() throws FrameworkException {

		if (actualFile != null && parent != null) {

			return actualFile.getProperty(Folder.folders);

		} else {

			return StructrApp.getInstance(getSecurityContext()).nodeQuery(Folder.class).and(AbstractFile.parent, null).getAsList();
		}
	}

	private List<FileBase> getFiles() throws FrameworkException {

		if (actualFile != null && parent != null) {

			return actualFile.getProperty(Folder.files);

		} else {

			return StructrApp.getInstance(getSecurityContext()).nodeQuery(FileBase.class).and(AbstractFile.parent, null).getAsList();
		}
	}

	@Override
	public String getAbsolutePath() {

		if (parent != null) {
			return parent.getAbsolutePath() + "/" + getName();
		}

		return "/";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOwner() {

		if (actualFile != null) {

			try (final Tx tx = StructrApp.getInstance(parent.getSecurityContext()).tx()) {

				final Principal owner = actualFile.getOwnerNode();
				String name           = null;

				if (owner != null) {

					name = owner.getProperty(AbstractNode.name);
				}

				tx.success();

				return name;

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
		return Collections.emptyMap();
	}

	@Override
	public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
	}

	@Override
	public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
		return null;
	}

	@Override
	public void setAttribute(Attribute attribute, Object value) throws IOException {
	}

	@Override
	public String readSymbolicLink() throws IOException {
		return null;
	}

	@Override
	public void createSymbolicLink(final SshFile destination) throws IOException {
	}

	@Override
	public boolean doesExist() {
		return parent == null || actualFile != null;
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	public boolean isExecutable() {
		return false;
	}

	@Override
	public boolean isRemovable() {
		return true;
	}

	@Override
	public SshFile getParentFile() {

		if (parent != null) {
			return parent;
		}

		return nullFile;
	}

	@Override
	public boolean delete() {

		if (actualFile != null) {

			final App app = StructrApp.getInstance(parent.getSecurityContext());
			try (final Tx tx = app.tx()) {

				app.delete(actualFile);
				tx.success();

				return true;

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return false;
	}

	@Override
	public long getLastModified() {

		if (actualFile != null) {

			try (final Tx tx = StructrApp.getInstance(parent.getSecurityContext()).tx()) {

				final long time = actualFile.getLastModifiedDate().getTime();

				tx.success();

				return time;

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return 0;
	}

	@Override
	public boolean setLastModified(final long time) {

		if (actualFile != null) {

			try (final Tx tx = StructrApp.getInstance(parent.getSecurityContext()).tx()) {

				actualFile.setProperty(AbstractFile.lastModifiedDate, new Date(time));

				tx.success();

				return true;

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return false;
	}

	@Override
	public long getSize() {

		if (actualFile != null) {

			try (final Tx tx = StructrApp.getInstance(parent.getSecurityContext()).tx()) {

				final long size = actualFile.getProperty(FileBase.size);

				tx.success();

				return size;

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return 0;
	}

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {

		final App app   = StructrApp.getInstance(parent.getSecurityContext());
		OutputStream os = null;

		try (final Tx tx = app.tx()) {


			if (actualFile == null) {
				create();
			}

			if (actualFile != null) {
				os = ((FileBase)actualFile).getOutputStream();
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return os;
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {

		final App app  = StructrApp.getInstance(parent.getSecurityContext());
		InputStream is = null;

		try (final Tx tx = app.tx()) {

			if (actualFile != null) {
				is = ((FileBase)actualFile).getInputStream();
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return is;
	}

	@Override
	public void handleClose() throws IOException {
	}
}
