package org.structr.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CaseHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import static org.structr.core.entity.AbstractSchemaNode.className;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.NodeAttribute;

/**
 *
 * @author Christian Morgner
 */
public class SchemaHelper {

	private static final Map<String, String> normalizedEntityNameCache = new LinkedHashMap<>();

	/**
	 * Tries to normalize (and singularize) the given string so that it resolves to
	 * an existing entity type.
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

			String[] names           = StringUtils.split(possibleEntityString, "/");

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
		
		// FIXME
		// third try: interface
//		if (type == null) {
//			type = reverseInterfaceMap.get(normalizedEntityName);
//
//		}

		// store type but only if it exists!
		if (type != null) {
			normalizedEntityNameCache.put(rawType, type.getSimpleName());
		}

		return type;
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer) {
		return SchemaService.reloadSchema(errorBuffer);
	}
	
	public static void createGrant(final String signature, final Long flags) {

		final App app = StructrApp.getInstance();
		try {

			app.beginTx();
			ResourceAccess grant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature).getFirst();
			long flagsValue      = 255;
			
			
			// set value from grant flags
			if (flags != null) {
				flagsValue = flags.longValue();
			}
			
			if (grant == null) {
				
				// create new grant
				app.create(ResourceAccess.class,
					new NodeAttribute(ResourceAccess.signature, signature),
					new NodeAttribute(ResourceAccess.flags, flagsValue)
				);
				
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
		
	}
	
	public static void removeGrant(final String signature) {

		
		final App app = StructrApp.getInstance();
		try {

			app.beginTx();
			
			// delete grant
			ResourceAccess grant = app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, signature).getFirst();
			if (grant != null) {
				
				app.delete(grant);
			}
			
			app.commitTx();

		} catch (Throwable t) {

			t.printStackTrace();

		} finally {

			app.finishTx();
		}
		
	}
}
