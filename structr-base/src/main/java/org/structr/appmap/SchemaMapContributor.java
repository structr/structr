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
package org.structr.appmap;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;

import java.util.List;
import java.util.Map;

/**
 * The data model: every schema type, every schema method with the type it belongs to, the
 * schema relationships and the inheritance between types. The scripts of the methods and of
 * the function properties connect their owner to the types, methods and external services
 * they name.
 */
public class SchemaMapContributor implements ApplicationMapContributor {

	// ownership (1:1, 1:n) binds; a many-to-many association is only a reference
	private static final Map<String, Double> MULTIPLICITY = Map.of("1:1", 1.2, "1:*", 1.0, "*:1", 1.0, "*:*", 0.1);

	@Override
	public void contribute(final ApplicationMap map, final App app) throws FrameworkException {

		final SchemaIndex schema = map.getSchema();

		for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_NODE).getAsList()) {

			final SchemaNode schemaNode = node.as(SchemaNode.class);

			// a service class holds logic, not data
			map.addNode(schemaNode.getUuid(), ApplicationMap.TYPE, schemaNode.getName()).setService(schemaNode.isServiceClass()).setSize(schema.getPropertyCount(schemaNode.getName()));
		}

		for (final SchemaIndex.Method method : schema.getMethods()) {

			map.addNode(method.id, ApplicationMap.METHOD, method.getLabel()).setCronExpression(method.cronExpression).setSize((int) method.source.lines().filter(line -> !line.isBlank()).count());

			if (method.ownerTypeId != null) {

				map.addEdge(method.id, method.ownerTypeId, "member", method.isLifecycle ? 2.5 : 1.5, "method of " + method.owner);
				map.connect(method.id, method.ownerTypeId);
			}

			ScriptScanner.scan(map, method.source, method.id);
		}

		for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getAsList()) {

			final SchemaRelationshipNode rel = node.as(SchemaRelationshipNode.class);
			final SchemaNode source          = rel.getSourceNode();
			final SchemaNode target          = rel.getTargetNode();

			if (source != null && target != null) {

				final String multiplicity = rel.getSourceMultiplicity() + ":" + rel.getTargetMultiplicity();

				map.addEdge(source.getUuid(), target.getUuid(), "relates", MULTIPLICITY.getOrDefault(multiplicity, 0.3), source.getName() + " " + multiplicity + " " + target.getName() + " (" + rel.getRelationshipType() + ")");
				map.connect(source.getUuid(), target.getUuid());
			}
		}

		for (final Map.Entry<String, String> type : schema.getTypeIds().entrySet()) {

			for (final String parent : schema.getParents(type.getKey())) {

				if (schema.isType(parent)) {
					map.addEdge(type.getValue(), schema.getTypeIds().get(parent), "inherits", 1.5, type.getKey() + " inherits from " + parent);
				}
			}
		}

		final Traits traits = Traits.of(StructrTraits.SCHEMA_PROPERTY);

		for (final NodeInterface property : app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).getAsList()) {

			final NodeInterface owner = property.getProperty(traits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY));
			final String type         = property.getProperty(traits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY));

			if (owner != null && "Function".equals(type)) {

				final String read  = property.getProperty(traits.key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY));
				final String write = property.getProperty(traits.key(SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY));

				ScriptScanner.scan(map, (read != null ? read : "") + "\n" + (write != null ? write : ""), owner.getUuid());
			}
		}
	}

	// a property or a method lies in its type
	@Override
	public List<String> locate(final NodeInterface node) {

		final AbstractSchemaNode owner = node.is(StructrTraits.SCHEMA_PROPERTY) ? node.as(SchemaProperty.class).getSchemaNode() : node.is(StructrTraits.SCHEMA_METHOD) ? node.as(SchemaMethod.class).getSchemaNode() : null;

		return owner != null ? List.of(owner.getUuid()) : List.of();
	}
}
