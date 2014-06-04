/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.common.CaseHelper;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.DynamicResourceAccess;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.parser.Functions;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.ActionEntry;
import org.structr.schema.parser.BooleanPropertyParser;
import org.structr.schema.parser.CountPropertyParser;
import org.structr.schema.parser.CypherPropertyParser;
import org.structr.schema.parser.DatePropertyParser;
import org.structr.schema.parser.DoublePropertyParser;
import org.structr.schema.parser.EnumPropertyParser;
import org.structr.schema.parser.FunctionPropertyParser;
import org.structr.schema.parser.NotionPropertyParser;
import org.structr.schema.parser.IntPropertyParser;
import org.structr.schema.parser.LongPropertyParser;
import org.structr.schema.parser.PropertyParser;
import org.structr.schema.parser.StringArrayPropertyParser;
import org.structr.schema.parser.StringPropertyParser;

/**
 *
 * @author Christian Morgner
 */
public class SchemaHelper {

	public enum Type {

		String, StringArray, Integer, Long, Double, Boolean, Enum, Date, Count, Function, Notion, Cypher
	}

	private static final ThreadLocalMatcher threadLocalTemplateMatcher        = new ThreadLocalMatcher("\\$\\{[^}]*\\}");
	private static final Map<String, String> normalizedEntityNameCache        = new LinkedHashMap<>();
	private static final Map<Type, Class<? extends PropertyParser>> parserMap = new LinkedHashMap<>();

