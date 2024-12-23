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
package org.structr.core.traits.wrappers;

import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.accesscontrollable.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AccessControllableTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements AccessControllable {

	public AccessControllableTraitWrapper(Traits traits, NodeInterface node) {
		super(traits, node);
	}

	@Override
	public final Principal getOwnerNode() {
		return traits.getMethod(GetOwnerNode.class).getOwnerNode(wrappedObject);
	}

	@Override
	public boolean allowedBySchema(final Principal principal, final Permission permission) {
		return traits.getMethod(AllowedBySchema.class).allowedBySchema(wrappedObject, principal, permission);
	}

	@Override
	public boolean isGranted(final Permission permission, final SecurityContext securityContext) {
		return traits.getMethod(IsGranted.class).isGranted(wrappedObject, permission, securityContext, false);
	}

	@Override
	public final Security getSecurityRelationship(final Principal p) {
		return traits.getMethod(GetSecurityRelationships.class).getSecurityRelationship(wrappedObject, p);
	}

	@Override
	public List<Security> getSecurityRelationships() {
		return traits.getMethod(GetSecurityRelationships.class).getSecurityRelationships(wrappedObject);
	}

	@Override
	public final void grant(final Permission permission, final Principal principal) throws FrameworkException {
		grant(Collections.singleton(permission), principal, getSecurityContext());
	}

	@Override
	public final void grant(final Set<Permission> permissions, final Principal principal) throws FrameworkException {
		grant(permissions, principal, getSecurityContext());
	}

	@Override
	public final void grant(final Set<Permission> permissions, final Principal principal, SecurityContext ctx) throws FrameworkException {
		traits.getMethod(Grant.class).grant(wrappedObject, permissions, principal, ctx);
	}

	@Override
	public final void revoke(Permission permission, Principal principal) throws FrameworkException {
		revoke(Collections.singleton(permission), principal, getSecurityContext());
	}

	@Override
	public final void revoke(final Set<Permission> permissions, Principal principal) throws FrameworkException {
		revoke(permissions, principal, getSecurityContext());
	}

	@Override
	public final void revoke(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
		traits.getMethod(Revoke.class).revoke(wrappedObject, permissions, principal, ctx);
	}

	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal) throws FrameworkException {
		setAllowed(permissions, principal, getSecurityContext());
	}

	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
		traits.getMethod(SetAllowed.class).setAllowed(wrappedObject, permissions, principal, ctx);
	}
}
