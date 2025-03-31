/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 */
public class Permissions {

	private static final Map<String, Permission> permissionMap = new HashMap<>();

	static {

 		permissionMap.put(Permission.read.name(),          Permission.read);
 		permissionMap.put(Permission.write.name(),         Permission.write);
		permissionMap.put(Permission.delete.name(),        Permission.delete);
		permissionMap.put(Permission.accessControl.name(), Permission.accessControl);
	}

	public static Permission valueOf(final String permissionString) {
		return permissionMap.get(permissionString);
	}
}
