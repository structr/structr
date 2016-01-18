/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.schema.export;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.json.JsonBooleanProperty;
import org.structr.schema.json.JsonDateProperty;
import org.structr.schema.json.JsonEnumProperty;
import org.structr.schema.json.JsonFunctionProperty;
import org.structr.schema.json.JsonIntegerProperty;
import org.structr.schema.json.JsonLongProperty;
import org.structr.schema.json.JsonNumberProperty;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonScriptProperty;
import org.structr.schema.json.JsonStringArrayProperty;
import org.structr.schema.json.JsonStringProperty;
import org.structr.schema.json.JsonType;

/**
 *
 *
 *
 * @param <T>
 */
public abstract class StructrTypeDefinition<T extends AbstractSchemaNode> implements JsonType, StructrDefinition {

	protected final Set<StructrPropertyDefinition> properties = new TreeSet<>();
	protected final Map<String, Set<String>> views            = new TreeMap<>();
	protected final Map<String, String> methods               = new TreeMap<>();
	protected StructrSchemaDefinition root                    = null;
	protected URI baseTypeReference                           = null;
	protected String name                                     = null;
	protected T schemaNode                                    = null;

	StructrTypeDefinition(final StructrSchemaDefinition root, final String name) {

		this.root = root;
		this.name = name;
	}

	abstract T createSchemaNode(final App app) throws FrameworkException;

	@Override
	public URI getId() {

		final URI id  = root.getId();
		final URI uri = URI.create("definitions/" + getName());

		return id.resolve(uri);
	}

