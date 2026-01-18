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
package org.structr.core.traits.wrappers;

import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;

/**
 *
 *
 */
public class SchemaGrantTraitWrapper extends AbstractNodeTraitWrapper implements SchemaGrant {

	public SchemaGrantTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getPrincipalName() {

		final Principal principal = getPrincipal();
		if (principal != null) {

			return principal.getName();
		}

		return null;
	}

	@Override
	public Principal getPrincipal() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY));
		if (node != null) {

			return node.as(Principal.class);
		}

		return null;
	}

	@Override
	public SchemaNode getSchemaNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY));
		if (node != null) {

			return node.as(SchemaNode.class);
		}

		return null;
	}

	@Override
	public String getStaticSchemaNodeName() {
		return wrappedObject.getProperty(traits.key(SchemaGrantTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY));
	}

	@Override
	public boolean allowRead() {
		return wrappedObject.getProperty(traits.key(SchemaGrantTraitDefinition.ALLOW_READ_PROPERTY));
	}

	@Override
	public boolean allowWrite() {
		return wrappedObject.getProperty(traits.key(SchemaGrantTraitDefinition.ALLOW_WRITE_PROPERTY));
	}

	@Override
	public boolean allowDelete() {
		return wrappedObject.getProperty(traits.key(SchemaGrantTraitDefinition.ALLOW_DELETE_PROPERTY));
	}

	@Override
	public boolean allowAccessControl() {
		return wrappedObject.getProperty(traits.key(SchemaGrantTraitDefinition.ALLOW_ACCESS_CONTROL_PROPERTY));
	}
}
