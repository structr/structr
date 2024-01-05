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
package org.structr.files.ssh.filesystem.path.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;

import java.nio.file.attribute.*;
import java.util.*;

/**
 *
 */
public class StructrNodeAttributes implements PosixFileAttributes, DosFileAttributes {

	private static final Logger logger              = LoggerFactory.getLogger(StructrNodeAttributes.class.getName());
	public static final Set<String> SUPPORTED_VIEWS = new LinkedHashSet<>(Arrays.asList(new String[] { "owner", "dos", "basic", "posix", "permissions" } ));

	private SecurityContext securityContext = null;
	private GraphObject node                = null;

	public StructrNodeAttributes(final SecurityContext securityContext, final GraphObject node) {

		this.securityContext = securityContext;
		this.node            = node;
	}

	@Override
	public UserPrincipal owner() {
		return null;
	}

	@Override
	public GroupPrincipal group() {
		return null;
	}

	@Override
	public FileTime lastModifiedTime() {

		if (node == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			time = FileTime.fromMillis(node.getProperty(GraphObject.lastModifiedDate).getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public FileTime lastAccessTime() {

		if (node == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			// Same as lastModifiedTime() as we don't store last access time in Structr yet
			time = FileTime.fromMillis(node.getProperty(GraphObject.lastModifiedDate).getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public FileTime creationTime() {

		if (node == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			time = FileTime.fromMillis(node.getProperty(GraphObject.createdDate).getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public boolean isRegularFile() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return true;
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
		return 0L;
	}

	@Override
	public Object fileKey() {

		if (node == null) {
			return null;
		}

		String uuid = null;
		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			uuid = node.getUuid();
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

		/*
		permissions.add(PosixFilePermission.GROUP_READ);
		permissions.add(PosixFilePermission.GROUP_WRITE);

		permissions.add(PosixFilePermission.OTHERS_READ);
		permissions.add(PosixFilePermission.OTHERS_WRITE);
		*/

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
		final int splitPos            = filter.indexOf(":");
		final String prefix           = filter.substring(0, splitPos >= 0 ? splitPos : filter.length());

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
			map.put("group", group());
			map.put("owner", owner());
		}

		// permissions only
		if ("permissions".equals(prefix)) {

			map.put("permissions", permissions());
		}

		if ("owner".equals(prefix)) {

			map.put("owner", owner());
		}

		return map;
	}
}
