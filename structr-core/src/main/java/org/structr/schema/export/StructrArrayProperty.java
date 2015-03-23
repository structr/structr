package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.parboiled.common.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonArrayProperty;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrArrayProperty extends StructrPropertyDefinition implements JsonArrayProperty {

	public StructrArrayProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("array");
		setName(name);
	}

	@Override
	public String getDirection() {
		return getString(this, JsonSchema.KEY_DIRECTION);
	}

	@Override
	public JsonArrayProperty setDirection(String direction) {

		put(JsonSchema.KEY_DIRECTION, direction);
		return this;
	}

	@Override
	public String getRelationship() {
		return getString(this, JsonSchema.KEY_RELATIONSHIP);
	}

	@Override
	public JsonArrayProperty setRelationship(String relationship) {

		put(JsonSchema.KEY_RELATIONSHIP, relationship);
		return this;
	}

	@Override
	public JsonArrayProperty setProperties(final String... properties) {

		final Map<String, Object> items = getMap(this, JsonSchema.KEY_ITEMS, true);
		if (items != null) {

			final List<String> propertyList = getList(items, JsonSchema.KEY_PROPERTIES, true);
			propertyList.addAll(Arrays.asList(properties));

			Collections.sort(propertyList);
		}

		return this;
	}

	@Override
	public Set<String> getProperties() {

		final Map<String, Object> items = getMap(this, JsonSchema.KEY_ITEMS, false);
		if (items != null) {

			final List<String> properties = getList(items, JsonSchema.KEY_PROPERTIES, false);
			if (properties != null) {

				return new TreeSet<>(properties);
			}
		}

		return Collections.EMPTY_SET;
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
	public JsonArrayProperty setReference(String ref) {

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

			final StructrDefinition def = resolveJsonPointer(reference);
			if (def != null) {

				final Set<String> properties = getProperties();
				final String direction = getDirection();

				if (direction == null || (properties != null && !properties.isEmpty())) {

					if (def instanceof JsonProperty) {

						final JsonProperty notionProperty = (JsonProperty)def;

						final SchemaProperty schemaProperty = app.create(SchemaProperty.class,
							new NodeAttribute(AbstractNode.name, getName()),
							new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
							new NodeAttribute(SchemaProperty.propertyType, Type.Notion.name()),
							new NodeAttribute(SchemaProperty.format, notionProperty.getName() + ", " + StringUtils.join(properties, ", "))
						);

						return schemaProperty;

					} else if (def instanceof JsonType) {

						System.out.println("########################################################################################");
						System.out.println("createDatabaseSchema for array property " + this);
						System.out.println("encountered type: " + def.getClass().getName());
					}

				} else {

					String relationship = getRelationship();

					final SchemaNode otherNode = def.getSchemaNode();

					if ("out".equals(direction)) {

						if (StringUtils.isEmpty(relationship)) {
							relationship = SchemaRelationshipNode.getDefaultRelationshipType(schemaNode, otherNode);
						}

						final SchemaRelationshipNode rel = StructrDefinition.getRelationship(app, schemaNode, otherNode, relationship);
						rel.setProperty(SchemaRelationshipNode.targetJsonName, getName());
						rel.setProperty(SchemaRelationshipNode.targetMultiplicity, "*");

					} else if ("in".equals(direction)) {

						if (StringUtils.isEmpty(relationship)) {
							relationship = SchemaRelationshipNode.getDefaultRelationshipType(otherNode, schemaNode);
						}

						final SchemaRelationshipNode rel = StructrDefinition.getRelationship(app, otherNode, schemaNode, relationship);
						rel.setProperty(SchemaRelationshipNode.sourceJsonName, getName());
						rel.setProperty(SchemaRelationshipNode.sourceMultiplicity, "*");

					} else {

						throw new IllegalStateException("Invalid direction " + direction + " for property " + getName() + " in type " + getParent().getName());
					}
				}
			}
		}

		return null;
	}

	@Override
	void initializeFromProperty(JsonProperty property) {

		if (property instanceof JsonArrayProperty) {

			final JsonArrayProperty obj = (JsonArrayProperty)property;

			setDirection(obj.getDirection());
			setRelationship(obj.getRelationship());
			setReference(obj.getReference());

			final Set<String> properties = obj.getProperties();
			if (properties != null && !properties.isEmpty()) {

				setProperties(properties.toArray(new String[0]));
			}

		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
