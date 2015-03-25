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
import org.structr.schema.SchemaHelper;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonSchema.Cascade;
import org.structr.schema.json.JsonSchema.Direction;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrObjectProperty extends StructrPropertyDefinition implements JsonReferenceProperty {

	public StructrObjectProperty(final StructrTypeDefinition parent, final String name, final String relationship) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("object");
		setName(name);
		setRelationship(relationship);
	}

	@Override
	public String getReference() {
		return getString(this, JsonSchema.KEY_REFERENCE);
	}

	@Override
	public JsonReferenceProperty setReference(String ref) {

		put(JsonSchema.KEY_REFERENCE, ref);
		return this;
	}

	@Override
	public Direction getDirection() {

		final String value = getString(this, JsonSchema.KEY_DIRECTION);
		if (value != null) {

			return Direction.valueOf(value);
		}

		return null;
	}

	@Override
	public JsonReferenceProperty setDirection(Direction direction) {

		put(JsonSchema.KEY_DIRECTION, direction.name());
		return this;
	}

	@Override
	public Cascade getCascadingDelete() {

		final Map<String, Object> cascade = getMap(this, JsonSchema.KEY_CASCADE, false);
		if (cascade != null) {

			final Object value = getString(cascade, JsonSchema.KEY_DELETE);
			if (value != null) {

				return Cascade.valueOf(value.toString());
			}
		}

		return null;
	}

	@Override
	public JsonReferenceProperty setCascadingDelete(final Cascade cascade) {

		final Map<String, Object> cascadeMap = getMap(this, JsonSchema.KEY_CASCADE, true);
		if (cascadeMap != null) {

			cascadeMap.put(JsonSchema.KEY_DELETE, cascade.name());
		}

		// find reverse relation and add flag there as well
		root.notifyReferenceChange(getParent(), this);

		return this;
	}

	@Override
	public Cascade getCascadingCreate() {

		final Map<String, Object> cascade = getMap(this, JsonSchema.KEY_CASCADE, false);
		if (cascade != null) {

			final Object value = getString(cascade, JsonSchema.KEY_CREATE);
			if (value != null) {

				return Cascade.valueOf(value.toString());
			}
		}

		return null;
	}

	@Override
	public JsonReferenceProperty setCascadingCreate(final Cascade cascade) {

		final Map<String, Object> cascadeMap = getMap(this, JsonSchema.KEY_CASCADE, true);
		if (cascadeMap != null) {

			cascadeMap.put(JsonSchema.KEY_CREATE, cascade.name());
		}

		// find reverse relation and add flag there as well
		root.notifyReferenceChange(getParent(), this);

		return this;
	}

	@Override
	public String getRelationship() {
		return getString(this, JsonSchema.KEY_RELATIONSHIP);
	}

	@Override
	public JsonReferenceProperty setRelationship(String relationship) {

		put(JsonSchema.KEY_RELATIONSHIP, relationship);
		return this;
	}

	@Override
	public JsonReferenceProperty setProperties(final String... properties) {

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
				final Direction direction    = getDirection();

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


					final SchemaNode otherNode = def.getSchemaNode();
					String relationship        = getRelationship();

					switch (direction) {

						case out:

							if (StringUtils.isEmpty(relationship)) {
								relationship = schemaNode.getName()+ "_" + otherNode.getName();
							}

							final SchemaRelationshipNode outRel = StructrDefinition.getRelationship(app, schemaNode, otherNode, relationship);
							outRel.resolveCascadingEnums(getCascadingDelete(), getCascadingCreate());
							outRel.setProperty(SchemaRelationshipNode.targetJsonName, getName());
							outRel.setProperty(SchemaRelationshipNode.targetMultiplicity, "1");
							break;

						case in:

							if (StringUtils.isEmpty(relationship)) {
								relationship = otherNode.getName()+ "_" + schemaNode.getName();
							}

							final SchemaRelationshipNode inRel = StructrDefinition.getRelationship(app, otherNode, schemaNode, relationship);
							inRel.resolveCascadingEnums(getCascadingDelete(), getCascadingCreate());
							inRel.setProperty(SchemaRelationshipNode.sourceJsonName, getName());
							inRel.setProperty(SchemaRelationshipNode.sourceMultiplicity, "1");
							break;

						default:
							throw new IllegalStateException("Invalid direction " + direction + " for property " + getName() + " in type " + getParent().getName());

					}
				}
			}
		}

		return null;
	}

	@Override
	void initializeFromProperty(final JsonProperty property) {

		if (property instanceof JsonReferenceProperty) {

			final JsonReferenceProperty obj = (JsonReferenceProperty)property;

			setRelationship(obj.getRelationship());
			setReference(obj.getReference());

			final Set<String> properties = obj.getProperties();
			if (properties != null && !properties.isEmpty()) {

				setProperties(properties.toArray(new String[0]));
			}

			final Direction direction = obj.getDirection();
			if (direction != null) {

				setDirection(direction);
			}

			final Cascade delete = obj.getCascadingDelete();
			if (delete != null) {

				setCascadingDelete(delete);
			}

			final Cascade create = obj.getCascadingCreate();
			if (create != null) {

				setCascadingCreate(create);
			}

		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
