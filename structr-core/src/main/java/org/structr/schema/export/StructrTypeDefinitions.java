package org.structr.schema.export;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrTypeDefinitions implements StructrDefinition {

	private final Set<StructrRelationshipTypeDefinition> relationships = new TreeSet<>();
	private final Set<StructrTypeDefinition> typeDefinitions           = new TreeSet<>();
	private StructrSchemaDefinition root                               = null;

	StructrTypeDefinitions(final StructrSchemaDefinition root) {
		this.root = root;
	}

	public JsonType getType(final String name) {

		for (final JsonType type : typeDefinitions) {

			if (name.equals(type.getName())) {
				return type;
			}
		}

		return null;
	}

	public JsonObjectType addType(final String name) {

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, name);

		typeDefinitions.add(def);

		return def;

	}

	public void createDatabaseSchema(final App app) throws FrameworkException {

		for (final StructrTypeDefinition type : typeDefinitions) {

			final AbstractSchemaNode schemaNode = type.createDatabaseSchema(app);
			if (schemaNode != null) {

				type.setSchemaNode(schemaNode);
			}
		}

		for (final StructrRelationshipTypeDefinition rel : relationships) {
			rel.resolveEndpointTypesForDatabaseSchemaCreation(app);
		}
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {

		for (final StructrTypeDefinition type : typeDefinitions) {

			if (key.equals(type.getName())) {
				return type;
			}
		}

		return null;
	}

	// ----- package methods -----
	Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		for (final StructrTypeDefinition type : typeDefinitions) {

			map.put(type.getName(), type.serialize());
		}

		return map;
	}

	void deserialize(final Map<String, Object> source) {

		for (final Entry<String, Object> entry : source.entrySet()) {

			final String name  = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof Map) {

				final Map<String, Object> map    = (Map)value;
				final StructrTypeDefinition type = StructrTypeDefinition.deserialize(root, name, map);

				if (type != null) {

					typeDefinitions.add(type);

					if (type instanceof StructrRelationshipTypeDefinition) {
						relationships.add((StructrRelationshipTypeDefinition)type);
					}

				}

			} else {

				throw new IllegalStateException("Invalid JSON object for " + name + ", expected object, got " + value.getClass().getSimpleName());
			}
		}

		// initialize reference properties after all types are done
		for (final StructrTypeDefinition type : typeDefinitions) {
			type.initializeReferenceProperties();
		}
	}

	void deserialize(final App app) throws FrameworkException {

		for (final SchemaNode schemaNode : app.nodeQuery(SchemaNode.class).getAsList()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(root, schemaNode);
			if (type != null) {

				typeDefinitions.add(type);
			}
		}

		for (final SchemaRelationshipNode schemaRelationship : app.nodeQuery(SchemaRelationshipNode.class).getAsList()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(root, schemaRelationship);
			if (type != null) {

				typeDefinitions.add(type);

				if (type instanceof StructrRelationshipTypeDefinition) {
					relationships.add((StructrRelationshipTypeDefinition)type);
				}
			}
		}

	}

	void addType(final StructrTypeDefinition type) {
		typeDefinitions.add(type);
	}

	Set<StructrRelationshipTypeDefinition> getRelationships() {
		return relationships;
	}
}
