/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.*;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.Visitor;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.operation.*;
import org.structr.schema.openapi.parameter.OpenAPIPropertyQueryParameter;
import org.structr.util.UrlUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

/**
 * @param <T>
 */
public abstract class StructrTypeDefinition<T extends AbstractSchemaNode> implements JsonType, StructrDefinition {

	public static final Set<String> VIEW_BLACKLIST = new LinkedHashSet<>(Arrays.asList("_html_", "all", "category", "custom", "editWidget", "effectiveNameView", "export", "fav", "schema", "ui"));
	public static final Set<String> TagBlacklist   = new LinkedHashSet<>(Arrays.asList("core", "default", "html", "ui"));

	private static final Logger logger = LoggerFactory.getLogger(StructrTypeDefinition.class);

	private final Set<String> filterPropertyBlacklist             = new LinkedHashSet<>(Arrays.asList("id", "type", "hidden"));
	protected final Set<StructrPropertyDefinition> properties     = new TreeSet<>();
	protected final Set<String> inheritedTraits                   = new TreeSet<>();
	protected final Map<String, Set<String>> views                = new TreeMap<>();
	protected final Map<String, String> viewOrder                 = new TreeMap<>();
	protected final List<StructrMethodDefinition> methods         = new LinkedList<>();
	protected final List<StructrGrantDefinition> grants           = new LinkedList<>();
	protected final Set<String> tags                              = new TreeSet<>();
	protected boolean visibleToAuthenticatedUsers                 = false;
	protected boolean visibleToPublicUsers                        = false;
	protected boolean includeInOpenAPI                            = false;
	protected boolean isInterface                                 = false;
	protected boolean isAbstract                                  = false;
	protected boolean changelogDisabled                           = false;
	protected StructrSchemaDefinition root                        = null;
	protected String description                                  = null;
	protected String category                                     = null;
	protected String summary                                      = null;
	protected String icon                                         = null;
	protected String name                                         = null;
	protected T schemaNode                                        = null;

	StructrTypeDefinition(final StructrSchemaDefinition root, final String name) {

		this.root = root;
		this.name = name;
	}

	abstract T createSchemaNode(final Map<String, SchemaNode> schemaNodes, final Map<String, SchemaRelationshipNode> schemaRels, final App app, final PropertyMap createProperties) throws FrameworkException;
	abstract boolean isBlacklisted(final Set<String> blacklist);

	@Override
	public String toString() {
		return "StructrTypeDefinition(" + name + ")";
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrTypeDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
	}

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
	public JsonType setCategory(final String category) {

		this.category = category;

		// test
		this.addTags(category);

		return this;
	}

	@Override
	public String getCategory() {
		return category;
	}

	@Override
	public JsonType setName(final String name) {

		this.name = name;
		return this;
	}

	@Override
	public boolean isInterface() {
		return isInterface;
	}

	@Override
	public JsonType setIsInterface() {
		this.isInterface = true;
		return this;
	}

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public JsonType setIsAbstract() {
		this.isAbstract = true;
		return this;
	}

	@Override
	public boolean isChangelogDisabled() {
		return changelogDisabled;
	}

	@Override
	public JsonType setIsChangelogDisabled() {
		this.changelogDisabled = true;
		return this;
	}

	@Override
	public boolean isVisibleForPublicUsers() {
		return this.visibleToPublicUsers;
	}

	@Override
	public JsonType setVisibleForPublicUsers() {
		this.visibleToPublicUsers = true;
		return this;
	}

	@Override
	public boolean isVisibleForAuthenticatedUsers() {
		return this.visibleToAuthenticatedUsers;
	}

	@Override
	public JsonType setVisibleForAuthenticatedUsers() {
		this.visibleToAuthenticatedUsers = true;
		return this;
	}

