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
package org.structr.core.traits.operations.accesscontrollable;

import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.List;

public abstract class GetSecurityRelationships extends FrameworkMethod<GetSecurityRelationships> {

	public abstract List<Security> getSecurityRelationships(final NodeInterface node);
	public abstract Security getSecurityRelationship(final NodeInterface node, final Principal principal);
}
