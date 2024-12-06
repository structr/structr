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

import org.structr.core.entity.SchemaGrant;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

/**
 *
 *
 */
public class SchemaGrantTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaGrant {

	public SchemaGrantTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getPrincipalId() {

		final PropertyKey<NodeInterface> key = traits.key("principal");
		final NodeInterface principal        = wrappedObject.getProperty(key);

		return principal.getUuid();

		// When Structr starts, the schema is not yet compiled and the Principal class does not yet
		// exist, so we only get GenericNode instances. Relationships are filtered by node type, and
		// that filter removes GenericNode instances, so we must use the unfiltered relationship
		// collection and check manually.

		/*
		for (final RelationshipInterface rel : wrappedObject.getIncomingRelationships()) {

			if (rel != null && rel instanceof PrincipalSchemaGrantRelationship) {

				return rel.getSourceNodeId();
			}
		}
		*/
	}

	@Override
	public String getPrincipalName() {

		final PropertyKey<NodeInterface> key = traits.key("principal");
		final NodeInterface principal        = wrappedObject.getProperty(key);

		return principal.getName();

		// When Structr starts, the schema is not yet compiled and the Principal class does not yet
		// exist, so we only get GenericNode instances. Relationships are filtered by node type, and
		// that filter removes GenericNode instances, so we must use the unfiltered relationship
		// collection and check manually.

		/*
		for (final RelationshipInterface rel : wrappedObject.getIncomingRelationships()) {

			if (rel != null && rel instanceof PrincipalSchemaGrantRelationship) {

				final NodeInterface _principal = rel.getSourceNode();
				if (_principal != null) {

					return _principal.getProperty(AbstractNode.name);
				}
			}
		}
		*/
	}

	@Override
	public boolean allowRead() {
		return wrappedObject.getProperty(traits.key("allowRead"));
	}

	@Override
	public boolean allowWrite() {
		return wrappedObject.getProperty(traits.key("allowWrite"));
	}

	@Override
	public boolean allowDelete() {
		return wrappedObject.getProperty(traits.key("allowDelete"));
	}

	@Override
	public boolean allowAccessControl() {
		return wrappedObject.getProperty(traits.key("allowAccessControl"));
	}
}
