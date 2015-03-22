package org.structr.schema.export;

import java.net.URISyntaxException;
import org.parboiled.common.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.json.JsonObjectProperty;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonSchema;

/**
 *
 * @author Christian Morgner
 */
public class StructrObjectProperty extends StructrPropertyDefinition implements JsonObjectProperty {

	public StructrObjectProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("object");
		setName(name);
	}

	@Override
	public String getReference() {
		return getString(this, JsonSchema.KEY_REFERENCE);
	}

	@Override
	public JsonObjectProperty setReference(String ref) {

		put(JsonSchema.KEY_REFERENCE, ref);
		return this;
	}

	@Override
	public String getDirection() {
		return getString(this, JsonSchema.KEY_DIRECTION);
	}

	@Override
	public JsonObjectProperty setDirection(String direction) {

		put(JsonSchema.KEY_DIRECTION, direction);
		return this;
	}

	@Override
	public String getRelationship() {
		return getString(this, JsonSchema.KEY_RELATIONSHIP);
	}

	@Override
	public JsonObjectProperty setRelationship(String relationship) {

		put(JsonSchema.KEY_RELATIONSHIP, relationship);
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
						relationship = schemaNode.getName()+ "_" + otherNode.getName();
					}

					final SchemaRelationshipNode rel = getRelationship(app, schemaNode, otherNode, relationship);
					rel.setProperty(SchemaRelationshipNode.targetJsonName, getName());
					rel.setProperty(SchemaRelationshipNode.targetMultiplicity, "1");


				} else if ("in".equals(direction)) {

					if (StringUtils.isEmpty(relationship)) {
						relationship = otherNode.getName()+ "_" + schemaNode.getName();
					}

					final SchemaRelationshipNode rel = getRelationship(app, otherNode, schemaNode, relationship);
					rel.setProperty(SchemaRelationshipNode.sourceJsonName, getName());
					rel.setProperty(SchemaRelationshipNode.sourceMultiplicity, "1");

				} else {

					throw new IllegalStateException("Invalid direction " + direction + " in schema.");
				}
			}
		}

		return null;
	}

	@Override
	void initializeFromProperty(final JsonProperty property) {

		if (property instanceof JsonObjectProperty) {

			final JsonObjectProperty obj = (JsonObjectProperty)property;

			setDirection(obj.getDirection());
			setRelationship(obj.getRelationship());
			setReference(obj.getReference());

		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}

	// ----- protected methods -----
	protected SchemaRelationshipNode getRelationship(final App app, final SchemaNode sourceNode, final SchemaNode targetNode, final String relationshipType) throws FrameworkException {

		SchemaRelationshipNode node = app.nodeQuery(SchemaRelationshipNode.class)
			.and(SchemaRelationshipNode.sourceId, sourceNode.getUuid())
			.and(SchemaRelationshipNode.targetId, targetNode.getUuid())
			.and(SchemaRelationshipNode.relationshipType, relationshipType)
			.getFirst();

		if (node == null) {

			node = app.create(SchemaRelationshipNode.class,
				new NodeAttribute(SchemaRelationshipNode.sourceNode, sourceNode),
				new NodeAttribute(SchemaRelationshipNode.targetNode, targetNode),
				new NodeAttribute(SchemaRelationshipNode.relationshipType, relationshipType)
			);
		}

		return node;
	}
}