	static {

		// IMPORTANT: parser map must be sorted by type name length
		//            because we look up the parsers using "startsWith"!

		parserMap.put(Type.StringArray, StringArrayPropertyParser.class);
		parserMap.put(Type.Function,    FunctionPropertyParser.class);
		parserMap.put(Type.Boolean,     BooleanPropertyParser.class);
		parserMap.put(Type.Integer,     IntPropertyParser.class);
		parserMap.put(Type.String,      StringPropertyParser.class);
		parserMap.put(Type.Double,      DoublePropertyParser.class);
		parserMap.put(Type.Notion,      NotionPropertyParser.class);
		parserMap.put(Type.Cypher,      CypherPropertyParser.class);
		parserMap.put(Type.Long,        LongPropertyParser.class);
		parserMap.put(Type.Enum,        EnumPropertyParser.class);
		parserMap.put(Type.Date,        DatePropertyParser.class);
		parserMap.put(Type.Count,       CountPropertyParser.class);
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

		StringBuilder result = new StringBuilder();

		if (possibleEntityString.contains("/")) {

			String[] names = StringUtils.split(possibleEntityString, "/");

			for (String possibleEntityName : names) {

				// CAUTION: this cache might grow to a very large size, as it
				// contains all normalized mappings for every possible
				// property key / entity name that is ever called.
				String normalizedType = normalizedEntityNameCache.get(possibleEntityName);

				if (normalizedType == null) {

					normalizedType = StringUtils.capitalize(CaseHelper.toUpperCamelCase(possibleEntityName));

					if (normalizedType.endsWith("ies")) {

						normalizedType = normalizedType.substring(0, normalizedType.length() - 3).concat("y");

					} else if (!normalizedType.endsWith("ss") && normalizedType.endsWith("s")) {

						normalizedType = normalizedType.substring(0, normalizedType.length() - 1);
					}
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

				normalizedType = StringUtils.capitalize(CaseHelper.toUpperCamelCase(possibleEntityString));

				if (normalizedType.endsWith("ies")) {

					normalizedType = normalizedType.substring(0, normalizedType.length() - 3).concat("y");

				} else if (!normalizedType.endsWith("ss") && normalizedType.endsWith("s")) {

					normalizedType = normalizedType.substring(0, normalizedType.length() - 1);
				}
			}

			return normalizedType;
		}
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

		return type;
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer) {

		final App app = StructrApp.getInstance();

		try {

			removeAllDynamicGrants();

			for (final SchemaNode schemaNode : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {

				createDynamicGrants(schemaNode.getResourceSignature(), null);

			}

			for (final SchemaRelationship schemaRelationship : StructrApp.getInstance().relationshipQuery(SchemaRelationship.class).getAsList()) {

				createDynamicGrants(schemaRelationship.getResourceSignature(), null);
				createDynamicGrants(schemaRelationship.getInverseResourceSignature(), null);

			}

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return SchemaService.reloadSchema(errorBuffer);

	}

	public static List<DynamicResourceAccess> createDynamicGrants(final String signature, final Long flags) {

		List<DynamicResourceAccess> grants = new LinkedList<>();

		final App app = StructrApp.getInstance();
		try {

			ResourceAccess grant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature).getFirst();
			long flagsValue = 255;

			// set value from grant flags
			if (flags != null) {
				flagsValue = flags.longValue();
			}

			if (grant == null) {

				// create new grant
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, signature),
					new NodeAttribute(DynamicResourceAccess.flags, flagsValue)
				));

				// create additional grant for the _schema resource
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, "_schema/" + signature),
					new NodeAttribute(DynamicResourceAccess.flags, flagsValue)
				));

				// create additional grant for the Ui view
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, signature + "/_Ui"),
					new NodeAttribute(DynamicResourceAccess.flags, flagsValue)
				));

			} else {

				// modify flags of grant
				// Caution: this means that the SchemaNode is the
				// primary source for resource access flag values
				// of dynamic nodes
				grant.setProperty(ResourceAccess.flags, flagsValue);
			}

		} catch (Throwable t) {

			t.printStackTrace();
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

			t.printStackTrace();
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

			t.printStackTrace();
		}

	}

	public static String extractProperties(final Schema entity, final Set<String> validators, final Set<String> enums, final Map<String, Set<String>> views, final Map<Actions.Type, List<ActionEntry>> actions, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyContainer propertyContainer = entity.getPropertyContainer();
 		final StringBuilder src                   = new StringBuilder();

		// output property source code and collect views
		for (String propertyName : SchemaHelper.getProperties(propertyContainer)) {

			if (!propertyName.startsWith("__") && propertyContainer.hasProperty(propertyName)) {

				String rawType = propertyContainer.getProperty(propertyName).toString();

				String dbName = null;
				// detect optional db name
				if (rawType.contains("|")) {

					dbName = rawType.substring(0, rawType.indexOf("|"));
					rawType = rawType.substring(rawType.indexOf("|")+1);

				}

				boolean notNull = false;

				// detect and remove not-null constraint
				if (rawType.startsWith("+")) {
					rawType = rawType.substring(1);
					notNull = true;
				}

				String defaultValue = null;

				// detect and remove default value
				if (rawType.contains(":") && !rawType.startsWith(Type.Cypher.name())) {

					final int lastIndex = rawType.lastIndexOf(":");
					defaultValue = rawType.substring(lastIndex+1);
					rawType = rawType.substring(0, lastIndex);

				}

				PropertyParser parser = SchemaHelper.getParserForRawValue(errorBuffer, entity.getClassName(), propertyName, dbName, rawType, notNull, defaultValue);
				if (parser != null) {

					// append created source from parser
					src.append(parser.getPropertySource(entity, errorBuffer));

					// register global elements created by parser
					validators.addAll(parser.getGlobalValidators());
					enums.addAll(parser.getEnumDefinitions());

					// register property in default view
					//addPropertyToView(PropertyView.Public, propertyName.substring(1), views);
					addPropertyToView(PropertyView.Ui, propertyName.substring(1), views);
				}
			}
		}

		for (final String rawViewName : getViews(propertyContainer)) {

			if (!rawViewName.startsWith("___") && propertyContainer.hasProperty(rawViewName)) {

				final String value = propertyContainer.getProperty(rawViewName).toString();
				final String[] parts = value.split("[,\\s]+");
				final String viewName = rawViewName.substring(2);

				// clear view before filling it again
				Set<String> view = views.get(viewName);
				if (view == null) {

					view = new LinkedHashSet<>();
					views.put(viewName, view);

				} else {

					view.clear();
				}

				// add parts to view, overrides defaults (because of clear() above)
				for (int i = 0; i < parts.length; i++) {
					view.add(parts[i].trim());
				}
			}
		}

		for (final String rawActionName : getActions(propertyContainer)) {

			if (propertyContainer.hasProperty(rawActionName)) {

				// split value on ";"
				final String value     = propertyContainer.getProperty(rawActionName).toString();
				final String[] parts1  = value.split("[;]+");
				final int parts1Length = parts1.length;

				for (int i=0; i<parts1Length; i++) {

					// split value on "&&"
					final String[] parts2     = parts1[i].split("\\&\\&");
					final int parts2Length    = parts2.length;

					for (int j=0; j<parts2Length; j++) {

						// since the parts in this loop are separated by "&&", all parts AFTER
						// the first one should only be run if the others before succeeded.

						final ActionEntry entry      = new ActionEntry(rawActionName, parts2[j], j == 0);
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
		}

		return src.toString();
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

	public static void formatImportStatements(final StringBuilder src, final Class baseType) {

		src.append("import ").append(baseType.getName()).append(";\n");
		src.append("import ").append(GraphObjectComparator.class.getName()).append(";\n");
		src.append("import ").append(FrameworkException.class.getName()).append(";\n");
		src.append("import ").append(ValidationHelper.class.getName()).append(";\n");
		src.append("import ").append(SecurityContext.class.getName()).append(";\n");
		src.append("import ").append(GraphObject.class.getName()).append(";\n");
		src.append("import ").append(Actions.class.getName()).append(";\n");
		src.append("import ").append(PropertyView.class.getName()).append(";\n");
		src.append("import ").append(ErrorBuffer.class.getName()).append(";\n");
		src.append("import ").append(Export.class.getName()).append(";\n");
		src.append("import ").append(View.class.getName()).append(";\n");
		src.append("import ").append(List.class.getName()).append(";\n");
		src.append("import org.structr.rest.RestMethodResult;\n");
		src.append("import org.structr.core.validator.*;\n");
		src.append("import org.structr.core.property.*;\n");
		src.append("import org.structr.core.notion.*;\n");
		src.append("import org.structr.core.entity.*;\n\n");

	}

	public static String cleanPropertyName(final String propertyName) {
		return propertyName.replaceAll("[^\\w]+", "");
	}

	public static void formatValidators(final StringBuilder src, final Set<String> validators) {

		if (!validators.isEmpty()) {

			src.append("\n\t@Override\n");
			src.append("\tpublic boolean isValid(final ErrorBuffer errorBuffer) {\n\n");
			src.append("\t\tboolean error = false;\n\n");

			for (final String validator : validators) {
				src.append("\t\terror |= ").append(validator).append(";\n");
			}

			src.append("\n\t\treturn !error;\n");
			src.append("\t}\n");
		}

	}

	public static void formatSaveActions(final StringBuilder src, final Map<Actions.Type, List<ActionEntry>> saveActions) {

		// save actions..
		for (final Map.Entry<Actions.Type, List<ActionEntry>> entry : saveActions.entrySet()) {

			final List<ActionEntry> actionList = entry.getValue();
			final Actions.Type type            = entry.getKey();

			if (!actionList.isEmpty()) {

				switch (type) {

					case Custom:
						// active actions are exported stored functions
						// that can be called by POSTing on the entity
						formatActiveActions(src, actionList);
						break;

					default:
						// passive actions are actions that are executed
						// automtatically on creation / modification etc.
						formatPassiveSaveActions(src, type, actionList);
						break;
				}
			}
		}

	}

	public static void formatActiveActions(final StringBuilder src, final List<ActionEntry> actionList) {

		for (final ActionEntry action : actionList) {

			src.append("\n\t@Export\n");
			src.append("\tpublic RestMethodResult ");
			src.append(action.getName());
			src.append("() throws FrameworkException {\n\n");

			src.append("\t\t");
			src.append(action.getSource());
			src.append(";\n\n");

			src.append("\t\treturn new RestMethodResult(200);\n");
			src.append("\t}\n");
		}

	}

	public static void formatPassiveSaveActions(final StringBuilder src, final Actions.Type type, final List<ActionEntry> actionList) {

		src.append("\n\t@Override\n");
		src.append("\tpublic boolean ");
		src.append(type.getMethod());
		src.append("(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {\n\n");
		src.append("\t\tboolean error = false;\n\n");

		for (final ActionEntry action : actionList) {

			if (action.runOnError()) {

				src.append("\t\terror |= ").append(action.getSource()).append(";\n");

			} else {

				src.append("\t\tif (!error) {\n");
				src.append("\t\t\terror |= ").append(action.getSource()).append(";\n");
				src.append("\t\t}\n");

			}
		}

		// don't forget super call
		src.append("\t\terror |= !super.");
		src.append(type.getMethod());
		src.append("(securityContext, errorBuffer);\n");

		src.append("\n\t\treturn !error;\n");
		src.append("\t}\n");

	}

	public static String getPropertyWithVariableReplacement(SecurityContext securityContext, final GraphObject entity, ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {

		return replaceVariables(securityContext, entity, renderContext, entity.getProperty(key));

	}

	public static String replaceVariables(final SecurityContext securityContext, final GraphObject entity, final ActionContext actionContext, final Object rawValue) throws FrameworkException {

		String value = null;

		if (rawValue == null) {

			return null;

		}

		if (rawValue instanceof String) {

			value = (String) rawValue;

			if (!actionContext.returnRawValue(securityContext)) {

				// re-use matcher from previous calls
				Matcher matcher = threadLocalTemplateMatcher.get();

				matcher.reset(value);

				while (matcher.find()) {

					String group          = matcher.group();
					String source         = group.substring(2, group.length() - 1);
					Object extractedValue = Functions.evaluate(securityContext, actionContext, entity, source);

					if (extractedValue == null) {
						extractedValue = "";
					}

					String partValue = StringUtils.remove(extractedValue.toString(), "\\");
					if (partValue != null) {

						value = value.replace(group, partValue);

					} else {

						// If the whole expression should be replaced, and partValue is null
						// replace it by null to make it possible for HTML attributes to not be rendered
						// and avoid something like ... selected="" ... which is interpreted as selected==true by
						// all browsers
						value = value.equals(group) ? null : value.replace(group, "");
					}
				}

			}

		} else if (rawValue instanceof Boolean) {

			value = Boolean.toString((Boolean) rawValue);

		} else {

			value = rawValue.toString();

		}

		// return literal null
		if (Functions.NULL_STRING.equals(value)) {
			return null;
		}

		return value;

	}

	// ----- private methods -----
	private static PropertyParser getParserForRawValue(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String source, final boolean notNull, final String defaultValue) throws FrameworkException {

		for (final Map.Entry<Type, Class<? extends PropertyParser>> entry : parserMap.entrySet()) {

			if (source.startsWith(entry.getKey().name())) {

				try {

					final PropertyParser parser = entry.getValue().getConstructor(ErrorBuffer.class, String.class, String.class, String.class, String.class, String.class).newInstance(errorBuffer, className, propertyName, dbName, source, defaultValue);
					parser.setNotNull(notNull);

					return parser;

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}

		errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_property_definition", "Unknow value type " + source + ", options are " + Arrays.asList(Type.values()) + "."));
		throw new FrameworkException(422, errorBuffer);
	}
}
