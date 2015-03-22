package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Map;
import org.parboiled.common.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.schema.json.JsonArrayProperty;
import org.structr.schema.json.JsonObjectProperty;
import org.structr.schema.json.JsonSchema;

/**
 *
 * @author Christian Morgner
 */
public class StructrArrayProperty extends StructrObjectProperty implements JsonArrayProperty {

	public StructrArrayProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("array");
		setName(name);
	}

	@Override
	public String getReference() {

		final Map<String, Object> items = getMap(this, JsonSchema.KEY_ITEMS, false);
		if (items != null) {

			return getString(items, JsonSchema.KEY_REFERENCE);
		}

		return null;
	}

	@Override
	public JsonObjectProperty setReference(String ref) {

		final Map<String, Object> items = getMap(this, JsonSchema.KEY_ITEMS, true);
		if (items != null) {

			items.put(JsonSchema.KEY_REFERENCE, ref);
		}

		return this;
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final SchemaNode schemaNode) throws FrameworkException {

		final String reference = getReference();
		if (reference != null) {

			final String direction = getDirection();
			String relationship = getRelationship();

			final StructrDefinition def = resolveJsonPointer(reference);
			if (def != null) {

				final SchemaNode otherNode = def.getSchemaNode();

				if ("out".equals(direction)) {

					if (StringUtils.isEmpty(relationship)) {
						relationship = SchemaRelationshipNode.getDefaultRelationshipType(schemaNode, otherNode);
					}

					final SchemaRelationshipNode rel = getRelationship(app, schemaNode, otherNode, relationship);
					rel.setProperty(SchemaRelationshipNode.targetJsonName, getName());
					rel.setProperty(SchemaRelationshipNode.targetMultiplicity, "*");

				} else if ("in".equals(direction)) {

					if (StringUtils.isEmpty(relationship)) {
						relationship = SchemaRelationshipNode.getDefaultRelationshipType(otherNode, schemaNode);
					}

					final SchemaRelationshipNode rel = getRelationship(app, otherNode, schemaNode, relationship);
					rel.setProperty(SchemaRelationshipNode.sourceJsonName, getName());
					rel.setProperty(SchemaRelationshipNode.sourceMultiplicity, "*");

				} else {

					throw new IllegalStateException("Invalid direction " + direction + " in schema.");
				}
			}
		}

		return null;
	}
}
