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
package org.structr.core.traits;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

import java.util.List;
import java.util.Set;

/**
 * Interface to encapsulate query-able permissions. This interface exists
 * in order to make {@link SecurityContext} testable.
 *
 *
 */
public interface AccessControllableTrait {

	Principal getOwnerNode(final NodeInterface node);

	boolean isGranted(final NodeInterface node, final Permission permission, final SecurityContext securityContext, final boolean isCreation);
	boolean allowedBySchema(final NodeInterface node, final Principal principal, Permission permission);

	void grant(final NodeInterface node, final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException;
	void revoke(final NodeInterface node, final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException;
	void setAllowed(final NodeInterface node, final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException;

	List<RelationshipInterface<Principal, NodeInterface>> getSecurityRelationships(final NodeInterface node);
	RelationshipInterface<Principal, NodeInterface> getSecurityRelationship(final NodeInterface node, final Principal principal);
}
