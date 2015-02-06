/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Structr. If not, see <http://www.gnu.org/licenses/>.
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
import javatools.parsers.PlingStemmer;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.common.CaseHelper;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
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
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.ActionEntry;
import org.structr.schema.action.Actions;
import org.structr.schema.parser.BooleanPropertyParser;
import org.structr.schema.parser.CountPropertyParser;
import org.structr.schema.parser.CypherPropertyParser;
import org.structr.schema.parser.DatePropertyParser;
import org.structr.schema.parser.DoublePropertyParser;
import org.structr.schema.parser.EnumPropertyParser;
import org.structr.schema.parser.FunctionPropertyParser;
import org.structr.schema.parser.IntPropertyParser;
import org.structr.schema.parser.JoinPropertyParser;
import org.structr.schema.parser.LongPropertyParser;
import org.structr.schema.parser.NotionPropertyParser;
import org.structr.schema.parser.PropertyParameters;
import org.structr.schema.parser.PropertyParser;
import org.structr.schema.parser.StringArrayPropertyParser;
import org.structr.schema.parser.StringPropertyParser;
import org.structr.schema.parser.Validator;

/**
 *
 * @author Christian Morgner
 */
public class SchemaHelper {

	private static final String WORD_SEPARATOR = "_";

	public enum Type {

		String, StringArray, Integer, Long, Double, Boolean, Enum, Date, Count, Function, Notion, Cypher, Join
	}

	private static final Map<String, String> normalizedEntityNameCache = new LinkedHashMap<>();
	private static final Map<Type, Class<? extends PropertyParser>> parserMap = new LinkedHashMap<>();

	static {

		// IMPORTANT: parser map must be sorted by type name length
		//            because we look up the parsers using "startsWith"!
		parserMap.put(Type.StringArray, StringArrayPropertyParser.class);
		parserMap.put(Type.Function, FunctionPropertyParser.class);
		parserMap.put(Type.Boolean, BooleanPropertyParser.class);
		parserMap.put(Type.Integer, IntPropertyParser.class);
		parserMap.put(Type.String, StringPropertyParser.class);
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

		StringBuilder result = new StringBuilder();

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

		return type;
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer) {

		try {

			for (final SchemaNode schemaNode : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {

				createDynamicGrants(schemaNode.getResourceSignature());

			}

			for (final SchemaRelationship schemaRelationship : StructrApp.getInstance().relationshipQuery(SchemaRelationship.class).getAsList()) {

				createDynamicGrants(schemaRelationship.getResourceSignature());
				createDynamicGrants(schemaRelationship.getInverseResourceSignature());

			}

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return SchemaService.reloadSchema(errorBuffer);

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

				// create additional grant for the _schema resource
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, "_schema/" + signature),
					new NodeAttribute(DynamicResourceAccess.flags, initialFlagsValue)
				));

				// create additional grant for the Ui view
				grants.add(app.create(DynamicResourceAccess.class,
					new NodeAttribute(DynamicResourceAccess.signature, signature + "/_Ui"),
					new NodeAttribute(DynamicResourceAccess.flags, initialFlagsValue)
				));
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

