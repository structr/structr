package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.structr.schema.SchemaHelper;
import org.structr.schema.json.JsonObjectProperty;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

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
	public JsonObjectProperty setProperties(final String... properties) {

		final List<String> propertyList = getList(this, JsonSchema.KEY_PROPERTIES, true);
		propertyList.addAll(Arrays.asList(properties));

		Collections.sort(propertyList);

		return this;
	}

	@Override
	public Set<String> getProperties() {

		final List<String> properties = getList(this, JsonSchema.KEY_PROPERTIES, false);
		if (properties != null) {

			return new TreeSet<>(properties);
		}

		return Collections.EMPTY_SET;
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final SchemaNode schemaNode) throws FrameworkException {

		final String reference = getReference();
		if (reference != null) {

			final StructrDefinition def = resolveJsonPointer(reference);
			if (def != null) {

				final Set<String> properties = getProperties();
				final String direction       = getDirection();

				if (direction == null || (properties != null && !properties.isEmpty())) {

					if (def instanceof JsonProperty) {

						final JsonProperty notionProperty = (JsonProperty)def;

						final SchemaProperty schemaProperty = app.create(SchemaProperty.class,
							new NodeAttribute(AbstractNode.name, getName()),
							new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
							new NodeAttribute(SchemaProperty.propertyType, SchemaHelper.Type.Notion.name()),
							new NodeAttribute(SchemaProperty.format, notionProperty.getName() + ", " + StringUtils.join(properties, ", "))
						);

						return schemaProperty;

					} else if (def instanceof JsonType) {

						System.out.println("########################################################################################");
						System.out.println("createDatabaseSchema for object property " + this);
						System.out.println("encountered type: " + def.getClass().getName());
					}

				} else {

					String relationship = getRelationship();

					final SchemaNode otherNode = def.getSchemaNode();

					if ("out".equals(direction)) {

						if (StringUtils.isEmpty(relationship)) {
							relationship = schemaNode.getName()+ "_" + otherNode.getName();
						}

						final SchemaRelationshipNode rel = StructrDefinition.getRelationship(app, schemaNode, otherNode, relationship);
						rel.setProperty(SchemaRelationshipNode.targetJsonName, getName());
						rel.setProperty(SchemaRelationshipNode.targetMultiplicity, "1");


					} else if ("in".equals(direction)) {

						if (StringUtils.isEmpty(relationship)) {
							relationship = otherNode.getName()+ "_" + schemaNode.getName();
						}

						final SchemaRelationshipNode rel = StructrDefinition.getRelationship(app, otherNode, schemaNode, relationship);
						rel.setProperty(SchemaRelationshipNode.sourceJsonName, getName());
						rel.setProperty(SchemaRelationshipNode.sourceMultiplicity, "1");

					} else {

						throw new IllegalStateException("Invalid direction " + direction + " for property " + getName() + " in type " + getParent().getName());
					}
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

			final Set<String> properties = obj.getProperties();
			if (properties != null && !properties.isEmpty()) {

				setProperties(properties.toArray(new String[0]));
			}

		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
