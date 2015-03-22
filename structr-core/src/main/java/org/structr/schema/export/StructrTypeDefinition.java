package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonArrayProperty;
import org.structr.schema.json.JsonBooleanProperty;
import org.structr.schema.json.JsonEnumProperty;
import org.structr.schema.json.JsonNumberProperty;
import org.structr.schema.json.JsonObjectProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonScriptProperty;
import org.structr.schema.json.JsonStringProperty;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrTypeDefinition extends StructrDefinition implements JsonType {

	private String name = null;

	StructrTypeDefinition(final StructrSchemaDefinition root, final String id) throws URISyntaxException {

		super(root, id);
		put(JsonSchema.KEY_TYPE, "object");
	}

	StructrTypeDefinition(final StructrSchemaDefinition root, final String id, final JsonType source) throws URISyntaxException {

		super(root, id);

		put(JsonSchema.KEY_TYPE, "object");
		initializeFrom(source);
	}

	@Override
	public StructrDefinition getParent() {
		return root;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JsonType setName(final String name) {

		this.name = name;
		return this;
	}

	@Override
	public Set<JsonProperty> getProperties() {

		final Map<String, StructrPropertyDefinition> properties = getPropertyDefinitions();
		final Set<JsonProperty> propertySet               = new TreeSet<>();

		for (final StructrPropertyDefinition prop : properties.values()) {
			propertySet.add(prop);
		}

		return propertySet;
	}

	@Override
	public Map<String, Set<String>> getViews() {
		return getViewDefinitions();
	}

	@Override
	public Set<String> getRequiredProperties() {
		return getSet(this, JsonSchema.KEY_REQUIRED, true);
	}

	@Override
	public JsonStringProperty addStringProperty(String name, String... views) throws URISyntaxException {

		final StructrStringProperty stringProperty = new StructrStringProperty(this, name);
		addProperty(stringProperty, name, views);

		return stringProperty;
	}

	@Override
	public JsonNumberProperty addNumberProperty(String name, String... views) throws URISyntaxException {

		final StructrNumberProperty numberProperty = new StructrNumberProperty(this, name);
		addProperty(numberProperty, name, views);

		return numberProperty;
	}

	@Override
	public JsonBooleanProperty addBooleanProperty(String name, String... views) throws URISyntaxException {

		final StructrBooleanProperty booleanProperty = new StructrBooleanProperty(this, name);
		addProperty(booleanProperty, name, views);

		return booleanProperty;
	}

	@Override
	public JsonObjectProperty addReference(final String name, final JsonType otherType, final String... views) throws URISyntaxException {

		final StructrObjectProperty objectProperty = new StructrObjectProperty(this, name);
		addProperty(objectProperty, name, views);

		objectProperty.put(JsonSchema.KEY_REFERENCE, "#/definitions/" + otherType.getName());

		return objectProperty;
	}

	@Override
	public JsonArrayProperty addArrayReference(final String name, final JsonType otherType, final String... views) throws URISyntaxException {

		final StructrArrayProperty arrayProperty = new StructrArrayProperty(this, name);
		addProperty(arrayProperty, name, views);

		final Map<String, Object> items = new TreeMap<>();
		arrayProperty.put(JsonSchema.KEY_ITEMS, items);

		items.put(JsonSchema.KEY_REFERENCE, "#/definitions/" + otherType.getName());

		return arrayProperty;
	}

	@Override
	public JsonScriptProperty addScriptProperty(String name, String... views) throws URISyntaxException {

		final StructrScriptProperty scriptProperty = new StructrScriptProperty(this, name);
		addProperty(scriptProperty, name, views);

		return scriptProperty;
	}

	@Override
	public JsonEnumProperty addEnumProperty(String name, String... views) throws URISyntaxException {

		final StructrEnumProperty enumProperty = new StructrEnumProperty(this, name);
		addProperty(enumProperty, name, views);

		return enumProperty;
	}

	// ----- interface Comparable<JsonSchemaType> -----
	@Override
	public int compareTo(JsonType other) {
		return getName().compareTo(other.getName());
	}

	// ----- package methods -----
	void addProperty(final StructrPropertyDefinition newProperty, final String name, final String... views) throws URISyntaxException {

		if (views != null) {

			final Map<String, Set<String>> viewDefinitions = getViewDefinitions();
			for (final String viewName : views) {

				Set<String> propertySet = viewDefinitions.get(viewName);
				if (propertySet == null) {

					propertySet = new TreeSet<>();
					viewDefinitions.put(viewName, propertySet);
				}

				propertySet.add(name);
			}
		}

		// store new property in this type
		getPropertyDefinitions().put(name, newProperty);
	}

	void createFromDatabase(final SchemaNode schemaNode) throws URISyntaxException {

		this.name = schemaNode.getProperty(AbstractNode.name);

		final Set<String> requiredProperties = new TreeSet<>();
		readLocalProperties(requiredProperties, schemaNode);
		readRemoteProperties(requiredProperties, schemaNode);
		readViews(schemaNode);

		// "required"
		if (!requiredProperties.isEmpty()) {
			put(JsonSchema.KEY_REQUIRED, requiredProperties);
		}
	}

	void createDatabaseSchemaProperties(final App app, final SchemaNode schemaNode) throws FrameworkException {

		final Map<String, StructrPropertyDefinition> propertyDefinitions = getPropertyDefinitions();
		final Map<String, Set<String>> views                             = getViewDefinitions();
		final Map<String, SchemaProperty> properties                     = new TreeMap<>();

		// create properties and store them in a map for later use
		for (final Entry<String, StructrPropertyDefinition> entry : propertyDefinitions.entrySet()) {

			final SchemaProperty property = entry.getValue().createDatabaseSchema(app, schemaNode);
			if (property != null) {

				properties.put(entry.getKey(), property);
			}
		}

		// create views and associate the properties
		for (final Entry<String, Set<String>> view : views.entrySet()) {

			final List<SchemaProperty> viewProperties = new LinkedList<>();
			final List<String> nonGraphProperties     = new LinkedList<>();

			for (final String propertyName : view.getValue()) {

				final SchemaProperty property = properties.get(propertyName);
				if (property != null) {

					viewProperties.add(property);

				} else {

					nonGraphProperties.add(propertyName);
				}
			}

			// create view node with parent and children
			app.create(SchemaView.class,
				new NodeAttribute(SchemaView.schemaNode, schemaNode),
				new NodeAttribute(AbstractNode.name, view.getKey()),
				new NodeAttribute(SchemaView.schemaProperties, viewProperties),
				new NodeAttribute(SchemaView.nonGraphProperties, StringUtils.join(nonGraphProperties, ", "))
			);
		}
	}

	void createFromSource(final Map<String, Object> source) throws InvalidSchemaException, URISyntaxException {

		final Map<String, StructrPropertyDefinition> propertyDefinitions = getPropertyDefinitions();
		final Map<String, Set<String>> viewDefinitions                   = getViewDefinitions();
		final String type                                                = getString(source, JsonSchema.KEY_TYPE);

		if ("object".equals(type)) {

			final Map<String, Object> properties = getMap(source, JsonSchema.KEY_PROPERTIES, false);
			if (properties != null) {

				for (final Entry<String, Object> entry : properties.entrySet()) {

					final String key   = entry.getKey();
					final Object value = entry.getValue();

					if (value instanceof Map) {

						final Map<String, Object> map       = (Map<String, Object>)value;
						final String propertyType           = getString(map, JsonSchema.KEY_TYPE);
						final boolean isEnum                = getList(map, JsonSchema.KEY_ENUM, false) != null;
						final StructrPropertyDefinition def = StructrPropertyDefinition.forStringType(this, propertyType, key, isEnum);

						def.createFromSource(map);

						propertyDefinitions.put(key, def);

					} else {

						throw new InvalidSchemaException("Property definition " + key + " has wrong type, expecting object.");
					}
				}
			}

			final Map<String, Object> views = getMap(source, JsonSchema.KEY_VIEWS, false);
			if (views != null) {

				for (final Entry<String, Object> entry : views.entrySet()) {

					final String key   = entry.getKey();
					final Object value = entry.getValue();

					Set<String> view = viewDefinitions.get(key);
					if (view == null) {

						view = new TreeSet<>();
						viewDefinitions.put(key, view);
					}

					if (value instanceof Collection) {

						for (final Object o : ((Collection)value)) {
							view.add((String)o);
						}

					} else {

						throw new InvalidSchemaException("Invalid view " + key + ", expected array.");
					}
				}
			}

			final List<String> requiredProperties = getList(source, JsonSchema.KEY_REQUIRED, false);
			if (requiredProperties != null) {

				for (final String propertyName : requiredProperties) {

					final StructrPropertyDefinition def = propertyDefinitions.get(propertyName);
					if (def != null) {

						def.setRequired(true);
					}
				}
			}

		} else {

			throw new InvalidSchemaException("Encountered invalid type " + type + ", expected object.");
		}

		if (propertyDefinitions.isEmpty()) {
			remove(JsonSchema.KEY_PROPERTIES);
		}

		if (viewDefinitions.isEmpty()) {
			remove(JsonSchema.KEY_VIEWS);
		}
	}

	// ----- private methods -----
	private Map<String, StructrPropertyDefinition> getPropertyDefinitions() {
		return (Map)getMap(this, StructrSchemaDefinition.KEY_PROPERTIES, true);
	}

	private Map<String, Set<String>> getViewDefinitions() {
		return (Map)getMap(this, StructrSchemaDefinition.KEY_VIEWS, true);
	}

	private void readLocalProperties(final Set<String> requiredProperties, final SchemaNode schemaNode) throws URISyntaxException {

		final Map<String, StructrPropertyDefinition> propertyDefinitions = getPropertyDefinitions();

		for (final SchemaProperty property : schemaNode.getProperty(AbstractSchemaNode.schemaProperties)) {

			final StructrPropertyDefinition def = StructrPropertyDefinition.forStructrType(this, property);

			if (def.isRequired()) {
				requiredProperties.add(def.getName());
			}

			propertyDefinitions.put(def.getName(), def);
		}

		if (propertyDefinitions.isEmpty()) {
			remove(JsonSchema.KEY_PROPERTIES);
		}
	}

	private void readRemoteProperties(final Set<String> requiredProperties, final SchemaNode schemaNode) throws URISyntaxException {

		final Map<String, StructrPropertyDefinition> propertyDefinitions = getPropertyDefinitions();
		final Set<String> existingPropertyNames                          = new HashSet<>();

		for (final SchemaRelationshipNode relationship : schemaNode.getProperty(SchemaNode.relatedTo)) {

			final StructrPropertyDefinition def = StructrPropertyDefinition.forStructrType(this, relationship, existingPropertyNames, true);

			if (def.isRequired()) {
				requiredProperties.add(def.getName());
			}

			propertyDefinitions.put(def.getName(), def);
		}

		for (final SchemaRelationshipNode relationship : schemaNode.getProperty(SchemaNode.relatedFrom)) {

			final StructrPropertyDefinition def = StructrPropertyDefinition.forStructrType(this, relationship, existingPropertyNames, false);

			if (def.isRequired()) {
				requiredProperties.add(def.getName());
			}

			propertyDefinitions.put(def.getName(), def);
		}

		// remove empty properties objects
		if (propertyDefinitions.isEmpty()) {
			remove(JsonSchema.KEY_PROPERTIES);
		}
	}

	private void readViews(final SchemaNode schemaNode) {

		final Map<String, Set<String>> viewDefinitions = getViewDefinitions();

		for (final SchemaView view : schemaNode.getProperty(AbstractSchemaNode.schemaViews)) {

			final Set<String> propertySet = new TreeSet<>();

			for (final SchemaProperty property : view.getProperty(SchemaView.schemaProperties)) {
				propertySet.add(property.getName());
			}

			final String nonGraphProperties = view.getProperty(SchemaView.nonGraphProperties);
			if (nonGraphProperties != null) {

				for (final String property : nonGraphProperties.split("[, ]+")) {

					final String trimmed = property.trim();
					if (StringUtils.isNotBlank(trimmed)) {

						propertySet.add(trimmed);
					}
				}
			}

			if (!propertySet.isEmpty()) {

				viewDefinitions.put(view.getName(), propertySet);
			}
		}

		// remove empty view object
		if (viewDefinitions.isEmpty()) {
			remove(JsonSchema.KEY_VIEWS);
		}
	}

	// ----- private methods -----
	private void initializeFrom(final JsonType source) throws URISyntaxException {

		setName(source.getName());

		final Map<String, StructrPropertyDefinition> propertyDefinitions = getPropertyDefinitions();
		for (final JsonProperty property : source.getProperties()) {

			final boolean isEnum = property instanceof JsonEnumProperty;
			propertyDefinitions.put(property.getName(), StructrPropertyDefinition.forJsonType(this, property, isEnum));
		}

		// copy views from source
		getViewDefinitions().putAll(source.getViews());
	}
}
