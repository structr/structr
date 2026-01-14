/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class StructrRootAttributes implements PosixFileAttributes, DosFileAttributes {

	public static final Set<String> SUPPORTED_VIEWS = new LinkedHashSet<>(Arrays.asList(new String[] { "owner", "dos", "basic", "posix" } ));

	private String key = null;

	public StructrRootAttributes(final String key) {
		this.key = key;
	}

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public FileTime lastAccessTime() {
		return FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public FileTime creationTime() {
		return FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
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
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public long size() {
		return 0;
	}

	@Override
	public Object fileKey() {
		return key;
	}

	@Override
	public UserPrincipal owner() {

		return new UserPrincipal() {

			@Override
			public String getName() {
				return "superadmin";
			}
		};
	}

	@Override
	public GroupPrincipal group() {

		return new GroupPrincipal() {

			@Override
			public String getName() {
				return "superadmin";
			}
		};
	}

	@Override
	public Set<PosixFilePermission> permissions() {

		final Set<PosixFilePermission> permissions = new HashSet<>();

		permissions.add(PosixFilePermission.OWNER_READ);
		permissions.add(PosixFilePermission.OWNER_WRITE);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);

		permissions.add(PosixFilePermission.GROUP_READ);
		permissions.add(PosixFilePermission.GROUP_WRITE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);

		permissions.add(PosixFilePermission.OTHERS_READ);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);

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
			map.put("group", group().getName());
			map.put("owner", owner().getName());
		}

		if ("owner".equals(prefix)) {

			map.put("owner", owner().getName());
		}

		return map;
	}
}
