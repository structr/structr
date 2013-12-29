package org.structr.schema;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.common.CaseHelper;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.parser.BooleanPropertyParser;
import org.structr.schema.parser.CountPropertyParser;
import org.structr.schema.parser.DatePropertyParser;
import org.structr.schema.parser.DoublePropertyParser;
import org.structr.schema.parser.EnumPropertyParser;
import org.structr.schema.parser.IntPropertyParser;
import org.structr.schema.parser.LongPropertyParser;
import org.structr.schema.parser.PropertyParser;
import org.structr.schema.parser.StringPropertyParser;

/**
 *
 * @author Christian Morgner
 */
public class SchemaHelper {

	public enum Type {

		String, Integer, Long, Double, Boolean, Enum, Date, Count
	}

	private static final Map<String, String> normalizedEntityNameCache = new LinkedHashMap<>();
	private static final Map<Type, Class<? extends PropertyParser>> parserMap = new EnumMap<>(Type.class);

	static {

		parserMap.put(Type.String, StringPropertyParser.class);
		parserMap.put(Type.Integer, IntPropertyParser.class);
		parserMap.put(Type.Long, LongPropertyParser.class);
		parserMap.put(Type.Double, DoublePropertyParser.class);
		parserMap.put(Type.Boolean, BooleanPropertyParser.class);
		parserMap.put(Type.Enum, EnumPropertyParser.class);
		parserMap.put(Type.Date, DatePropertyParser.class);
		parserMap.put(Type.Count, CountPropertyParser.class);
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
		return SchemaService.reloadSchema(errorBuffer);
	}

	public static List<ResourceAccess> createGrants(final String signature, final Long flags) {

		List<ResourceAccess> grants = new LinkedList<>();

		final App app = StructrApp.getInstance();
		try {

			app.beginTx();
			ResourceAccess grant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature).getFirst();
			long flagsValue = 255;

			// set value from grant flags
			if (flags != null) {
				flagsValue = flags.longValue();
			}

			if (grant == null) {

				// create new grant
				grants.add(app.create(ResourceAccess.class,
					new NodeAttribute(ResourceAccess.signature, signature),
					new NodeAttribute(ResourceAccess.flags, flagsValue)
				));

				// create additional grant for the _schema resource
				grants.add(app.create(ResourceAccess.class,
					new NodeAttribute(ResourceAccess.signature, "_schema/" + signature),
					new NodeAttribute(ResourceAccess.flags, flagsValue)
				));

				// create additional grant for the Ui view
				grants.add(app.create(ResourceAccess.class,
					new NodeAttribute(ResourceAccess.signature, signature + "/_Ui"),
					new NodeAttribute(ResourceAccess.flags, flagsValue)
				));

			} else {

				// modify flags of grant
				// Caution: this means that the SchemaNode is the 
				// primary source for resource access flag values
				// of dynamic nodes
				grant.setProperty(ResourceAccess.flags, flagsValue);
			}

			app.commitTx();

		} catch (Throwable t) {

			t.printStackTrace();

		} finally {

			app.finishTx();
		}

		return grants;

	}

	public static void updateGrants(final List<ResourceAccess> grants, final String signature, final Long flags) {

		final App app = StructrApp.getInstance();
		
		long flagsValue = 255;

		// set value from grant flags
		if (flags != null) {
			flagsValue = flags.longValue();
		}
		
		try {
			app.beginTx();
			
			for (ResourceAccess grant : grants) {
				
				final String oldSignature = grant.getProperty(ResourceAccess.signature);
				
				if (oldSignature.startsWith("_schema/")) {
					
					grant.setProperty(ResourceAccess.signature, "_schema/" + signature);
					
				} else if (oldSignature.endsWith("/_Ui")) {
					
					grant.setProperty(ResourceAccess.signature, signature + "/_Ui");
					
				} else {
					
					grant.setProperty(ResourceAccess.signature, signature);
					
				}
				
				grant.setProperty(ResourceAccess.flags, flagsValue);
				
			}
			
			app.commitTx();

		} catch (Throwable t) {

			t.printStackTrace();

		} finally {

			app.finishTx();
		}

	}

	public static void removeGrants(final String signature) {

		final App app = StructrApp.getInstance();
		try {

			app.beginTx();

			// delete grant
			ResourceAccess grant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature).getFirst();
			if (grant != null) {

				app.delete(grant);
			}

			// delete grant
			ResourceAccess schemaGrant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, "_schema/" + signature).getFirst();
			if (schemaGrant != null) {

				app.delete(schemaGrant);
			}
			// delete grant
			ResourceAccess viewGrant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature + "/_Ui").getFirst();
			if (viewGrant != null) {

				app.delete(viewGrant);
			}

			app.commitTx();

		} catch (Throwable t) {

			t.printStackTrace();

		} finally {

			app.finishTx();
		}

	}

	public static String extractProperties(final Schema entity, final Set<String> validators, final Set<String> enums, final Map<String, Set<String>> views, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyContainer propertyContainer = entity.getPropertyContainer();
		final StringBuilder src = new StringBuilder();

		// output property source code and collect views
		for (final String propertyName : SchemaHelper.getProperties(propertyContainer)) {

			if (!propertyName.startsWith("__") && propertyContainer.hasProperty(propertyName)) {

				String rawType = propertyContainer.getProperty(propertyName).toString();
				boolean notNull = false;

				// detect and remove not-null constraint
				if (rawType.startsWith("+")) {
					rawType = rawType.substring(1);
					notNull = true;
				}

				PropertyParser parser = SchemaHelper.getParserForRawValue(errorBuffer, entity.getClassName(), propertyName, rawType, notNull);
				if (parser != null) {

					// append created source from parser
					src.append(parser.getPropertySource(errorBuffer));

					// register global elements created by parser
					validators.addAll(parser.getGlobalValidators());
					enums.addAll(parser.getEnumDefinitions());

					// register property in default view
					addPropertyToView(PropertyView.Public, propertyName.substring(1), views);
					addPropertyToView(PropertyView.Ui, propertyName.substring(1), views);
				}
			}
		}

		for (final String rawViewName : getViews(propertyContainer)) {

			if (propertyContainer.hasProperty(rawViewName)) {

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

		return src.toString();
	}

	public static void addPropertyToView(final String viewName, final String propertyName, final Map<String, Set<String>> views) {

		Set<String> view = views.get(viewName);
		if (view == null) {

			view = new LinkedHashSet<>();
			views.put(viewName, view);
		}

		view.add(propertyName + "Property");
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
		src.append("import ").append(PropertyView.class.getName()).append(";\n");
		src.append("import ").append(View.class.getName()).append(";\n");
		src.append("import ").append(ValidationHelper.class.getName()).append(";\n");
		src.append("import ").append(ErrorBuffer.class.getName()).append(";\n");
		src.append("import org.structr.core.validator.*;\n");
		src.append("import org.structr.core.property.*;\n");
		src.append("import org.structr.core.notion.*;\n");
		src.append("import org.structr.core.entity.*;\n");
		src.append("import java.util.Date;\n");
		src.append("import java.util.List;\n\n");

	}

	// ----- private methods -----
	private static PropertyParser getParserForRawValue(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String source, final boolean notNull) throws FrameworkException {

		for (final Map.Entry<Type, Class<? extends PropertyParser>> entry : parserMap.entrySet()) {

			if (source.startsWith(entry.getKey().name())) {

				try {

					final PropertyParser parser = entry.getValue().getConstructor(ErrorBuffer.class, String.class, String.class, String.class).newInstance(errorBuffer, className, propertyName, source);
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
