/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.schema;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javatools.parsers.PlingStemmer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotFoundException;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.CaseHelper;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PermissionPropagation;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.DynamicResourceAccess;
import org.structr.core.entity.Relation;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaMethod;
import static org.structr.core.entity.SchemaMethod.source;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.property.StringProperty;
import org.structr.module.StructrModule;
import org.structr.schema.action.ActionEntry;
import org.structr.schema.action.Actions;
import org.structr.schema.parser.BooleanArrayPropertyParser;
import org.structr.schema.parser.BooleanPropertyParser;
import org.structr.schema.parser.CountPropertyParser;
import org.structr.schema.parser.CypherPropertyParser;
import org.structr.schema.parser.DatePropertyParser;
import org.structr.schema.parser.DoublePropertyParser;
import org.structr.schema.parser.EnumPropertyParser;
import org.structr.schema.parser.FunctionPropertyParser;
import org.structr.schema.parser.IntPropertyParser;
import org.structr.schema.parser.IntegerArrayPropertyParser;
import org.structr.schema.parser.JoinPropertyParser;
import org.structr.schema.parser.LongArrayPropertyParser;
import org.structr.schema.parser.LongPropertyParser;
import org.structr.schema.parser.NotionPropertyParser;
import org.structr.schema.parser.PropertyDefinition;
import org.structr.schema.parser.PropertySourceGenerator;
import org.structr.schema.parser.StringArrayPropertyParser;
import org.structr.schema.parser.StringBasedPropertyDefinition;
import org.structr.schema.parser.StringPropertySourceGenerator;
import org.structr.schema.parser.Validator;

/**
 *
 *
 */
public class SchemaHelper {

	private static final Logger logger = LoggerFactory.getLogger(SchemaHelper.class.getName());

	private static final String WORD_SEPARATOR = "_";

	public enum Type {

		String, StringArray, LongArray, IntegerArray, BooleanArray, Integer, Long, Double, Boolean, Enum, Date, Count, Function, Notion, Cypher, Join, Thumbnail;
	}

	public static final Map<Type, Class<? extends PropertySourceGenerator>> parserMap = new TreeMap<>(new ReverseTypeComparator());
	private static final Map<String, String> normalizedEntityNameCache = new LinkedHashMap<>();

	static {

		// IMPORTANT: parser map must be sorted by type name length
		//            because we look up the parsers using "startsWith"!
		parserMap.put(Type.BooleanArray, BooleanArrayPropertyParser.class);
		parserMap.put(Type.IntegerArray, IntegerArrayPropertyParser.class);
		parserMap.put(Type.StringArray, StringArrayPropertyParser.class);
		parserMap.put(Type.LongArray, LongArrayPropertyParser.class);
		parserMap.put(Type.Function, FunctionPropertyParser.class);
		parserMap.put(Type.Boolean, BooleanPropertyParser.class);
		parserMap.put(Type.Integer, IntPropertyParser.class);
		parserMap.put(Type.String, StringPropertySourceGenerator.class);
		parserMap.put(Type.Double, DoublePropertyParser.class);
		parserMap.put(Type.Notion, NotionPropertyParser.class);
		parserMap.put(Type.Cypher, CypherPropertyParser.class);
		parserMap.put(Type.Long, LongPropertyParser.class);
		parserMap.put(Type.Enum, EnumPropertyParser.class);
		parserMap.put(Type.Date, DatePropertyParser.class);
		parserMap.put(Type.Count, CountPropertyParser.class);
		parserMap.put(Type.Join, JoinPropertyParser.class);
	}

	/**
	 * Tries to normalize (and singularize) the given string so that it
	 * resolves to an existing entity type.
	 *
	 * @param possibleEntityString
	 * @return the normalized entity name in its singular form
	 */
	public static String normalizeEntityName(String possibleEntityString) {

		if (possibleEntityString == null) {

			return null;

		}

		if ("/".equals(possibleEntityString)) {

			return "/";

		}

		final StringBuilder result = new StringBuilder();

		if (possibleEntityString.contains("/")) {

			final String[] names = StringUtils.split(possibleEntityString, "/");

			for (String possibleEntityName : names) {

				// CAUTION: this cache might grow to a very large size, as it
				// contains all normalized mappings for every possible
				// property key / entity name that is ever called.
				String normalizedType = normalizedEntityNameCache.get(possibleEntityName);

				if (normalizedType == null) {

					normalizedType = StringUtils.capitalize(CaseHelper.toUpperCamelCase(stem(possibleEntityName)));

				}

				result.append(normalizedType).append("/");

			}

			return StringUtils.removeEnd(result.toString(), "/");

		} else {

//                      CAUTION: this cache might grow to a very large size, as it
			// contains all normalized mappings for every possible
			// property key / entity name that is ever called.
			String normalizedType = normalizedEntityNameCache.get(possibleEntityString);

			if (normalizedType == null) {

				normalizedType = StringUtils.capitalize(CaseHelper.toUpperCamelCase(stem(possibleEntityString)));

			}

			return normalizedType;
		}
	}

