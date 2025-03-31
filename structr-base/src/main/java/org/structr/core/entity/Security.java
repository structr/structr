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
package org.structr.core.entity;

import org.structr.common.Permission;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.RelationshipTrait;

import java.util.Set;

public interface Security extends RelationshipTrait {

	Principal getSourceNode();
	NodeInterface getTargetNode();

	boolean isAllowed(final Permission permission);
	void setAllowed(final Set<String> allowed);
	void setAllowed(final Permission... allowed);
	Set<String> getPermissions();
	void addPermission(final Permission permission);
	void addPermissions(final Set<Permission> permissions);
	void removePermission(final Permission permission);
	void removePermissions(final Set<Permission> permissions);
}
