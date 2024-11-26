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

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.traits.NodeTrait;

import java.util.Date;
import java.util.Set;

/**
 * Interface to encapsulate query-able permissions. This interface exists
 * in order to make {@link SecurityContext} testable.
 *
 *
 */
public interface AccessControllable extends NodeTrait  {

	/**
	 * Return owner node
	 *
	 * @return owner
	 */
	public Principal getOwnerNode();

	/**
	 * Return true if principal has the given permission on this object.
	 *
	 * @param permission
	 * @param securityContext
	 *
	 * @return whether the security context has the given permissions on this node
	 */
	boolean isGranted(final Permission permission, final SecurityContext securityContext);
	boolean isGranted(final Permission permission, final SecurityContext context, final boolean isCreation);
	/**
	 * Grant given permission to given principal.
	 *
	 * @param permission
	 * @param principal
	 * @throws FrameworkException
	 */
	public void grant(final Permission permission, final Principal principal) throws FrameworkException;

	/**
	 * Grant given permissions to given principal.
	 *
	 * @param permissions
	 * @param principal
	 * @throws FrameworkException
	 */
	public void grant(final Set<Permission> permissions, final Principal principal) throws FrameworkException;

	/**
	 * Grant given permissions to given principal.
	 *
	 * @param permissions
	 * @param principal
	 * @param ctx
	 * @throws FrameworkException
	 */
	public void grant(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException;

	/**
	 * Revoke given permission from given principal.
	 *
	 * @param permission
	 * @param principal
	 * @throws FrameworkException
	 */
	public void revoke(final Permission permission, final Principal principal) throws FrameworkException;

	/**
	 * Revoke given permissions from given principal.
	 *
	 * @param permissions
	 * @param principal
	 * @throws FrameworkException
	 */
	public void revoke(final Set<Permission> permissions, final Principal principal) throws FrameworkException;

	/**
	 * Revoke given permissions from given principal.
	 *
	 * @param permissions
	 * @param principal
	 * @param ctx
	 * @throws FrameworkException
	 */
	public void revoke(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException;

	/**
	 * Applies the given permissions to the given principal.
	 * Permissions not in the set of permissions will be removed if already set
	 *
	 * @param permissions
	 * @param principal
	 * @throws FrameworkException
	 */
	public void setAllowed(final Set<Permission> permissions, final Principal principal) throws FrameworkException;

	/**
	 * Applies the given permissions to the given principal.
	 * Permissions not in the set of permissions will be removed if already set
	 *
	 * @param permissions
	 * @param principal
	 * @param ctx
	 * @throws FrameworkException
	 */
	public void setAllowed(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException;

	/**
	 * Return the (cached) incoming relationship between this node and the
	 * given principal which holds the security information.
	 *
	 * @param principal
	 * @return incoming security relationship
	 */
	public Security getSecurityRelationship(final Principal principal);

	// visibility
	public boolean isVisibleToPublicUsers();
	public boolean isVisibleToAuthenticatedUsers();
	public boolean isHidden();

	// access
	public Date getCreatedDate();
	public Date getLastModifiedDate();
}