	@Override
	public JsonSchema getSchema() {
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
	public JsonType addMethod(final String name, final String source) {

		methods.put(name, source);
		return this;
	}

	@Override
	public JsonType setExtends(final JsonType superType) {

		this.baseTypeReference = superType.getId();
		return this;

	}

	@Override
	public JsonType setExtends(final URI externalReference) {

		this.baseTypeReference = externalReference;
		return this;
	}

	@Override
	public URI getExtends() {
		return baseTypeReference;
	}

	@Override
	public Set<JsonProperty> getProperties() {
		return (Set)properties;
	}

	@Override
	public Map<String, String> getMethods() {
		return methods;
	}

	@Override
	public Set<String> getViewNames() {
		return views.keySet();
	}

	@Override
	public Set<String> getViewPropertyNames(final String viewName) {
		return views.get(viewName);
	}

	@Override
	public JsonType addViewProperty(final String viewName, final String propertyName) {

		addPropertyNameToViews(propertyName, viewName);
		return this;
	}

	@Override
	public Set<String> getRequiredProperties() {

		final Set<String> requiredProperties = new TreeSet<>();

		for (final StructrPropertyDefinition property : properties) {

			if (property.isRequired()) {

				requiredProperties.add(property.getName());
			}
		}

		return requiredProperties;
	}

	@Override
	public JsonStringProperty addStringProperty(final String name, final String... views) throws URISyntaxException {

		final StructrStringProperty stringProperty = new StructrStringProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(stringProperty);

		return stringProperty;
	}

	@Override
	public JsonStringArrayProperty addStringArrayProperty(final String name, final String... views) throws URISyntaxException {

		final StructrStringArrayProperty stringProperty = new StructrStringArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(stringProperty);

		return stringProperty;
	}

	@Override
	public JsonDateProperty addDateProperty(final String name, final String... views) throws URISyntaxException {

		final StructrDateProperty dateProperty = new StructrDateProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(dateProperty);

		return dateProperty;

	}

	@Override
	public JsonIntegerProperty addIntegerProperty(final String name, final String... views) throws URISyntaxException {

		final StructrIntegerProperty numberProperty = new StructrIntegerProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonLongProperty addLongProperty(final String name, final String... views) throws URISyntaxException {

		final StructrLongProperty numberProperty = new StructrLongProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonNumberProperty addNumberProperty(final String name, final String... views) throws URISyntaxException {

		final StructrNumberProperty numberProperty = new StructrNumberProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonBooleanProperty addBooleanProperty(final String name, final String... views) throws URISyntaxException {

		final StructrBooleanProperty booleanProperty = new StructrBooleanProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(booleanProperty);

		return booleanProperty;
	}

	@Override
	public JsonScriptProperty addScriptProperty(final String name, final String... views) throws URISyntaxException {

		final StructrScriptProperty scriptProperty = new StructrScriptProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(scriptProperty);

		return scriptProperty;
	}

	@Override
	public JsonFunctionProperty addFunctionProperty(final String name, final String... views) throws URISyntaxException {

		final StructrFunctionProperty functionProperty = new StructrFunctionProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(functionProperty);

		return functionProperty;
	}

	@Override
	public JsonEnumProperty addEnumProperty(final String name, final String... views) throws URISyntaxException {

		final StructrEnumProperty enumProperty = new StructrEnumProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(enumProperty);

		return enumProperty;
	}

	@Override
	public JsonReferenceProperty addReferenceProperty(final String name, final JsonReferenceProperty referencedProperty, final String... views) {

		final String reference = root.toJsonPointer(referencedProperty.getId());
		final String refType   = referencedProperty.getType();
		final String refName   = referencedProperty.getName();

		final StructrReferenceProperty ref = new NotionReferenceProperty(this, name, reference, refType, refName);

		addPropertyNameToViews(name, views);

		properties.add(ref);

		return ref;
	}

	@Override
	public int compareTo(final JsonType o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {

		switch (key) {

			case "properties":
				return new StructrDefinition() {

					@Override
					public StructrDefinition resolveJsonPointerKey(final String key) {

						for (final StructrPropertyDefinition property : properties) {

							if (key.equals(property.getName())) {

								return property;
							}
						}

						return null;
					}
				};

			case "views":
				return new StructrDefinition() {

					@Override
					public StructrDefinition resolveJsonPointerKey(final String key) {
						return null;
					}
				};
		}

		return null;
	}

	// ----- package methods -----
	Map<String, Object> serialize() {

		final Map<String, Object> serializedForm       = new TreeMap<>();
		final Map<String, Object> serializedProperties = new TreeMap<>();

		// populate properties
		for (final StructrPropertyDefinition property : properties) {
			serializedProperties.put(property.getName(), property.serialize());
		}

		serializedForm.put(JsonSchema.KEY_TYPE, "object");

		// properties
		if (!serializedProperties.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_PROPERTIES, serializedProperties);
		}

		// required
		final Set<String> requiredProperties = getRequiredProperties();
		if (!requiredProperties.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_REQUIRED, requiredProperties);
		}

		// views
		if (!views.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_VIEWS, views);
		}

		// methods
		if (!methods.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_METHODS, methods);
		}

		final URI ext = getExtends();
		if (ext != null) {
			serializedForm.put(JsonSchema.KEY_EXTENDS, root.toJsonPointer(ext));
		}

		return serializedForm;
	}

	void deserialize(final Map<String, Object> source) {

		if (source.containsKey(JsonSchema.KEY_EXTENDS)) {

			String jsonPointerFormat = (String)source.get(JsonSchema.KEY_EXTENDS);
			if (jsonPointerFormat.startsWith("#")) {

				jsonPointerFormat = jsonPointerFormat.substring(1);
			}

			this.baseTypeReference = root.getId().relativize(URI.create(jsonPointerFormat));
		}
	}

	void deserialize(final T schemaNode) {

		for (final SchemaProperty property : schemaNode.getProperty(AbstractSchemaNode.schemaProperties)) {

			final StructrPropertyDefinition propertyDefinition = StructrPropertyDefinition.deserialize(this, property);
			if (propertyDefinition != null) {

				properties.add(propertyDefinition);
			}
		}

		for (final SchemaView view : schemaNode.getProperty(AbstractSchemaNode.schemaViews)) {

			if (!View.INTERNAL_GRAPH_VIEW.equals(view.getName())) {

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
					views.put(view.getName(), propertySet);
				}
			}
		}

		for (final SchemaMethod method : schemaNode.getProperty(AbstractSchemaNode.schemaMethods)) {

			final String _name   = method.getName();
			final String _source = method.getProperty(SchemaMethod.source);

			methods.put(_name, _source);
		}

		// $extends
		final String extendsClass = schemaNode.getProperty(SchemaNode.extendsClass);
		if (extendsClass != null) {

			final String typeName = extendsClass.substring(extendsClass.lastIndexOf(".") + 1);
			if (extendsClass.startsWith("org.structr.dynamic.")) {

				this.baseTypeReference = root.getId().resolve("definitions/" + typeName);

			} else {

				this.baseTypeReference = StructrApp.getSchemaBaseURI().resolve("definitions/" + typeName);
			}
		}
	}

