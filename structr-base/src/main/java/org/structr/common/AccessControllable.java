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
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

import java.util.Date;
import java.util.Set;

/**
 * Interface to encapsulate query-able permissions. This interface exists
 * in order to make {@link SecurityContext} testable.
 *
 *
 */
public interface AccessControllable {

	public PrincipalInterface getOwnerNode();

	public boolean isGranted(final Permission permission, final SecurityContext securityContext);

	public void grant(final Permission permission, final PrincipalInterface principal) throws FrameworkException;
	public void grant(final Set<Permission> permissions, final PrincipalInterface principal) throws FrameworkException;
	public void grant(final Set<Permission> permissions, final PrincipalInterface principal, final SecurityContext ctx) throws FrameworkException;

	public void revoke(final Permission permission, final PrincipalInterface principal) throws FrameworkException;
	public void revoke(final Set<Permission> permissions, final PrincipalInterface principal) throws FrameworkException;
	public void revoke(final Set<Permission> permissions, final PrincipalInterface principal, final SecurityContext ctx) throws FrameworkException;

	public void setAllowed(final Set<Permission> permissions, final PrincipalInterface principal) throws FrameworkException;
	public void setAllowed(final Set<Permission> permissions, final PrincipalInterface principal, final SecurityContext ctx) throws FrameworkException;


	// visibility
	public boolean isVisibleToPublicUsers();
	public boolean isVisibleToAuthenticatedUsers();
	public boolean isHidden();

	// access
	public Date getCreatedDate();
	public Date getLastModifiedDate();
}