	private static String stem(final String term) {


		String lastWord;
		String begin = "";

		if (StringUtils.contains(term, WORD_SEPARATOR)) {

			lastWord = StringUtils.substringAfterLast(term, WORD_SEPARATOR);
			begin = StringUtils.substringBeforeLast(term, WORD_SEPARATOR);

		} else {

			lastWord = term;

		}

		lastWord = PlingStemmer.stem(lastWord);

		return begin.concat(WORD_SEPARATOR).concat(lastWord);

	}

	public static Class getEntityClassForRawType(final String rawType) {

		// first try: raw name
		Class type = getEntityClassForRawType(rawType, false);
		if (type == null) {

			// second try: normalized name
			type = getEntityClassForRawType(rawType, true);
		}

		return type;
	}

	private static Class getEntityClassForRawType(final String rawType, final boolean normalize) {

		final String normalizedEntityName = normalize ? normalizeEntityName(rawType) : rawType;
		final ConfigurationProvider configuration = StructrApp.getConfiguration();

		// first try: node entity
		Class type = configuration.getNodeEntities().get(normalizedEntityName);

		// second try: relationship entity
		if (type == null) {
			type = configuration.getRelationshipEntities().get(normalizedEntityName);
		}

		// third try: interface
		if (type == null) {
			type = configuration.getInterfaces().get(normalizedEntityName);
		}

		// store type but only if it exists!
		if (type != null) {
			normalizedEntityNameCache.put(rawType, type.getSimpleName());
		}

		// fallback to support generic queries on all types
		if (type == null) {

			if (AbstractNode.class.getSimpleName().equals(rawType)) {
				return AbstractNode.class;
			}

			if (NodeInterface.class.getSimpleName().equals(rawType)) {
				return NodeInterface.class;
			}

			if (AbstractRelationship.class.getSimpleName().equals(rawType)) {
				return AbstractRelationship.class;
			}

			if (RelationshipInterface.class.getSimpleName().equals(rawType)) {
				return RelationshipInterface.class;
			}
		}

		return type;
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer) {

		try {

			final App app = StructrApp.getInstance();

			final List<SchemaNode> existingSchemaNodes = app.nodeQuery(SchemaNode.class).getAsList();

			cleanUnusedDynamicGrants(existingSchemaNodes);

			for (final SchemaNode schemaNode : existingSchemaNodes) {

				createDynamicGrants(schemaNode.getResourceSignature());

			}

			for (final SchemaRelationshipNode schemaRelationship : StructrApp.getInstance().nodeQuery(SchemaRelationshipNode.class).getAsList()) {

				createDynamicGrants(schemaRelationship.getResourceSignature());
				createDynamicGrants(schemaRelationship.getInverseResourceSignature());

			}

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return SchemaService.reloadSchema(errorBuffer);

	}

	public static void cleanUnusedDynamicGrants(final List<SchemaNode> existingSchemaNodes) {

		try {

			final List<DynamicResourceAccess> existingDynamicGrants  = StructrApp.getInstance().nodeQuery(DynamicResourceAccess.class).getAsList();

			final Set<String> existingSchemaNodeNames = new HashSet<>();

			for (final SchemaNode schemaNode : existingSchemaNodes) {

				existingSchemaNodeNames.add(schemaNode.getResourceSignature());
			}

			for (final DynamicResourceAccess grant : existingDynamicGrants) {

				boolean foundAllParts = true;

				final String sig;
				try {
					sig = grant.getResourceSignature();

				} catch (NotFoundException nfe) {
					logger.debug("Unable to get signature from grant");
					continue;
				}

				// Try to find schema nodes for all parts of the grant signature
				final String[] parts = StringUtils.split(sig, "/");

				if (parts != null) {


					for (final String sigPart : parts) {

						if ("/".equals(sigPart) || sigPart.startsWith("_")) {
							continue;
						}

						// If one of the signature parts doesn't have an equivalent existing schema node, remove it
						foundAllParts &= existingSchemaNodeNames.contains(sigPart);
					}
				}

				if (!foundAllParts) {

					logger.info("Did not find all parts of signature, will be removed: {}, ", new Object[]{ sig });

					removeDynamicGrants(sig);
				}


			}

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}

	public static List<DynamicResourceAccess> createDynamicGrants(final String signature) {

		final List<DynamicResourceAccess> grants = new LinkedList<>();
		final long initialFlagsValue = 0;

		final App app = StructrApp.getInstance();
		try {

			ResourceAccess grant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature).getFirst();

			if (grant == null) {

				// create new grant
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, signature),
					new NodeAttribute(DynamicResourceAccess.flags, initialFlagsValue)
				));

				logger.info("New signature created: {}", new Object[]{ (signature) });
			}

