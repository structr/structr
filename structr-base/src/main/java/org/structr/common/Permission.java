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
package org.structr.common;

/**
 * A type-safe enumeration of structr's access privileges.
 *
 *
 */
public interface Permission {

	public static final Permission read             = new PermissionImpl("read");
	public static final Permission write            = new PermissionImpl("write");
	public static final Permission delete           = new PermissionImpl("delete");
	public static final Permission accessControl    = new PermissionImpl("accessControl");

	public static final Permission[] allPermissions = { read, write, delete, accessControl };

	public String name();

	static class PermissionImpl implements Permission {

		private String name = null;

		private PermissionImpl(final String name) {

			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}
	}
}
