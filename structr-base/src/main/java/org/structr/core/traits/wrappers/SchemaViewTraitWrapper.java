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

import org.structr.api.util.Iterables;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;

public class SchemaViewTraitWrapper extends AbstractNodeTraitWrapper implements SchemaView {

	public SchemaViewTraitWrapper(Traits traits, NodeInterface node) {
		super(traits, node);
	}

	@Override
	public Iterable<SchemaProperty> getSchemaProperties() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(SchemaViewTraitDefinition.SCHEMA_PROPERTIES_PROPERTY);

		return Iterables.map(n -> n.as(SchemaProperty.class), wrappedObject.getProperty(key));
	}

	@Override
	public String getStaticSchemaNodeName() {
		return wrappedObject.getProperty(traits.key(SchemaViewTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY));
	}

	@Override
	public String getNonGraphProperties() {
		return wrappedObject.getProperty(traits.key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY));
	}

	@Override
	public String getSortOrder() {
		return wrappedObject.getProperty(traits.key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY));
	}
}