	@Override
	public JsonMethod addMethod(final String name, final String source) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, name);

		newMethod.setSource(source);

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod addMethod(final String name) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, name);

		newMethod.setCodeType("java");
		newMethod.setReturnType("void");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod overrideMethod(final String name, final boolean callSuper, final String implementation) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, name);

		newMethod.setSource(implementation);
		newMethod.setOverridesExisting(true);
		newMethod.setCallSuper(callSuper);
		newMethod.setCodeType("java");
		newMethod.setReturnType("void");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod addPropertyGetter(final String propertyName, final Class type) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, "get" + StringUtils.capitalize(propertyName));

		newMethod.setSource("return getProperty(" + propertyName + "Property);");

		if (type.isArray()) {

			newMethod.setReturnType(type.getPackageName() + "." +  type.getSimpleName());
		} else {

			newMethod.setReturnType(type.getName().replace("$", "."));
		}

		newMethod.setCodeType("java");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod addPropertySetter(final String propertyName, final Class type) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, "set" + StringUtils.capitalize(propertyName));

		newMethod.setSource("setProperty(" + propertyName + "Property, value);");
		newMethod.addParameter("value", type.getName());
		newMethod.setCodeType("java");
		newMethod.addException("FrameworkException");
		newMethod.setReturnType("void");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonType addTrait(final String name) {

		this.inheritedTraits.add(name);

		return this;
	}

	@Override
	public Set<JsonProperty> getProperties() {
		return (Set)properties;
	}

	@Override
	public List<JsonMethod> getMethods() {
		return (List)methods;
	}

	@Override
	public List<JsonGrant> getGrants() {
		return (List)grants;
	}

	@Override
	public Set<String> getTags() {
		return tags;
	}

	@Override
	public void addTags(final String... tags) {
		this.tags.addAll(Arrays.asList(tags));
	}

	@Override
	public String getSummary() {
		return this.summary;
	}

	@Override
	public JsonType setSummary(final String summary) {
		this.summary = summary;
		return this;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public JsonType setDescription(final String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getIcon() {
		return this.icon;
	}

	@Override
	public JsonType setIcon(final String icon) {
		this.icon = icon;
		return this;
	}

	@Override
	public boolean includeInOpenAPI() {
		return includeInOpenAPI;
	}

	@Override
	public JsonType setIncludeInOpenAPI(final boolean includeInOpenAPI) {
		this.includeInOpenAPI = includeInOpenAPI;
		return this;
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
	public JsonStringProperty addStringProperty(final String name, final String... views) {

		final StructrStringProperty stringProperty = new StructrStringProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(stringProperty);

		return stringProperty;
	}

	@Override
	public JsonStringProperty addEncryptedProperty(final String name, final String... views) {

		final StructrEncryptedStringProperty encrypted = new StructrEncryptedStringProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(encrypted);

		return encrypted;
	}

	@Override
	public JsonStringProperty addPasswordProperty(final String name, final String... views) {

		final StructrPasswordProperty passwordProperty = new StructrPasswordProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(passwordProperty);

		return passwordProperty;
	}

	@Override
	public JsonStringArrayProperty addStringArrayProperty(final String name, final String... views) {

		final StructrStringArrayProperty stringArrayProperty = new StructrStringArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(stringArrayProperty);

		return stringArrayProperty;
	}

	@Override
	public JsonDateArrayProperty addDateArrayProperty(final String name, final String... views) {

		final StructrDateArrayProperty dateArrayProperty = new StructrDateArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(dateArrayProperty);

		return dateArrayProperty;
	}

	@Override
	public JsonDateProperty addDateProperty(final String name, final String... views) {

		final StructrDateProperty dateProperty = new StructrDateProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(dateProperty);

		return dateProperty;

	}

	@Override
	public JsonIntegerProperty addIntegerProperty(final String name, final String... views) {

		final StructrIntegerProperty numberProperty = new StructrIntegerProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonIntegerArrayProperty addIntegerArrayProperty(final String name, final String... views) {

		final StructrIntegerArrayProperty numberProperty = new StructrIntegerArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonLongProperty addLongProperty(final String name, final String... views) {

		final StructrLongProperty numberProperty = new StructrLongProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonLongArrayProperty addLongArrayProperty(final String name, final String... views) {

		final StructrLongArrayProperty numberProperty = new StructrLongArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonDoubleProperty addDoubleProperty(final String name, final String... views) {

		final StructrNumberProperty numberProperty = new StructrNumberProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonDoubleArrayProperty addDoubleArrayProperty(final String name, final String... views) {

		final StructrNumberArrayProperty numberArrayProperty = new StructrNumberArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberArrayProperty);

		return numberArrayProperty;
	}

	@Override
	public JsonBooleanProperty addBooleanProperty(final String name, final String... views) {

		final StructrBooleanProperty booleanProperty = new StructrBooleanProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(booleanProperty);

		return booleanProperty;
	}

	@Override
	public JsonBooleanArrayProperty addBooleanArrayProperty(final String name, final String... views) {

		final StructrBooleanArrayProperty booleanArrayProperty = new StructrBooleanArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(booleanArrayProperty);

		return booleanArrayProperty;
	}

	@Override
	public JsonByteArrayProperty addByteArrayProperty(final String name, final String... views) {

		final StructrByteArrayProperty byteArrayProperty = new StructrByteArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(byteArrayProperty);

		return byteArrayProperty;
	}

	@Override
	public JsonCountProperty addCountProperty(final String name, final String... views) {

		final StructrCountProperty countProperty = new StructrCountProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(countProperty);

		return countProperty;
	}

	@Override
	public JsonScriptProperty addScriptProperty(final String name, final String... views) {

		final StructrScriptProperty scriptProperty = new StructrScriptProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(scriptProperty);

		return scriptProperty;
	}

	@Override
	public JsonFunctionProperty addFunctionProperty(final String name, final String... views) {

		final StructrFunctionProperty functionProperty = new StructrFunctionProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(functionProperty);

		return functionProperty;
	}

	@Override
	public JsonEnumProperty addEnumProperty(final String name, final String... views) {

		final StructrEnumProperty enumProperty = new StructrEnumProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(enumProperty);

		return enumProperty;
	}

	@Override
	public JsonDynamicProperty addCustomProperty(final String name, final String fqcn, final String... views) {

		final StructrCustomProperty customProperty = new StructrCustomProperty(this, name);

		customProperty.setFqcn(fqcn);

		addPropertyNameToViews(name, views);

		properties.add(customProperty);

		return customProperty;
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
	public JsonReferenceProperty addIdReferenceProperty(final String name, final JsonReferenceProperty referencedProperty, final String... views) {

		final String reference = root.toJsonPointer(referencedProperty.getId());
		final String refType   = referencedProperty.getType();
		final String refName   = referencedProperty.getName();

		final StructrReferenceProperty ref = new IdNotionReferenceProperty(this, name, reference, refType, refName);

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

			case JsonSchema.KEY_PROPERTIES:
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

			case JsonSchema.KEY_VIEWS:
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
		final Map<String, Object> serializedMethods    = new TreeMap<>();
		final Map<String, Object> serializedGrants     = new TreeMap<>();

		// populate properties
		for (final StructrPropertyDefinition property : properties) {
			serializedProperties.put(property.getName(), property.serialize());
		}

		// populate methods
		for (final StructrMethodDefinition method : methods) {

			final String name = method.getName();

			if (serializedMethods.containsKey(name)) {

				// Name is already present in the map, so there are at least
				// two method with the same name.
				final Object currentValue = serializedMethods.get(name);
				if (currentValue instanceof Collection) {

					// add serialized method to collection
					final Collection collection = (Collection)currentValue;

					collection.add(method.serialize());

				} else if (currentValue instanceof Map) {

					// remove map, add collection, add
					// map to collection
					final Map<String, Object> otherMethod   = (Map<String, Object>)serializedMethods.get(name);
					final Map<String, Object> currentMethod = method.serialize();
					final List<Map<String, Object>> list    = new LinkedList<>();

					list.add(otherMethod);
					list.add(currentMethod);

					serializedMethods.put(name, list);

				} else {

					logger.warn("Invalid storage datastructure for methods: {}", currentValue.getClass().getName());
				}

			} else {

				serializedMethods.put(method.getName(), method.serialize());
			}
		}

		// populate grants
		for (final StructrGrantDefinition grant : grants) {

			serializedGrants.put(grant.getPrincipalName(), grant.serialize());
		}

		serializedForm.put(JsonSchema.KEY_TYPE, "object");
		serializedForm.put(JsonSchema.KEY_IS_ABSTRACT, isAbstract);
		serializedForm.put(JsonSchema.KEY_IS_INTERFACE, isInterface);
		serializedForm.put(JsonSchema.KEY_INCLUDE_IN_OPENAPI, includeInOpenAPI);

		if (changelogDisabled) {
			serializedForm.put(JsonSchema.KEY_CHANGELOG_DISABLED, true);
		}

		if (visibleToPublicUsers) {
			serializedForm.put(JsonSchema.KEY_VISIBLE_TO_PUBLIC, true);
		}

		if (visibleToAuthenticatedUsers) {
			serializedForm.put(JsonSchema.KEY_VISIBLE_TO_AUTHENTICATED, true);
		}

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
			serializedForm.put(JsonSchema.KEY_VIEW_ORDER, viewOrder);
		}

		// methods
		if (!serializedMethods.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_METHODS, serializedMethods);
		}

		// grants
		if (!serializedGrants.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_GRANTS, serializedGrants);
		}

		if (inheritedTraits != null) {
			serializedForm.put(JsonSchema.KEY_TRAITS, inheritedTraits);
		}

		if (StringUtils.isNotBlank(category)) {
			serializedForm.put(JsonSchema.KEY_CATEGORY, category);
		}

		if (StringUtils.isNotBlank(summary)) {
			serializedForm.put(JsonSchema.KEY_SUMMARY, summary);
		}

		if (StringUtils.isNotBlank(description)) {
			serializedForm.put(JsonSchema.KEY_DESCRIPTION, description);
		}

		if (StringUtils.isNotBlank(icon)) {
			serializedForm.put(JsonSchema.KEY_ICON, icon);
		}

		if (!tags.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_TAGS, tags);
		}

		return serializedForm;
	}

	void deserialize(final Map<String, Object> source) {

		if (source.containsKey(JsonSchema.KEY_IS_ABSTRACT)) {
			this.isAbstract = (Boolean)source.get(JsonSchema.KEY_IS_ABSTRACT);
		}

		if (source.containsKey(JsonSchema.KEY_IS_INTERFACE)) {
			this.isInterface = (Boolean)source.get(JsonSchema.KEY_IS_INTERFACE);
		}

		if (source.containsKey(JsonSchema.KEY_CHANGELOG_DISABLED)) {
			this.changelogDisabled = (Boolean)source.get(JsonSchema.KEY_CHANGELOG_DISABLED);
		}

		if (source.containsKey(JsonSchema.KEY_VISIBLE_TO_PUBLIC)) {
			this.visibleToPublicUsers = (Boolean)source.get(JsonSchema.KEY_VISIBLE_TO_PUBLIC);
		}

		if (source.containsKey(JsonSchema.KEY_VISIBLE_TO_AUTHENTICATED)) {
			this.visibleToAuthenticatedUsers = (Boolean)source.get(JsonSchema.KEY_VISIBLE_TO_AUTHENTICATED);
		}

		if (source.containsKey(JsonSchema.KEY_TRAITS)) {
			this.inheritedTraits.addAll((Collection)source.get(JsonSchema.KEY_TRAITS));
		}

		if (source.containsKey(JsonSchema.KEY_TAGS)) {

			final Object tagsValue = source.get(JsonSchema.KEY_TAGS);
			if (tagsValue instanceof List) {

				tags.addAll((List<String>)tagsValue);
			}
		}

		if (source.containsKey(JsonSchema.KEY_SUMMARY)) {
			this.summary = (String)source.get(JsonSchema.KEY_SUMMARY);
		}

		if (source.containsKey(JsonSchema.KEY_DESCRIPTION)) {
			this.description = (String)source.get(JsonSchema.KEY_DESCRIPTION);
		}

		final Object _includeInOpenAPI = source.get(JsonSchema.KEY_INCLUDE_IN_OPENAPI);
		if (_includeInOpenAPI != null && _includeInOpenAPI instanceof Boolean) {

			this.includeInOpenAPI = (Boolean)_includeInOpenAPI;
		}
	}

	void deserialize(final Map<String, SchemaNode> schemaNodes, final T schemaNode) {

		for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

			final StructrPropertyDefinition propertyDefinition = StructrPropertyDefinition.deserialize(schemaNodes, this, property);
			if (propertyDefinition != null) {

				properties.add(propertyDefinition);
			}
		}

		for (final SchemaView view : schemaNode.getSchemaViews()) {

			final Set<String> propertySet = new TreeSet<>();
			for (final SchemaProperty property : view.getSchemaProperties()) {
				propertySet.add(property.getName());
			}

			final String nonGraphProperties = view.getNonGraphProperties();
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

				final String order = view.getSortOrder();
				if (order != null) {
					viewOrder.put(view.getName(), order);
				}
			}
		}

		for (final SchemaMethod method : schemaNode.getSchemaMethods()) {

			final StructrMethodDefinition newMethod = StructrMethodDefinition.deserialize(this, method);
			if (newMethod != null) {

				methods.add(newMethod);
			}
		}

		this.changelogDisabled           = schemaNode.changelogDisabled();
		this.summary                     = schemaNode.getSummary();
		this.icon                        = schemaNode.getIcon();
		this.description                 = schemaNode.getDescription();
		this.schemaNode                  = schemaNode;

		/*
		if (this.category == null && getClass().equals(StructrNodeTypeDefinition.class)) {

			final JsonType type = SchemaService.getDynamicSchema().getType(this.getName(), false);
			if (type != null) {

				this.category = type.getCategory();
			}
		}
		*/

		final String[] tagArray = schemaNode.getTags();
		if (tagArray != null) {

			this.tags.addAll(Arrays.asList(tagArray));
		}
	}

	AbstractSchemaNode createDatabaseSchema(final Map<String, SchemaNode> schemaNodes, final Map<String, SchemaRelationshipNode> schemaRels, final App app) throws FrameworkException {

		final Traits schemaNodeTraits                      = Traits.of("SchemaNode");
		final Traits schemaViewTraits                      = Traits.of("SchemaView");
		final Map<String, SchemaProperty> schemaProperties = new TreeMap<>();
		final PropertyMap createProperties                 = new PropertyMap();
		final PropertyMap nodeProperties                   = new PropertyMap();

		// properties that always need to be set
		createProperties.put(schemaNodeTraits.key("isInterface"),            isInterface);
		createProperties.put(schemaNodeTraits.key("isAbstract"),             isAbstract);
		createProperties.put(schemaNodeTraits.key("category"),               category);
		createProperties.put(schemaNodeTraits.key("changelogDisabled"),      changelogDisabled);
		createProperties.put(schemaNodeTraits.key("defaultVisibleToPublic"), visibleToPublicUsers);
		createProperties.put(schemaNodeTraits.key("defaultVisibleToAuth"),   visibleToAuthenticatedUsers);

		final T newSchemaNode = createSchemaNode(schemaNodes, schemaRels, app, createProperties);

		for (final StructrPropertyDefinition property : properties) {

			final SchemaProperty schemaProperty = property.createDatabaseSchema(app, newSchemaNode);
			if (schemaProperty != null) {

				schemaProperties.put(schemaProperty.getName(), schemaProperty);
			}
		}

		// create views and associate the properties
		for (final Entry<String, Set<String>> view : views.entrySet()) {

			final List<NodeInterface> viewProperties = new LinkedList<>();
			final List<String> nonGraphProperties    = new LinkedList<>();

			for (final String propertyName : view.getValue()) {

				final SchemaProperty property = schemaProperties.get(propertyName);
				if (property != null) {

					viewProperties.add(property);

				} else {

					nonGraphProperties.add(propertyName);
				}
			}

			SchemaView viewNode = newSchemaNode.getSchemaView(view.getKey());
			if (viewNode == null) {


				viewNode = app.create("SchemaView",
					new NodeAttribute<>(schemaViewTraits.key("schemaNode"), newSchemaNode),
					new NodeAttribute<>(schemaViewTraits.key("name"), view.getKey())
				).as(SchemaView.class);
			}

			final PropertyMap updateProperties = new PropertyMap();

			updateProperties.put(schemaViewTraits.key("schemaProperties"), viewProperties);
			updateProperties.put(schemaViewTraits.key("nonGraphProperties"), StringUtils.join(nonGraphProperties, ", "));

			if (viewOrder.containsKey(view.getKey())) {
				updateProperties.put(schemaViewTraits.key("sortOrder"), viewOrder.get(view.getKey()));
			}

			// update properties of existing or new schema view node
			viewNode.setProperties(SecurityContext.getSuperUserInstance(), updateProperties);
		}

		for (final StructrMethodDefinition method : methods) {

			// FIXME: this is migration code for ancient imports
			if ("getQoS".equals(method.getName()) && "java".equals(method.getCodeType())) {

				logger.warn("Preventing creation of duplicate getQoS / getQos method pair from legacy import.");

			} else {

				method.createDatabaseSchema(app, newSchemaNode);
			}
		}

		for (final StructrGrantDefinition grant : grants) {
			grant.createDatabaseSchema(app, newSchemaNode);
		}

		final Set<String> mergedTags     = new LinkedHashSet<>(this.tags);
		final String[] existingTagsArray = newSchemaNode.getTags();

		if (existingTagsArray != null) {

			mergedTags.addAll(Arrays.asList(existingTagsArray));
		}

		if (!mergedTags.isEmpty()) {
			nodeProperties.put(schemaNodeTraits.key("tags"), listToArray(mergedTags));
		}

		nodeProperties.put(schemaNodeTraits.key("inheritedTraits"),   getInheritedTraits().toArray(new String[0]));
		nodeProperties.put(schemaNodeTraits.key("includeInOpenAPI"),  includeInOpenAPI());
		nodeProperties.put(schemaNodeTraits.key("summary"),           getSummary());
		nodeProperties.put(schemaNodeTraits.key("description"),       getDescription());
		nodeProperties.put(schemaNodeTraits.key("icon"),              getIcon());

		newSchemaNode.setProperties(SecurityContext.getSuperUserInstance(), nodeProperties);

		return newSchemaNode;
	}

	private Set<String> getInheritedTraits() {
		return inheritedTraits;
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

	Map<String, String> getViewOrder() {
		return viewOrder;
	}

	void initializeReferenceProperties() {

		for (final StructrPropertyDefinition property : properties) {
			property.initializeReferences();
		}
	}

	void diff(final Traits nodeType, final StructrTypeDefinition other) throws FrameworkException {

		diffMethods(nodeType, other);
		diffProperties(nodeType, other);
	}

	void diffProperties(final Traits nodeType, final StructrTypeDefinition other) throws FrameworkException {

		final Map<String, StructrPropertyDefinition> databaseProperties = getMappedProperties();
		final Map<String, StructrPropertyDefinition> structrProperties  = other.getMappedProperties();
		final Set<String> propertiesOnlyInDatabase                      = new TreeSet<>(databaseProperties.keySet());
		final Set<String> propertiesOnlyInStructrSchema                 = new TreeSet<>(structrProperties.keySet());
		final Set<String> bothPropertys                                 = new TreeSet<>(databaseProperties.keySet());

		propertiesOnlyInDatabase.removeAll(structrProperties.keySet());
		propertiesOnlyInStructrSchema.removeAll(databaseProperties.keySet());
		bothPropertys.retainAll(structrProperties.keySet());

		// properties that exist in the database only
		for (final String key : propertiesOnlyInDatabase) {

			final StructrPropertyDefinition property = databaseProperties.get(key);

			handleRemovedProperty(nodeType, property);
		}

		// find detailed differences in the intersection of both schemas
		for (final String name : bothPropertys) {

			final StructrPropertyDefinition localProperty = databaseProperties.get(name);
			final StructrPropertyDefinition otherProperty = structrProperties.get(name);

			// compare properties in detail
			localProperty.diff(otherProperty);
		}
	}

	void diffMethods(final Traits nodeType, final StructrTypeDefinition other) throws FrameworkException {

		final Map<String, StructrMethodDefinition> databaseMethods = getMappedMethodsBySignature();
		final Map<String, StructrMethodDefinition> structrMethods  = other.getMappedMethodsBySignature();
		final Set<String> methodsOnlyInDatabase                    = new TreeSet<>(databaseMethods.keySet());
		final Set<String> methodsOnlyInStructrSchema               = new TreeSet<>(structrMethods.keySet());
		final Set<String> bothMethods                              = new TreeSet<>(databaseMethods.keySet());

		methodsOnlyInDatabase.removeAll(structrMethods.keySet());
		methodsOnlyInStructrSchema.removeAll(databaseMethods.keySet());
		bothMethods.retainAll(structrMethods.keySet());

		// methods that exist in the database only
		for (final String key : methodsOnlyInDatabase) {

			final StructrMethodDefinition method = databaseMethods.get(key);

			handleRemovedMethod(nodeType, method);
		}

		// find detailed differences in the intersection of both schemas
		for (final String name : bothMethods) {

			final StructrMethodDefinition localMethod = databaseMethods.get(name);
			final StructrMethodDefinition otherMethod = structrMethods.get(name);

			// compare methods in detail
			localMethod.diff(otherMethod);
		}
	}

	// ----- static methods -----
	static StructrTypeDefinition deserialize(final StructrSchemaDefinition root, final String name, final Map<String, Object> source) {

		final Map<String, StructrPropertyDefinition> deserializedProperties = new TreeMap<>();
		final StructrTypeDefinition typeDefinition                          = StructrTypeDefinition.determineType(root, name, source);
		final Map<String, Object> properties                                = (Map)source.get(JsonSchema.KEY_PROPERTIES);
		final List<String> requiredPropertyNames                            = (List)source.get(JsonSchema.KEY_REQUIRED);
		final Map<String, Object> views                                     = (Map)source.get(JsonSchema.KEY_VIEWS);
		final Map<String, String> viewOrder                                 = (Map)source.get(JsonSchema.KEY_VIEW_ORDER);
		final Map<String, Object> methods                                   = (Map)source.get(JsonSchema.KEY_METHODS);
		final Map<String, Object> grants                                    = (Map)source.get(JsonSchema.KEY_GRANTS);

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

		if (viewOrder != null) {

			typeDefinition.getViewOrder().putAll(viewOrder);
		}

		if (methods != null) {

			for (final Entry<String, Object> entry : methods.entrySet()) {

				final String methodName = entry.getKey();
				final Object value      = entry.getValue();

				if (value instanceof Map) {

					final StructrMethodDefinition method = StructrMethodDefinition.deserialize(typeDefinition, methodName, (Map)value);
					if (method != null) {

						typeDefinition.getMethods().add(method);
					}

				} else if (value instanceof Collection) {

					// more than on method with the same name exists..
					final Collection<Map<String, Object>> methodList = (Collection)value;

					for (final Map<String, Object> map : methodList) {

						final StructrMethodDefinition method = StructrMethodDefinition.deserialize(typeDefinition, methodName, map);
						if (method != null) {

							typeDefinition.getMethods().add(method);
						}
					}

				} else {

					throw new IllegalStateException("Method definition " + methodName + " must be of type string or map.");
				}
			}
		}

		if (grants != null) {

			for (final Entry<String, Object> entry : grants.entrySet()) {

				final String principalName = entry.getKey();
				final Object value         = entry.getValue();

				if (value instanceof Map) {

					final StructrGrantDefinition grant = StructrGrantDefinition.deserialize(typeDefinition, principalName, (Map)value);
					if (grant != null) {

						typeDefinition.getGrants().add(grant);
					}

				} else {

					throw new IllegalStateException("Grant definition " + principalName + " must be of type map.");
				}
			}
		}

		final Object isAbstractValue = source.get(JsonSchema.KEY_IS_ABSTRACT);
		if (isAbstractValue != null && Boolean.TRUE.equals(isAbstractValue)) {

			typeDefinition.setIsAbstract();
		}

		final Object isInterfaceValue = source.get(JsonSchema.KEY_IS_INTERFACE);
		if (isInterfaceValue != null && Boolean.TRUE.equals(isInterfaceValue)) {

			typeDefinition.setIsInterface();
		}

		final Object isChangelogDisabled = source.get(JsonSchema.KEY_CHANGELOG_DISABLED);
		if (isChangelogDisabled != null && Boolean.TRUE.equals(isChangelogDisabled)) {

			typeDefinition.setIsChangelogDisabled();
		}

		final Object isVisibleToAnonymous = source.get(JsonSchema.KEY_VISIBLE_TO_PUBLIC);
		if (isVisibleToAnonymous != null && Boolean.TRUE.equals(isVisibleToAnonymous)) {

			typeDefinition.setVisibleForPublicUsers();
		}

		final Object isVisibleToAuthenticated = source.get(JsonSchema.KEY_VISIBLE_TO_AUTHENTICATED);
		if (isVisibleToAuthenticated != null && Boolean.TRUE.equals(isVisibleToAuthenticated)) {

			typeDefinition.setVisibleForAuthenticatedUsers();
		}

		final Object categoryValue = source.get(JsonSchema.KEY_CATEGORY);
		if (categoryValue != null) {

			typeDefinition.setCategory(categoryValue.toString());
		}

		final Object description = source.get(JsonSchema.KEY_DESCRIPTION);
		if (description != null) {

			typeDefinition.setDescription(description.toString());
		}

		final Object icon = source.get(JsonSchema.KEY_ICON);
		if (icon != null) {

			typeDefinition.setIcon(icon.toString());
		}

		return typeDefinition;
	}

	static StructrTypeDefinition deserialize(final Map<String, SchemaNode> schemaNodes, final StructrSchemaDefinition root, final SchemaNode schemaNode) {

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, schemaNode.getClassName());
		def.deserialize(schemaNodes, schemaNode);

		return def;
	}

	static StructrTypeDefinition deserialize(final Map<String, SchemaNode> schemaNodes, final StructrSchemaDefinition root, final SchemaRelationshipNode schemaRelationship) {

		final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, schemaRelationship.getClassName());
		def.deserialize(schemaNodes, schemaRelationship);

		return def;
	}

	// ----- protected methods -----
	protected SchemaNode resolveSchemaNode(final Map<String, SchemaNode> schemaNodes, final App app, final URI uri) throws FrameworkException {

		// find schema nodes for the given source and target nodes
		final Object source = root.resolveURI(uri);
		if (source != null && source instanceof StructrTypeDefinition) {

			return (SchemaNode)((StructrTypeDefinition)source).getSchemaNode();

		} else {

			if (uri.isAbsolute()) {

				final String type = StructrApp.resolveSchemaId(uri);
				if (type != null) {

					return schemaNodes.get(type);
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

	private String getParameterizedType(final String type, final String queryString) {

		final StringBuilder buf           = new StringBuilder();
		final List<String> typeParameters = new LinkedList<>();

		buf.append(type);

		if (queryString != null) {

			final Map<String, String> params = UrlUtils.parseQueryString(queryString);
			final String types               = params.get("typeParameters");

			if (types != null) {

				final String[] parameterTypes = types.split("[, ]+");
				for (final String parameterType : parameterTypes) {

					final String trimmed = parameterType.trim();
					if (StringUtils.isNotBlank(trimmed)) {

						typeParameters.add(trimmed);
					}
				}

				buf.append("<");
				buf.append(StringUtils.join(typeParameters, ", "));
				buf.append(">");
			}
		}

		return buf.toString();

	}

	private String resolveParameterizedType(final String parameterizedType) {

		// input is something like "org.structr.core.entity.LinkedTreeNodeImpl<org.structr.web.entity.AbstractFile>"
		// output is LinkedTreeNodeImpl?typeParameters=org.structr.web.entity.AbstractFile

		if (parameterizedType.contains("<") && parameterizedType.contains(">")) {

			final int startOfTypeParam = parameterizedType.indexOf("<");
			final String typeParameter = parameterizedType.substring(startOfTypeParam);
			final String baseType      = parameterizedType.substring(0, startOfTypeParam);

			return baseType + "?typeParameters=" + typeParameter.substring(1, typeParameter.length() - 1);
		}

		return parameterizedType.substring(parameterizedType.lastIndexOf(".") + 1);
	}

	private Map<String, StructrPropertyDefinition> getMappedProperties() {

		final LinkedHashMap<String, StructrPropertyDefinition> mapped = new LinkedHashMap<>();

		for (final StructrPropertyDefinition def : this.properties) {

			mapped.put(def.getName(), def);
		}

		return mapped;
	}

	private Map<String, StructrMethodDefinition> getMappedMethodsBySignature() {

		final LinkedHashMap<String, StructrMethodDefinition> mapped = new LinkedHashMap<>();

		for (final StructrMethodDefinition def : this.methods) {

			mapped.put(def.getSignature(), def);
		}

		return mapped;
	}

	private void handleRemovedMethod(final Traits nodeType, final StructrMethodDefinition method) throws FrameworkException {

		final Set<String> deleteWhitelist = Set.of(
			"Mailbox.getAvailableFoldersOnServer",
			"DOMElement.renderStructrAppLib",
			"MQTTClient.getQoS",
			"File.getFileOnDisk",
			"File.getMinificationTargets",
			"Group.addMember",
			"Group.removeMember",
			"Page.setContent"
		);

		if ("java".equals(method.getCodeType())) {

			final String typeAndName = getName() + "." + method.getName();

			if (method.overridesExisting() || deleteWhitelist.contains(typeAndName)) {

				StructrApp.getInstance().delete(method.getSchemaMethod());

			} else {

				// look for matching method in class
				final Method staticMethod = getMethodOrNull(nodeType, method);
				if (staticMethod != null) {

					StructrApp.getInstance().delete(method.getSchemaMethod());
				}
			}
		}
	}

	private void handleRemovedProperty(final Traits nodeType, final StructrPropertyDefinition property) throws FrameworkException {

		// do not delete properties that are defined in dynamic types
		if (nodeType != null && nodeType.getName().startsWith("org.structr.dynamic.")) {
			return;
		}

		// check if property has moved to static schema
		Field field = getFieldOrNull(nodeType, property.getName());
		if (field == null) {

			// check if property has moved to static schema, with "Property" suffix
			field = getFieldOrNull(nodeType, property.getName() + "Property");
		}

		if (field != null || property instanceof DeletedPropertyDefinition) {

			StructrApp.getInstance().delete(property.getSchemaProperty());
		}
	}

	// ----- OpenAPI methods -----
	public Map<String, Object> serializeOpenAPIOperations(final String tag, Set<String> viewNames) {


		final Map<String, Object> root      = new LinkedHashMap<>();
		final Map<String, Object> singleOps = new LinkedHashMap<>();
		final Map<String, Object> multiOps  = new LinkedHashMap<>();
		final Set<String> views             = getInheritedViewNamesExcludingPublic();

		for (final String view : views) {

			root.put("/" + name + "/" + view, Map.of("get", new OpenAPIGetMultipleOperation(this, view)));
		}

		root.put("/" + name, multiOps);

		multiOps.put("get",    new OpenAPIGetMultipleOperation(this, PropertyView.Public));
		//multiOps.put("patch",  new OpenAPIPatchOperation(this));
		multiOps.put("post",   new OpenAPIPostOperation(this, viewNames));
		multiOps.put("delete", new OpenAPIDeleteMultipleOperation(this));


		for (final String view : views) {

			root.put("/" + name + "/{uuid}" + "/" + view, Map.of("get", new OpenAPIGetSingleOperation(this, view)));
		}

		root.put("/" + name + "/{uuid}", singleOps);

		singleOps.put("get",    new OpenAPIGetSingleOperation(this, PropertyView.Public));
		singleOps.put("put",    new OpenAPIPutSingleOperation(this, viewNames));
		singleOps.put("delete", new OpenAPIDeleteSingleOperation(this));

		// methods
		for (final StructrMethodDefinition method : methods) {

			if (method.isSelected(tag)) {

				root.putAll(method.serializeOpenAPI(this, viewNames));
			}
		}

		return root;
	}

	public Set<String> getTagsForOpenAPI() {

		final Set<String> result = new LinkedHashSet<>();

		//result.addAll(tags);

		// group by type
		result.add(name);

		return result;
	}

	public boolean isSelected(final String tag) {

		final Set<String> tags = getTags();
		boolean selected       = tag == null || tags.contains(tag);

		// don't show types without tags
		if (tags.isEmpty()) {

			return true;

		} else if (tags.isEmpty()) {

			return false;
		}

		// skip blacklisted tags
		if (intersects(TagBlacklist, tags)) {

			// if a tag is selected, it overrides the blacklist
			selected = tag != null && tags.contains(tag);
		}

		return selected;
	}

	public void visitProperties(final Visitor<PropertyKey> visitor, final String viewName) {

		final Traits type = Traits.of(name);
		if (type != null) {

			final Set<PropertyKey> keys = type.getPropertyKeysForView(viewName);
			if (keys != null) {

				keys.stream().forEach(visitor::visit);

			} else {

				// fallback: iterate over id, type, name
				Traits.getDefaultKeys().stream().forEach(visitor::visit);
			}
		}
	}

	public List<Map<String, Object>> getOpenAPIParameters(final String viewName, final int level, final Boolean isGetOperation) {

		final List<Map<String, Object>> params = new LinkedList<>();

		// add indexed properties
		visitProperties(property -> {

			if (property.isIndexed() && !filterPropertyBlacklist.contains(property.jsonName())) {

				params.add(new OpenAPIPropertyQueryParameter(name, property, viewName, level));
			}

		}, viewName);

		params.add(new OpenAPISchemaReference("#/components/parameters/page"));
		params.add(new OpenAPISchemaReference("#/components/parameters/pageSize"));
		params.add(new OpenAPISchemaReference("#/components/parameters/inexactSearch"));

		if (isGetOperation) {
			params.add(new OpenAPISchemaReference("#/components/parameters/outputNestingDepth"));
		}

		return params;
	}

	protected Set<String> getInheritedViewNamesExcludingPublic() {

		final Traits type           = Traits.of(name);
		final Set<String> inherited = new LinkedHashSet<>();

		if (type != null) {

			inherited.addAll(type.getViewNames());

			inherited.removeAll(VIEW_BLACKLIST);
			inherited.remove("public");
		}

		return inherited;
	}

	String resolveTypeReferenceForOpenAPI(final URI reference) {

		// do not include static Structr Schema URIs here
		if (reference != null && !reference.toString().startsWith("https://structr.org/v1.1/")) {

			// non-default base type?
			final String name = StringUtils.substringAfterLast(reference.getPath(), "/");

			return "#/components/schemas/" + name;
		}

		return null;
	}

	private boolean intersects(final Set<String> set1, final Set<String> set2) {

		final Set<String> intersection = new LinkedHashSet<>(set1);

		intersection.retainAll(set2);

		return !intersection.isEmpty();
	}

	private String resolveStaticType(final URI uri) {

		// The following code allows static Java classes to be used as endpoints for dynamic relationships.
		// The FQCN of the class is encoded in the sourceType or targetType URI as https://structr.org/v1.1/static/<fqcn>
		final String prefix = "static/";
		final int start = prefix.length();

		final URI rel = StructrApp.getSchemaBaseURI().relativize(uri);
		final String path = rel.toString();

		if (path.startsWith(prefix)) {

			return path.substring(start);
		}

		return null;
	}

	protected String getStaticTypeReference(final Class type) {
		return "static/" + type.getName();
	}
}
