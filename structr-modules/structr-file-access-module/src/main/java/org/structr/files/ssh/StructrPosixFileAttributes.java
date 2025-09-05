/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.AbstractFile;

import java.nio.file.attribute.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class StructrPosixFileAttributes implements PosixFileAttributes {

	private static final Logger logger = LoggerFactory.getLogger(StructrPosixFileAttributes.class.getName());

	final AbstractFile file;

	StructrPosixFileAttributes(final StructrSSHFile path) {
		file = path.getActualFile();
	}

	@Override
	public UserPrincipal owner() {

		UserPrincipal owner = null;

		try (Tx tx = StructrApp.getInstance().tx()) {
			owner = file.as(AccessControllable.class).getOwnerNode()::getName;
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return owner;
	}

	@Override
	public GroupPrincipal group() {
		final List<Group> groups = Iterables.toList(file.as(AccessControllable.class).getOwnerNode().getGroups());
		return groups != null && groups.size() > 0 ? groups.get(0)::getName : null;
	}

	@Override
	public Set<PosixFilePermission> permissions() {

		final Set<PosixFilePermission> permissions = new HashSet<>();
		permissions.add(PosixFilePermission.OWNER_READ);

		return permissions;
	}

	@Override
	public FileTime lastModifiedTime() {

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance().tx()) {
			time = FileTime.fromMillis(file.getLastModifiedDate().getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;

	}

	@Override
	public FileTime lastAccessTime() {

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance().tx()) {
			// Same as lastModifiedTime() as we don't store last access time in Structr yet
			time = FileTime.fromMillis(file.getLastModifiedDate().getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public FileTime creationTime() {

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance().tx()) {
			time = FileTime.fromMillis(file.getCreatedDate().getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public boolean isRegularFile() {

		boolean isRegularFile = false;

		try (Tx tx = StructrApp.getInstance().tx()) {
			isRegularFile = file.is(StructrTraits.FILE);
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return isRegularFile;
	}

	@Override
	public boolean isDirectory() {

		boolean isDirectory = false;

		try (Tx tx = StructrApp.getInstance().tx()) {
			isDirectory = file.is(StructrTraits.FOLDER);
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

		long size = 0;

		try (Tx tx = StructrApp.getInstance().tx()) {
			size = StorageProviderFactory.getStorageProvider(file).size();
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return size;
	}

	@Override
	public Object fileKey() {

		String uuid = null;
		try (Tx tx = StructrApp.getInstance().tx()) {
			uuid = file.getUuid();
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return uuid;
	}

}
