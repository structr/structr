/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.files.ssh.filesystem.path.page;

import java.io.IOException;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 */
public class StructrDOMAttributes implements PosixFileAttributes, DosFileAttributes, PosixFileAttributeView {

	private static final Logger logger              = LoggerFactory.getLogger(StructrDOMAttributes.class.getName());
	public static final Set<String> SUPPORTED_VIEWS = new LinkedHashSet<>(Arrays.asList(new String[] { "unix" /* "owner", "dos", "basic", "posix", "permissions"*/  } ));

	private SecurityContext securityContext = null;
	private DOMNode domNode                 = null;

	public StructrDOMAttributes(final SecurityContext securityContext, final DOMNode domNode) {

		this.securityContext = securityContext;
		this.domNode            = domNode;
	}

	@Override
	public UserPrincipal owner() {

		if (domNode == null) {
			return null;
		}

		UserPrincipal owner = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Principal fileOwner = domNode.getOwnerNode();
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

		if (domNode == null) {
			return null;
		}

		final List<Group> groups = new LinkedList<>();

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Principal fileOwner = domNode.getOwnerNode();
			if (fileOwner != null) {

				groups.addAll(fileOwner.getProperty(User.groups));
			}
			tx.success();

		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return groups != null && groups.size() > 0 ? groups.get(0)::getName : null;
	}

	@Override
	public FileTime lastModifiedTime() {

		if (domNode == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			time = FileTime.fromMillis(domNode.getLastModifiedDate().getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;

	}

	@Override
	public FileTime lastAccessTime() {

		if (domNode == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			// Same as lastModifiedTime() as we don't store last access time in Structr yet
			time = FileTime.fromMillis(domNode.getLastModifiedDate().getTime());
			tx.success();
		} catch (FrameworkException fex) {
			logger.error("", fex);
		}

		return time;
	}

	@Override
	public FileTime creationTime() {

		if (domNode == null) {
			return null;
		}

		FileTime time = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			time = FileTime.fromMillis(domNode.getCreatedDate().getTime());
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

		if (domNode == null) {
			return null;
		}

		String uuid = null;
		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			uuid = domNode.getUuid();
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
		permissions.add(PosixFilePermission.OWNER_EXECUTE);

		if (domNode != null) {

			try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

				if (domNode.isVisibleToPublicUsers()) {

					permissions.add(PosixFilePermission.OTHERS_READ);
					permissions.add(PosixFilePermission.OTHERS_WRITE);
					permissions.add(PosixFilePermission.OTHERS_EXECUTE);
				}

				if (domNode.isVisibleToAuthenticatedUsers()) {

					permissions.add(PosixFilePermission.GROUP_READ);
					permissions.add(PosixFilePermission.GROUP_WRITE);
					permissions.add(PosixFilePermission.GROUP_EXECUTE);
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
		final String prefix           = filter.substring(0, filter.indexOf(":"));

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

	// ----- interface PosixFileAttributeView -----
	@Override
	public String name() {

		if (domNode == null) {
			return null;
		}

		String name = null;

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			name = domNode.getName();
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

		if (domNode == null) {
			return;
		}

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			domNode.setProperty(AbstractNode.visibleToAuthenticatedUsers, perms.contains(PosixFilePermission.GROUP_READ));
			domNode.setProperty(AbstractNode.visibleToPublicUsers,        perms.contains(PosixFilePermission.OTHERS_READ));

			tx.success();

		} catch (FrameworkException fex) {
			logger.error("Unable to set mapped file permissions for " + domNode, fex);
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
