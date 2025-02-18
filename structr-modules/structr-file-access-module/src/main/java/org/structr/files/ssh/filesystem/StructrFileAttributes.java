/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.files.ssh.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.IOException;
import java.nio.file.attribute.*;
import java.util.*;

/**
 *
 */
public class  StructrFileAttributes implements PosixFileAttributes, DosFileAttributes, PosixFileAttributeView {

	private static final Logger logger              = LoggerFactory.getLogger(StructrFileAttributes.class.getName());
	public static final Set<String> SUPPORTED_VIEWS = new LinkedHashSet<>(Arrays.asList("owner", "dos", "basic", "posix", "permissions"));

	private SecurityContext securityContext = null;
	private AbstractFile file               = null;

	public StructrFileAttributes(final SecurityContext securityContext, final AbstractFile file) {

		this.securityContext = securityContext;
		this.file            = file;
	}

	@Override
	public UserPrincipal owner() {

		if (file == null) {
			return null;
		}

		UserPrincipal owner = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Principal fileOwner = file.as(AccessControllable.class).getOwnerNode();
			if (fileOwner == null) {

				owner = securityContext.getUser(false)::getName;

			} else {
				owner = fileOwner::getName;
			}
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return owner;
	}

	@Override
	public GroupPrincipal group() {

		if (file == null) {
			return null;
		}

		final List<Group> groups = new LinkedList<>();

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Principal owner = file.as(AccessControllable.class).getOwnerNode();
			if (owner != null) {

				groups.addAll(Iterables.toList(owner.getGroups()));
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return groups.size() > 0 ? groups.get(0)::getName : null;
	}

	@Override
	public FileTime lastModifiedTime() {

		if (file == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Date date = file.getLastModifiedDate();
			if (date != null) {

				time = FileTime.fromMillis(date.getTime());
			}

			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public FileTime lastAccessTime() {
		return lastModifiedTime();
	}

	@Override
	public FileTime creationTime() {

		if (file == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Date date = file.getCreatedDate();
			if (date != null) {

				time = FileTime.fromMillis(date.getTime());
			}
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public boolean isRegularFile() {

		if (file == null) {
			return false;
		}

		boolean isRegularFile = false;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			isRegularFile = file instanceof File;
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return isRegularFile;
	}

	@Override
	public boolean isDirectory() {

		if (file == null) {
			return false;
		}

		boolean isDirectory = false;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			isDirectory = file instanceof Folder;
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return isDirectory;
	}

	@Override
	public boolean isSymbolicLink() {
		// Structr doesn't support symbolic links yet
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public long size() {

		if (file == null) {
			return 0L;
		}

		long size = 0;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			if (file instanceof File) {

				final Number s = StorageProviderFactory.getStorageProvider(file).size();
				if (s != null) {

					size = s.longValue();
				}
			}

			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return size;
	}

	@Override
	public Object fileKey() {

		if (file == null) {
			return null;
		}

		String uuid = null;
		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			uuid = file.getUuid();
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return uuid;
	}

	@Override
	public Set<PosixFilePermission> permissions() {

		final Set<PosixFilePermission> permissions = new HashSet<>();

		permissions.add(PosixFilePermission.OWNER_READ);
		permissions.add(PosixFilePermission.OWNER_WRITE);

		if (file != null) {

			if (file instanceof Folder) {
				permissions.add(PosixFilePermission.OWNER_EXECUTE);
			}

			try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

				if (file.isVisibleToPublicUsers()) {

					permissions.add(PosixFilePermission.OTHERS_READ);
					permissions.add(PosixFilePermission.OTHERS_WRITE);

					if (file instanceof Folder) {
						permissions.add(PosixFilePermission.OTHERS_EXECUTE);
					}
				}

				if (file.isVisibleToAuthenticatedUsers()) {

					permissions.add(PosixFilePermission.GROUP_READ);
					permissions.add(PosixFilePermission.GROUP_WRITE);

					if (file instanceof Folder) {
						permissions.add(PosixFilePermission.GROUP_EXECUTE);
					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.error("", fex);
			}
		}

		return permissions;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public boolean isArchive() {
		return false;
	}

	@Override
	public boolean isSystem() {
		return false;
	}

	public Map<String, Object> toMap(final String filter) {

		final Map<String, Object> map = new HashMap<>();

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final String prefix = filter.substring(0, filter.indexOf(":"));
			final GroupPrincipal group = group();
			final UserPrincipal owner = owner();

			if ("dos".equals(prefix)) {

				map.put("hidden", isHidden());
				map.put("archive", isArchive());
				map.put("system", isSystem());
				map.put("readonly", isReadOnly());
			}

			if (!"owner".equals(prefix)) {

				map.put("lastModifiedTime", lastModifiedTime());
				map.put("lastAccessTime", lastAccessTime());
				map.put("creationTime", creationTime());
				map.put("size", size());
				map.put("isRegularFile", isRegularFile());
				map.put("isDirectory", isDirectory());
				map.put("isSymbolicLink", isSymbolicLink());
				map.put("isOther", isOther());
				map.put("fileKey", fileKey());
			}

			// POSIX properties
			if ("posix".equals(prefix)) {

				map.put("permissions", permissions());

				if (group != null) {
					map.put("group", group.getName());
				}

				if (owner != null) {
					map.put("owner", owner.getName());

					// set group to owner
					if (group == null) {
						map.put("group", owner.getName());
					}
				}
			}

			// permissions only
			if ("permissions".equals(prefix)) {

				map.put("permissions", permissions());
			}

			if ("owner".equals(prefix) && owner != null) {

				map.put("owner", owner().getName());
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return map;
	}

	// ----- interface PosixFileAttributeView -----
	@Override
	public String name() {

		if (file == null) {
			return null;
		}

		String name = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			name = file.getName();
			tx.success();

		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return name;
	}

	@Override
	public PosixFileAttributes readAttributes() throws IOException {
		return this;
	}

	@Override
	public void setPermissions(final Set<PosixFilePermission> perms) throws IOException {

		if (file == null) {
			return;
		}

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			file.setVisibleToAuthenticatedUsers(perms.contains(PosixFilePermission.GROUP_READ));
			file.setVisibleToPublicUsers(perms.contains(PosixFilePermission.OTHERS_READ));

			tx.success();

		} catch (FrameworkException fex) {
			logger.error("Unable to set mapped file permissions for " + file, fex);
		}
	}

	@Override
	public void setGroup(final GroupPrincipal group) throws IOException {
	}

	@Override
	public void setTimes(final FileTime lastModifiedTime, final FileTime lastAccessTime, final FileTime createTime) throws IOException {
	}

	@Override
	public UserPrincipal getOwner() throws IOException {
		return owner();
	}

	@Override
	public void setOwner(final UserPrincipal owner) throws IOException {
	}
}
