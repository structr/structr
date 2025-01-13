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
package org.structr.schema;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.property.PropertyKey;

import java.util.Set;

public class DynamicNodeTraitDefinition extends AbstractDynamicTraitDefinition<SchemaNode> {

	public DynamicNodeTraitDefinition(final SchemaNode schemaNode) {
		super(schemaNode);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Set<PropertyKey> keys = super.getPropertyKeys();

		// linked properties
		for (final SchemaRelationshipNode outRel : schemaNode.getRelatedTo()) {

			try {

				keys.add(outRel.createKey(schemaNode, true));

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}

		for (final SchemaRelationshipNode inRel : schemaNode.getRelatedFrom()) {

			try {

				keys.add(inRel.createKey(schemaNode, false));

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}

		return keys;
	}
}