			final String schemaSig = schemaResourceSignature(signature);

			ResourceAccess schemaGrant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, schemaSig).getFirst();
			if (schemaGrant == null) {
				// create additional grant for the _schema resource
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, schemaSig),
					new NodeAttribute(DynamicResourceAccess.flags, initialFlagsValue)
				));

				logger.info("New signature created: {}", new Object[]{ schemaSig });
			}

			final String uiSig = uiViewResourceSignature(signature);

			ResourceAccess uiViewGrant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, uiSig).getFirst();
			if (uiViewGrant == null) {

				// create additional grant for the Ui view
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, uiSig),
					new NodeAttribute(DynamicResourceAccess.flags, initialFlagsValue)
				));

				logger.info("New signature created: {}", new Object[]{ uiSig });
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return grants;

	}

	public static void removeAllDynamicGrants() {

		final App app = StructrApp.getInstance();
		try {

			// delete grants
			for (DynamicResourceAccess grant : app.nodeQuery(DynamicResourceAccess.class).getAsList()) {
				app.delete(grant);
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}

	public static void removeDynamicGrants(final String signature) {

		final App app = StructrApp.getInstance();
		try {

			// delete grant
			DynamicResourceAccess grant = app.nodeQuery(DynamicResourceAccess.class).and(DynamicResourceAccess.signature, signature).getFirst();
			if (grant != null) {

				app.delete(grant);
			}

			// delete grant
			DynamicResourceAccess schemaGrant = app.nodeQuery(DynamicResourceAccess.class).and(DynamicResourceAccess.signature, "_schema/" + signature).getFirst();
			if (schemaGrant != null) {

				app.delete(schemaGrant);
			}
			// delete grant
			DynamicResourceAccess viewGrant = app.nodeQuery(DynamicResourceAccess.class).and(DynamicResourceAccess.signature, signature + "/_Ui").getFirst();
			if (viewGrant != null) {

				app.delete(viewGrant);
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}

	}

	public static String extractProperties(final Schema entity, final Set<String> propertyNames, final Set<Validator> validators, final Set<String> enums, final Map<String, Set<String>> views, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyContainer propertyContainer = entity.getPropertyContainer();
		final StringBuilder src                   = new StringBuilder();

		// output property source code and collect views
		for (String propertyName : SchemaHelper.getProperties(propertyContainer)) {

			if (!propertyName.startsWith("__") && propertyContainer.hasProperty(propertyName)) {

				String rawType = propertyContainer.getProperty(propertyName).toString();

				PropertySourceGenerator parser = SchemaHelper.getSourceGenerator(errorBuffer, entity.getClassName(), new StringBasedPropertyDefinition(propertyName, rawType));
				if (parser != null) {

					// migrate properties
					if (entity instanceof AbstractSchemaNode) {
						parser.createSchemaPropertyNode((AbstractSchemaNode)entity, propertyName);
					}
				}
			}
		}

		final List<SchemaProperty> schemaProperties = entity.getSchemaProperties();
		if (schemaProperties != null) {

			for (final SchemaProperty schemaProperty : schemaProperties) {

				if (!schemaProperty.getProperty(SchemaProperty.isBuiltinProperty)) {

					// migrate property source
					if (Type.Function.equals(schemaProperty.getPropertyType())) {

						// Note: This is a temporary migration from the old format to the new readFunction property
						final String format = schemaProperty.getFormat();
						if (format != null) {

							try {
								schemaProperty.setProperty(SchemaProperty.readFunction, format);
								schemaProperty.setProperty(SchemaProperty.format, null);

							} catch (FrameworkException ex) {

								logger.warn("", ex);
							}
						}

					}

					final PropertySourceGenerator parser = SchemaHelper.getSourceGenerator(errorBuffer, entity.getClassName(), schemaProperty);
					if (parser != null) {

						final String propertyName = schemaProperty.getPropertyName();

						// add property name to set for later use
						propertyNames.add(propertyName);

						// append created source from parser
						parser.getPropertySource(src, entity);

						// register global elements created by parser
						validators.addAll(parser.getGlobalValidators());
						enums.addAll(parser.getEnumDefinitions());

						// register property in default view
						addPropertyToView(PropertyView.Ui, propertyName, views);
					}
				}
			}
		}

		return src.toString();
	}

	public static void extractViews(final Schema entity, final Map<String, Set<String>> views, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyContainer propertyContainer = entity.getPropertyContainer();
		final ConfigurationProvider config        = StructrApp.getConfiguration();

		Class superClass = config.getNodeEntityClass(entity.getSuperclassName());
		if (superClass == null) {

			superClass = config.getRelationshipEntityClass(entity.getSuperclassName());
		}

		if (superClass == null) {
			superClass = AbstractNode.class;
		}


		for (final String rawViewName : getViews(propertyContainer)) {

			if (!rawViewName.startsWith("___") && propertyContainer.hasProperty(rawViewName)) {

				final String value = propertyContainer.getProperty(rawViewName).toString();
				final String[] parts = value.split("[,\\s]+");
				final String viewName = rawViewName.substring(2);

				if (entity instanceof AbstractSchemaNode) {

					final List<String> nonGraphProperties = new LinkedList<>();
					final List<SchemaProperty> properties = new LinkedList<>();
					final AbstractSchemaNode schemaNode   = (AbstractSchemaNode)entity;
					final App app                         = StructrApp.getInstance();

					if (app.nodeQuery(SchemaView.class).and(SchemaView.schemaNode, schemaNode).and(AbstractNode.name, viewName).getFirst() == null) {

						// add parts to view, overrides defaults (because of clear() above)
						for (int i = 0; i < parts.length; i++) {

							String propertyName = parts[i].trim();

							while (propertyName.startsWith("_")) {
								propertyName = propertyName.substring(1);
							}

							// remove "Property" suffix in views where people needed to
							// append this as a workaround to include remote properties
							if (propertyName.endsWith("Property")) {
								propertyName = propertyName.substring(0, propertyName.length() - "Property".length());
							}

							final SchemaProperty propertyNode = app.nodeQuery(SchemaProperty.class).and(SchemaProperty.schemaNode, schemaNode).andName(propertyName).getFirst();
							if (propertyNode != null) {

								properties.add(propertyNode);

							} else {

								nonGraphProperties.add(propertyName);
							}
						}

						app.create(SchemaView.class,
							new NodeAttribute<>(SchemaView.schemaNode, schemaNode),
							new NodeAttribute<>(SchemaView.schemaProperties, properties),
							new NodeAttribute<>(SchemaView.name, viewName),
							new NodeAttribute<>(SchemaView.nonGraphProperties, StringUtils.join(nonGraphProperties, ","))
						);

						schemaNode.removeProperty(new StringProperty(rawViewName));
					}
				}
			}
		}

		final List<SchemaView> schemaViews = entity.getSchemaViews();
		if (schemaViews != null) {

			for (final SchemaView schemaView : schemaViews) {

				final String nonGraphProperties = schemaView.getProperty(SchemaView.nonGraphProperties);
				final String viewName           = schemaView.getName();

				// clear view before filling it again
				Set<String> view = views.get(viewName);
				if (view == null) {

					view = new LinkedHashSet<>();
					views.put(viewName, view);
				}

				for (final SchemaProperty property : schemaView.getProperty(SchemaView.schemaProperties)) {

					if (property.getProperty(SchemaProperty.isBuiltinProperty) && !property.getProperty(SchemaProperty.isDynamic)) {

						view.add(property.getPropertyName());

					} else {

						view.add(property.getPropertyName() + "Property");
					}
				}

				// add properties that are not part of the graph
				if (StringUtils.isNotBlank(nonGraphProperties)) {

					for (final String propertyName : nonGraphProperties.split("[, ]+")) {

						String extendedPropertyName = propertyName;
						if (superClass != null) {

							final PropertyKey property = config.getPropertyKeyForJSONName(superClass, propertyName, false);
							if (property != null) {

								// property exists in superclass
								if (property.isDynamic()) {
									extendedPropertyName = extendedPropertyName + "Property";
								}

							} else {

								extendedPropertyName = extendedPropertyName + "Property";
							}

						} else {
							extendedPropertyName = extendedPropertyName + "Property";

						}

						view.add(extendedPropertyName);
					}
				}
			}
		}
	}

	public static void extractMethods(final Schema entity, final Map<Actions.Type, List<ActionEntry>> actions) throws FrameworkException {

		final PropertyContainer propertyContainer = entity.getPropertyContainer();

		for (final String rawActionName : getActions(propertyContainer)) {

			if (propertyContainer.hasProperty(rawActionName)) {

				final String value = propertyContainer.getProperty(rawActionName).toString();

				if (entity instanceof AbstractSchemaNode) {

					final AbstractSchemaNode schemaNode = (AbstractSchemaNode)entity;
					final App app                       = StructrApp.getInstance();
					final String methodName             = rawActionName.substring(3);

					if (app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, schemaNode).and(AbstractNode.name, methodName).getFirst() == null) {

						app.create(SchemaMethod.class,
							new NodeAttribute<>(SchemaMethod.schemaNode, schemaNode),
							new NodeAttribute<>(SchemaMethod.name, methodName),
							new NodeAttribute<>(SchemaMethod.source, value)
						);

						schemaNode.removeProperty(new StringProperty(rawActionName));
					}
				}
			}
		}

		final List<SchemaMethod> schemaMethods = entity.getSchemaMethods();
		if (schemaMethods != null) {

			for (final SchemaMethod schemaMethod : schemaMethods) {

				final ActionEntry entry      = schemaMethod.getActionEntry();
				List<ActionEntry> actionList = actions.get(entry.getType());

				if (actionList == null) {

					actionList = new LinkedList<>();
					actions.put(entry.getType(), actionList);
				}

				actionList.add(entry);

				Collections.sort(actionList);

			}
		}
	}

	public static void addPropertyToView(final String viewName, final String propertyName, final Map<String, Set<String>> views) {

		Set<String> view = views.get(viewName);
		if (view == null) {

			view = new LinkedHashSet<>();
			views.put(viewName, view);
		}

		view.add(SchemaHelper.cleanPropertyName(propertyName) + "Property");
	}

	public static Iterable<String> getProperties(final PropertyContainer propertyContainer) {

		final List<String> keys = new LinkedList<>();

		for (final String key : propertyContainer.getPropertyKeys()) {

			if (propertyContainer.hasProperty(key) && key.startsWith("_")) {

				keys.add(key);
			}
		}

		return keys;
	}

	public static Iterable<String> getViews(final PropertyContainer propertyContainer) {

		final List<String> keys = new LinkedList<>();

		for (final String key : propertyContainer.getPropertyKeys()) {

			if (propertyContainer.hasProperty(key) && key.startsWith("__")) {

				keys.add(key);
			}
		}

		return keys;
	}

	public static Iterable<String> getActions(final PropertyContainer propertyContainer) {

		final List<String> keys = new LinkedList<>();

		for (final String key : propertyContainer.getPropertyKeys()) {

			if (propertyContainer.hasProperty(key) && key.startsWith("___")) {

				keys.add(key);
			}
		}

		return keys;
	}

	public static void formatView(final StringBuilder src, final String _className, final String viewName, final String view, final Set<String> viewProperties) {

		// output default view
		src.append("\n\tpublic static final View ").append(viewName).append("View = new View(").append(_className).append(".class, \"").append(view).append("\",\n");
		src.append("\t\t");

		for (final Iterator<String> it = viewProperties.iterator(); it.hasNext();) {

			String propertyName = it.next();

			// convert _-prefixed property names to "real" name
			if (propertyName.startsWith("_")) {
				propertyName = propertyName.substring(1) + "Property";
			}

			src.append(propertyName);

			if (it.hasNext()) {
				src.append(", ");
			}
		}

		src.append("\n\t);\n");

	}

	public static void formatImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder src, final Class baseType) {

		src.append("import ").append(baseType.getName()).append(";\n");
		src.append("import ").append(ConfigurationProvider.class.getName()).append(";\n");
		src.append("import ").append(GraphObjectComparator.class.getName()).append(";\n");
		src.append("import ").append(PermissionPropagation.class.getName()).append(";\n");
		src.append("import ").append(FrameworkException.class.getName()).append(";\n");
		src.append("import ").append(DatePropertyParser.class.getName()).append(";\n");
		src.append("import ").append(ModificationQueue.class.getName()).append(";\n");
		src.append("import ").append(PropertyConverter.class.getName()).append(";\n");
		src.append("import ").append(ValidationHelper.class.getName()).append(";\n");
		src.append("import ").append(SecurityContext.class.getName()).append(";\n");
		src.append("import ").append(LinkedHashSet.class.getName()).append(";\n");
		src.append("import ").append(PropertyView.class.getName()).append(";\n");
		src.append("import ").append(GraphObject.class.getName()).append(";\n");
		src.append("import ").append(ErrorBuffer.class.getName()).append(";\n");
		src.append("import ").append(StringUtils.class.getName()).append(";\n");
		src.append("import ").append(Collections.class.getName()).append(";\n");
		src.append("import ").append(StructrApp.class.getName()).append(";\n");
		src.append("import ").append(LinkedList.class.getName()).append(";\n");
		src.append("import ").append(Collection.class.getName()).append(";\n");
		src.append("import ").append(Services.class.getName()).append(";\n");
		src.append("import ").append(Actions.class.getName()).append(";\n");
		src.append("import ").append(HashMap.class.getName()).append(";\n");
		src.append("import ").append(Export.class.getName()).append(";\n");
		src.append("import ").append(View.class.getName()).append(";\n");
		src.append("import ").append(List.class.getName()).append(";\n");
		src.append("import ").append(Map.class.getName()).append(";\n");
		src.append("import ").append(Set.class.getName()).append(";\n");

		if (hasRestClasses()) {
			src.append("import org.structr.rest.RestMethodResult;\n");
		}

		src.append("import org.structr.core.property.*;\n");

		if (hasUiClasses()) {
			src.append("import org.structr.web.property.*;\n");
		}

		for (final StructrModule module : StructrApp.getConfiguration().getModules().values()) {
			module.insertImportStatements(schemaNode, src);
		}

		src.append("import org.structr.core.notion.*;\n");
		src.append("import org.structr.core.entity.*;\n\n");

	}

	public static void formatInterfacesFromModules(final StringBuilder src, final SchemaNode schemaNode) {

		final Set<String> interfaces = new LinkedHashSet<>();
		boolean firstInterface       = true;

		// collect all interfaces that a module wants to add to the given type
		for (final StructrModule module : StructrApp.getConfiguration().getModules().values()) {

			final Set<String> moduleInterfacesForType = module.getInterfacesForType(schemaNode);
			if (moduleInterfacesForType != null) {

				interfaces.addAll(moduleInterfacesForType);
			}
		}

		// add all interfaces dynamically
		for (final String iface : interfaces) {

			src.append(firstInterface ? " implements " : ", ");
			src.append(iface);

			firstInterface = false;
		}
	}

	public static String cleanPropertyName(final String propertyName) {
		return propertyName.replaceAll("[^\\w]+", "");
	}

	public static void formatValidators(final StringBuilder src, final Set<Validator> validators) {

		if (!validators.isEmpty()) {

			src.append("\n\t@Override\n");
			src.append("\tpublic boolean isValid(final ErrorBuffer errorBuffer) {\n\n");
			src.append("\t\tboolean valid = super.isValid(errorBuffer);\n\n");

			for (final Validator validator : validators) {
				src.append("\t\tvalid &= ").append(validator.getSource("this", true)).append(";\n");
			}

			src.append("\n\t\treturn valid;\n");
			src.append("\t}\n");
		}

	}

	public static void formatDynamicValidators(final StringBuilder src, final Set<Validator> validators) {

		if (!validators.isEmpty()) {

			src.append("\tpublic static boolean isValid(final AbstractNode obj, final ErrorBuffer errorBuffer) {\n\n");
			src.append("\t\tboolean valid = true;\n\n");

			for (final Validator validator : validators) {
				src.append("\t\tvalid &= ").append(validator.getSource("obj", false)).append(";\n");
			}

			src.append("\n\t\treturn valid;\n");
			src.append("\t}\n\n");
		}
	}

	public static void formatSaveActions(final AbstractSchemaNode schemaNode, final StringBuilder src, final Map<Actions.Type, List<ActionEntry>> saveActions) {

		// save actions..
		for (final Map.Entry<Actions.Type, List<ActionEntry>> entry : saveActions.entrySet()) {

			final List<ActionEntry> actionList = entry.getValue();
			final Actions.Type type = entry.getKey();

			switch (type) {

				case Custom:
					// active actions are exported stored functions
					// that can be called by POSTing on the entity
					formatActiveActions(src, actionList);
					break;

				default:
					// passive actions are actions that are executed
					// automtatically on creation / modification etc.
					formatPassiveSaveActions(schemaNode, src, type, actionList);
					break;
			}
		}

	}

	public static void formatDynamicSaveActions(final StringBuilder src, final Map<Actions.Type, List<ActionEntry>> saveActions) {

		// save actions..
		for (final Map.Entry<Actions.Type, List<ActionEntry>> entry : saveActions.entrySet()) {

			final List<ActionEntry> actionList = entry.getValue();
			final Actions.Type type = entry.getKey();

			if (!actionList.isEmpty()) {

				switch (type) {

					case Custom:
						// active actions are exported stored functions
						// that can be called by POSTing on the entity
						//formatDynamicActiveActions(src, actionList);
						break;
					default:
						// passive actions are actions that are executed
						// automtatically on creation / modification etc.
						formatDynamicPassiveSaveActions(src, type, actionList);
						break;
				}
			}
		}

	}

	public static void formatActiveActions(final StringBuilder src, final List<ActionEntry> actionList) {

		for (final ActionEntry action : actionList) {

			src.append("\n\t@Export\n");
			src.append("\tpublic Object ");
			src.append(action.getName());
			src.append("(final Map<String, Object> parameters) throws FrameworkException {\n\n");

			src.append("\t\treturn ");
			src.append(action.getSource("this", true));
			src.append(";\n\n");
			src.append("\t}\n");
		}

	}

	public static void formatDynamicActiveActions(final StringBuilder src, final List<ActionEntry> actionList) {

		for (final ActionEntry action : actionList) {

			src.append("\tpublic static Object ");
			src.append(action.getName());
			src.append("(final AbstractNode obj) throws FrameworkException {\n\n");

			src.append("\t\treturn ");
			src.append(action.getSource("obj"));
			src.append(";\n\n");
			src.append("\t}\n");
		}

	}

	public static void formatPassiveSaveActions(final AbstractSchemaNode schemaNode, final StringBuilder src, final Actions.Type type, final List<ActionEntry> actionList) {

		src.append("\n\t@Override\n");
		src.append("\tpublic boolean ");
		src.append(type.getMethod());
		src.append("(");
		src.append(type.getSignature());
		src.append(") throws FrameworkException {\n\n");
		src.append("\t\tboolean valid = super.");
		src.append(type.getMethod());
		src.append("(");
		src.append(type.getParameters());
		src.append(");\n\n");

		for (final StructrModule module : StructrApp.getConfiguration().getModules().values()) {
			module.insertSaveAction(schemaNode, src, type);
		}

		for (final ActionEntry action : actionList) {

			src.append("\t\t").append(action.getSource("this")).append(";\n");
		}

		src.append("\n\t\treturn valid;\n");
		src.append("\t}\n");

	}

	public static void formatDynamicPassiveSaveActions(final StringBuilder src, final Actions.Type type, final List<ActionEntry> actionList) {

		src.append("\tpublic static boolean ");
		src.append(type.getMethod());
		src.append("(final AbstractNode obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {\n\n");
		src.append("\t\tboolean valid = obj.isValid(errorBuffer);\n\n");

		for (final ActionEntry action : actionList) {

			src.append("\t\t").append(action.getSource("obj")).append(";\n");
		}

		src.append("\n\t\treturn valid;\n");
		src.append("\t}\n\n");

	}


	private static Map<String, Object> getPropertiesForView(final SecurityContext securityContext, final Class type, final String propertyView) throws FrameworkException {

		final Set<PropertyKey> properties = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(type, propertyView));
		final Map<String, Object> propertyConverterMap = new LinkedHashMap<>();

		for (PropertyKey property : properties) {

			propertyConverterMap.put(property.jsonName(), getPropertyInfo(securityContext, property));
		}

		return propertyConverterMap;
	}

	// ----- public static methods -----
	public static List<GraphObjectMap> getSchemaTypeInfo(final SecurityContext securityContext, final String rawType, final Class type, final String propertyView) throws FrameworkException {

		List<GraphObjectMap> resultList = new LinkedList<>();

		// create & add schema information

		if (type != null) {

			if (propertyView != null) {

				for (final Map.Entry<String, Object> entry : getPropertiesForView(securityContext, type, propertyView).entrySet()) {

					final GraphObjectMap property = new GraphObjectMap();

					for (final Map.Entry<String, Object> prop : ((Map<String, Object>) entry.getValue()).entrySet()) {

						property.setProperty(new GenericProperty(prop.getKey()), prop.getValue());
					}

					resultList.add(property);
				}

			} else {

				final GraphObjectMap schema = new GraphObjectMap();

				resultList.add(schema);

				String url = "/".concat(rawType);

				schema.setProperty(new StringProperty("url"), url);
				schema.setProperty(new StringProperty("type"), type.getSimpleName());
				schema.setProperty(new StringProperty("className"), type.getName());
				schema.setProperty(new StringProperty("extendsClass"), type.getSuperclass().getName());
				schema.setProperty(new BooleanProperty("isRel"), AbstractRelationship.class.isAssignableFrom(type));
				schema.setProperty(new LongProperty("flags"), SecurityContext.getResourceFlags(rawType));

				Set<String> propertyViews = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertyViewsForType(type));

				// list property sets for all views
				Map<String, Map<String, Object>> views = new TreeMap();
				schema.setProperty(new GenericProperty("views"), views);

				for (final String view : propertyViews) {

					if (!View.INTERNAL_GRAPH_VIEW.equals(view)) {

						views.put(view, getPropertiesForView(securityContext, type, view));
					}
				}
			}
		}

		return resultList;
	}


	public static Map<String, Object> getPropertyInfo(final SecurityContext securityContext, final PropertyKey property) {

		final Map<String, Object> map = new LinkedHashMap();

		map.put("dbName", property.dbName());
		map.put("jsonName", property.jsonName());
		map.put("className", property.getClass().getName());

		final Class declaringClass = property.getDeclaringClass();

		map.put("declaringClass", declaringClass.getSimpleName());
		map.put("defaultValue", property.defaultValue());
		if (property instanceof StringProperty) {
			map.put("contentType", ((StringProperty) property).contentType());
		}
		map.put("format", property.format());
		map.put("readOnly", property.isReadOnly());
		map.put("system", property.isSystemInternal());
		map.put("indexed", property.isIndexed());
		map.put("indexedWhenEmpty", property.isIndexedWhenEmpty());
		map.put("unique", property.isUnique());
		map.put("notNull", property.isNotNull());
		map.put("dynamic", property.isDynamic());

		final Class<? extends GraphObject> relatedType = property.relatedType();
		if (relatedType != null) {

			map.put("relatedType", relatedType.getName());
			map.put("type", relatedType.getSimpleName());
			map.put("uiType", relatedType.getSimpleName() + (property.isCollection() ? "[]" : ""));

		} else {

			map.put("type", property.typeName());
			map.put("uiType", property.typeName() + (property.isCollection() ? "[]" : ""));
		}

		map.put("isCollection", property.isCollection());

		final PropertyConverter databaseConverter = property.databaseConverter(securityContext, null);
		final PropertyConverter inputConverter = property.inputConverter(securityContext);

		if (databaseConverter != null) {

			map.put("databaseConverter", databaseConverter.getClass().getName());
		}

		if (inputConverter != null) {

			map.put("inputConverter", inputConverter.getClass().getName());
		}

		//if (declaringClass != null && ("org.structr.dynamic".equals(declaringClass.getPackage().getName()))) {
		if (declaringClass != null && property instanceof RelationProperty) {

			Relation relation = ((RelationProperty) property).getRelation();
			if (relation != null) {

				map.put("relationshipType", relation.name());
			}

		}

		return map;
	}

	// ----- private methods -----
	private static PropertySourceGenerator getSourceGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition propertyDefinition) throws FrameworkException {

		final String propertyName                                  = propertyDefinition.getPropertyName();
		final Type propertyType                                    = propertyDefinition.getPropertyType();
		final Class<? extends PropertySourceGenerator> parserClass = parserMap.get(propertyType);

		try {

			return parserClass.getConstructor(ErrorBuffer.class, String.class, PropertyDefinition.class).newInstance(errorBuffer, className, propertyDefinition);

		} catch (Throwable t) {
			logger.warn("", t);
		}

		errorBuffer.add(new InvalidPropertySchemaToken(SchemaProperty.class.getSimpleName(), propertyName, "invalid_property_definition", "Unknow value type " + source + ", options are " + Arrays.asList(Type.values()) + "."));
		throw new FrameworkException(422, "Invalid property definition for property " + propertyDefinition.getPropertyName(), errorBuffer);
	}

	private static boolean hasRestClasses() {

		try {

			Class.forName("org.structr.rest.RestMethodResult");

			// success
			return true;

		} catch (Throwable t) {
		}

		return false;
	}

	private static boolean hasUiClasses() {

		try {

			Class.forName("org.structr.web.property.ThumbnailProperty");

			// success
			return true;

		} catch (Throwable t) {
		}

		return false;
	}

	public static boolean hasPeerToPeerService() {

		try {

			Class.forName("org.structr.net.SharedNodeInterface");

			// success
			return true;

		} catch (Throwable t) {
		}

		return false;
	}

	private static class ReverseTypeComparator implements Comparator<Type> {

		@Override
		public int compare(final Type o1, final Type o2) {
			return o2.name().compareTo(o1.name());
		}
	}

	private static String schemaResourceSignature(final String signature) {
		return "_schema/" + signature;
	}

	private static String uiViewResourceSignature(final String signature) {
		return signature + "/_Ui";
	}

}