	AbstractSchemaNode createDatabaseSchema(final App app) throws FrameworkException {

		final Map<String, SchemaProperty> schemaProperties = new TreeMap<>();
		final T schemaNode                                 = createSchemaNode(app);

		for (final StructrPropertyDefinition property : properties) {

			final SchemaProperty schemaProperty = property.createDatabaseSchema(app, schemaNode);
			if (schemaProperty != null) {

				schemaProperties.put(schemaProperty.getName(), schemaProperty);
			}
		}

		// create views and associate the properties
		for (final Entry<String, Set<String>> view : views.entrySet()) {

			final List<SchemaProperty> viewProperties = new LinkedList<>();
			final List<String> nonGraphProperties = new LinkedList<>();

			for (final String propertyName : view.getValue()) {

				final SchemaProperty property = schemaProperties.get(propertyName);
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

		// create methods
		for (final Entry<String, String> method : methods.entrySet()) {

			// create view node with parent and children
			app.create(SchemaMethod.class,
				new NodeAttribute(SchemaMethod.schemaNode, schemaNode),
				new NodeAttribute(AbstractNode.name, method.getKey()),
				new NodeAttribute(SchemaMethod.source, method.getValue())
			);
		}

		// extends
		if (baseTypeReference != null) {

			final Object def = root.resolveURI(baseTypeReference);

			if (def != null && def instanceof JsonType) {

				final JsonType jsonType     = (JsonType)def;
				final String superclassName = "org.structr.dynamic." + jsonType.getName();

				schemaNode.setProperty(SchemaNode.extendsClass, superclassName);

			} else {

				final Class superclass = StructrApp.resolveSchemaId(baseTypeReference);
				if (superclass != null) {

					schemaNode.setProperty(SchemaNode.extendsClass, superclass.getName());
				}
			}
		}

		return schemaNode;
	}

	T getSchemaNode() {
		return schemaNode;
	}

	void setSchemaNode(final T schemaNode) {
		this.schemaNode = schemaNode;
	}

	Map<String, Set<String>> getViews() {
		return views;
	}

	void initializeReferenceProperties() {

		for (final StructrPropertyDefinition property : properties) {
			property.initializeReferences();
		}
	}

	// ----- static methods -----
	static StructrTypeDefinition deserialize(final StructrSchemaDefinition root, final String name, final Map<String, Object> source) {

		final Map<String, StructrPropertyDefinition> deserializedProperties = new TreeMap<>();
		final StructrTypeDefinition typeDefinition                          = StructrTypeDefinition.determineType(root, name, source);
		final Map<String, Object> properties                                = (Map)source.get(JsonSchema.KEY_PROPERTIES);
		final List<String> requiredPropertyNames                            = (List)source.get(JsonSchema.KEY_REQUIRED);
		final Map<String, Object> views                                     = (Map)source.get(JsonSchema.KEY_VIEWS);
		final Map<String, Object> methods                                   = (Map)source.get(JsonSchema.KEY_METHODS);

		if (properties != null) {

			for (final Entry<String, Object> entry : properties.entrySet()) {

				final String propertyName = entry.getKey();
				final Object value        = entry.getValue();

				if (value instanceof Map) {

					final StructrPropertyDefinition property = StructrPropertyDefinition.deserialize(typeDefinition, propertyName, (Map)value);
					if (property != null) {

						deserializedProperties.put(property.getName(), property);
						typeDefinition.getProperties().add(property);
					}

				} else {

					throw new IllegalStateException("Invalid JSON property definition for property " + propertyName + ", expected object.");
				}
			}
		}

		if (requiredPropertyNames != null) {

			for (final String requiredPropertyName : requiredPropertyNames) {

				// set required properties
				final StructrPropertyDefinition property = deserializedProperties.get(requiredPropertyName);
				if (property != null) {

					property.setRequired(true);

				} else {

					throw new IllegalStateException("Required property " + requiredPropertyName + " not defined for type " + typeDefinition.getName() + ".");
				}
			}
		}

		if (views != null) {

			for (final Entry<String, Object> entry : views.entrySet()) {

				final String viewName = entry.getKey();
				final Object value    = entry.getValue();

				if (value instanceof List) {

					final Set<String> viewProperties = new TreeSet<>((List)value);
					typeDefinition.getViews().put(viewName, viewProperties);

				} else {

					throw new IllegalStateException("View definition " + viewName + " must be of type array.");
				}
			}
		}

		if (methods != null) {

			for (final Entry<String, Object> entry : methods.entrySet()) {

				final String methodName = entry.getKey();
				final Object value      = entry.getValue();

				if (value instanceof String) {

					typeDefinition.getMethods().put(methodName, value.toString());

				} else {

					throw new IllegalStateException("Method definition " + methodName + " must be of type string.");
				}
			}
		}

		return typeDefinition;
	}

	static StructrTypeDefinition deserialize(final StructrSchemaDefinition root, final SchemaNode schemaNode) {

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, schemaNode.getClassName());
		def.deserialize(schemaNode);

		return def;
	}

	static StructrTypeDefinition deserialize(final StructrSchemaDefinition root, final SchemaRelationshipNode schemaRelationship) {

		final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, schemaRelationship.getClassName());
		def.deserialize(schemaRelationship);

		return def;
	}

	// ----- protected methods -----
	protected SchemaNode resolveSchemaNode(final App app, final URI uri) throws FrameworkException {

		// find schema nodes for the given source and target nodes
		final Object source = root.resolveURI(uri);
		if (source != null && source instanceof StructrTypeDefinition) {

			return (SchemaNode)((StructrTypeDefinition)source).getSchemaNode();

		} else {

			if (uri.isAbsolute()) {

				final Class type = StructrApp.resolveSchemaId(uri);
				if (type != null) {

					return app.nodeQuery(SchemaNode.class).andName(type.getSimpleName()).getFirst();
				}
			}
		}

		return null;
	}

	// ----- private methods -----
	private static StructrTypeDefinition determineType(final StructrSchemaDefinition root, final String name, final Map<String, Object> source) {

		if (source.containsKey(JsonSchema.KEY_RELATIONSHIP)) {

			final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, name);
			def.deserialize(source);

			return def;

		} else {

			final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, name);
			def.deserialize(source);

			return def;
		}
	}

	private void addPropertyNameToViews(final String name, final String... views) {

		for (final String viewName : views) {

			Set<String> view = this.views.get(viewName);
			if (view == null) {

				view = new TreeSet<>();
				this.views.put(viewName, view);
			}

			// add property name to view
			view.add(name);
		}
	}

	// ----- nested classes -----
}
