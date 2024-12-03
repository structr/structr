package org.structr.core.entity;

import org.structr.common.Permission;
import org.structr.core.traits.RelationshipTrait;

import java.util.Set;

public interface Security extends RelationshipTrait {

	boolean isAllowed(final Permission permission);
	void setAllowed(final Set<String> allowed);
	void setAllowed(final Permission... allowed);
	Set<String> getPermissions();
	void addPermission(final Permission permission);
	void addPermissions(final Set<Permission> permissions);
	void removePermission(final Permission permission);
	void removePermissions(final Set<Permission> permissions);
}
