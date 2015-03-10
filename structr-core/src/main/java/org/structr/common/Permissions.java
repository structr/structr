package org.structr.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Christian Morgner
 */
public class Permissions {

	private static final Map<String, Permission> permissionMap = new LinkedHashMap<>();

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