	public static String extractProperties(final Schema entity, final Set<String> propertyNames, final Set<Validator> validators, final Set<String> enums, final Map<String, Set<String>> views, final Map<Actions.Type, List<ActionEntry>> actions, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyContainer propertyContainer = entity.getPropertyContainer();
		final StringBuilder src = new StringBuilder();

		// output property source code and collect views
		for (String propertyName : SchemaHelper.getProperties(propertyContainer)) {

			if (!propertyName.startsWith("__") && propertyContainer.hasProperty(propertyName)) {

				String rawType = propertyContainer.getProperty(propertyName).toString();

				PropertyParser parser = SchemaHelper.getParserForRawValue(errorBuffer, entity.getClassName(), propertyName, rawType);
				if (parser != null) {

					// add property name to set for later use
					propertyNames.add(parser.getPropertyName());

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

				final String value           = propertyContainer.getProperty(rawActionName).toString();
				final ActionEntry entry      = new ActionEntry(rawActionName, value);
				List<ActionEntry> actionList = actions.get(entry.getType());

				if (actionList == null) {

					actionList = new LinkedList<>();
					actions.put(entry.getType(), actionList);
				}

				actionList.add(entry);

				Collections.sort(actionList);
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
		src.append("import ").append(ConfigurationProvider.class.getName()).append(";\n");
		src.append("import ").append(GraphObjectComparator.class.getName()).append(";\n");
		src.append("import ").append(FrameworkException.class.getName()).append(";\n");
		src.append("import ").append(DatePropertyParser.class.getName()).append(";\n");
		src.append("import ").append(ValidationHelper.class.getName()).append(";\n");
		src.append("import ").append(SecurityContext.class.getName()).append(";\n");
		src.append("import ").append(GraphObject.class.getName()).append(";\n");
		src.append("import ").append(Actions.class.getName()).append(";\n");
		src.append("import ").append(PropertyView.class.getName()).append(";\n");
		src.append("import ").append(ErrorBuffer.class.getName()).append(";\n");
		src.append("import ").append(StructrApp.class.getName()).append(";\n");
		src.append("import ").append(Export.class.getName()).append(";\n");
		src.append("import ").append(View.class.getName()).append(";\n");
		src.append("import ").append(List.class.getName()).append(";\n");
		src.append("import ").append(Map.class.getName()).append(";\n");

		if (hasUiClasses()) {
			src.append("import org.structr.rest.RestMethodResult;\n");
		}

		src.append("import org.structr.core.validator.*;\n");
		src.append("import org.structr.core.property.*;\n");
		src.append("import org.structr.core.notion.*;\n");
		src.append("import org.structr.core.entity.*;\n\n");

	}

	public static String cleanPropertyName(final String propertyName) {
		return propertyName.replaceAll("[^\\w]+", "");
	}

	public static void formatValidators(final StringBuilder src, final Set<Validator> validators) {

		if (!validators.isEmpty()) {

			src.append("\n\t@Override\n");
			src.append("\tpublic boolean isValid(final ErrorBuffer errorBuffer) {\n\n");
			src.append("\t\tboolean error = !super.isValid(errorBuffer);\n\n");

			for (final Validator validator : validators) {
				src.append("\t\terror |= ").append(validator.getSource("this", true)).append(";\n");
			}

			src.append("\n\t\treturn !error;\n");
			src.append("\t}\n");
		}

	}

	public static void formatDynamicValidators(final StringBuilder src, final Set<Validator> validators) {

		if (!validators.isEmpty()) {

			src.append("\tpublic static boolean isValid(final AbstractNode obj, final ErrorBuffer errorBuffer) {\n\n");
			src.append("\t\tboolean error = false;\n\n");

			for (final Validator validator : validators) {
				src.append("\t\terror |= ").append(validator.getSource("obj", false)).append(";\n");
			}

			src.append("\n\t\treturn !error;\n");
			src.append("\t}\n\n");
		}
	}

	public static void formatSaveActions(final StringBuilder src, final Map<Actions.Type, List<ActionEntry>> saveActions) {

		// save actions..
		for (final Map.Entry<Actions.Type, List<ActionEntry>> entry : saveActions.entrySet()) {

			final List<ActionEntry> actionList = entry.getValue();
			final Actions.Type type = entry.getKey();

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

	public static void formatDynamicSaveActions(final StringBuilder src, final Map<Actions.Type, List<ActionEntry>> saveActions) {

		// save actions..
		for (final Map.Entry<Actions.Type, List<ActionEntry>> entry : saveActions.entrySet()) {

			final List<ActionEntry> actionList = entry.getValue();
			final Actions.Type type = entry.getKey();

			if (!actionList.isEmpty()) {

				switch (type) {

					case Custom:
						throw new UnsupportedOperationException("Active save actions are not supported for overridable types.");

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

	public static void formatPassiveSaveActions(final StringBuilder src, final Actions.Type type, final List<ActionEntry> actionList) {

		src.append("\n\t@Override\n");
		src.append("\tpublic boolean ");
		src.append(type.getMethod());
		src.append("(");
		src.append(type.getSignature());
		src.append(") throws FrameworkException {\n\n");
		src.append("\t\tboolean error = !super.");
		src.append(type.getMethod());
		src.append("(");
		src.append(type.getParameters());
		src.append(");\n\n");

		if (!actionList.isEmpty()) {

			for (final ActionEntry action : actionList) {

				src.append("\t\tif (!error) {\n");
				src.append("\t\t\terror |= ").append(action.getSource("this")).append(";\n");
				src.append("\t\t}\n");
			}
		}

		src.append("\n\t\treturn !error;\n");
		src.append("\t}\n");

	}

	public static void formatDynamicPassiveSaveActions(final StringBuilder src, final Actions.Type type, final List<ActionEntry> actionList) {

		src.append("\tpublic static boolean ");
		src.append(type.getMethod());
		src.append("(final AbstractNode obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {\n\n");
		src.append("\t\tboolean error = !obj.isValid(errorBuffer);\n\n");

		if (!actionList.isEmpty()) {

			for (final ActionEntry action : actionList) {

				src.append("\t\tif (!error) {\n");
				src.append("\t\t\terror |= ").append(action.getSource("obj")).append(";\n");
				src.append("\t\t}\n");
			}
		}

		src.append("\n\t\treturn !error;\n");
		src.append("\t}\n\n");

	}

	public static String getPropertyWithVariableReplacement(SecurityContext securityContext, final GraphObject entity, ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {

		return Scripting.replaceVariables(securityContext, entity, renderContext, entity.getProperty(key));

	}

	// ----- private methods -----
	private static PropertyParser getParserForRawValue(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String source) throws FrameworkException {

		final PropertyParameters params = PropertyParser.detectDbNameNotNullAndDefaultValue(source);

		for (final Map.Entry<Type, Class<? extends PropertyParser>> entry : parserMap.entrySet()) {

			if (params.source.startsWith(entry.getKey().name())) {

				try {

					final PropertyParser parser = entry.getValue().getConstructor(ErrorBuffer.class, String.class, String.class, PropertyParameters.class).newInstance(errorBuffer, className, propertyName, params);

					return parser;

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}

		errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_property_definition", "Unknow value type " + source + ", options are " + Arrays.asList(Type.values()) + "."));
		throw new FrameworkException(422, errorBuffer);
	}

	private static boolean hasUiClasses() {

		try {

			Class.forName("org.structr.rest.RestMethodResult");

			// success
			return true;

		} catch (Throwable t) {
		}

		return false;
	}
}
