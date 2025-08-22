/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.service.ServiceResult;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;
import org.structr.rest.resource.SchemaResource;
import org.structr.schema.parser.*;

import java.util.*;

/**
 */
public class SchemaHelper {

	private static final Map<String, String> normalizedEntityNameCache = new HashMap<>();
	private static final String WORD_SEPARATOR                         = "_";

	public static final Map<Type, PropertyGeneratorFactory> generatorMap = new TreeMap<>(new ReverseTypeComparator());
	public static final Map<Type, Integer> sortIndexMap                  = new LinkedHashMap<>();

	static {

		generatorMap.put(Type.ZonedDateTime, (e, t, p) -> new ZonedDateTimePropertyGenerator(e, t, p));
		generatorMap.put(Type.BooleanArray,  (e, t, p) -> new BooleanArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.IntegerArray,  (e, t, p) -> new IntegerArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.DoubleArray,   (e, t, p) -> new DoubleArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.StringArray,   (e, t, p) -> new StringArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.Encrypted,     (e, t, p) -> new EncryptedStringPropertyGenerator(e, t, p));
		generatorMap.put(Type.DateArray,     (e, t, p) -> new DateArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.EnumArray,     (e, t, p) -> new EnumArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.ByteArray,     (e, t, p) -> new ByteArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.LongArray,     (e, t, p) -> new LongArrayPropertyGenerator(e, t, p));
		generatorMap.put(Type.Function,      (e, t, p) -> new FunctionPropertyGenerator(e, t, p));
		generatorMap.put(Type.Password,      (e, t, p) -> new PasswordPropertyGenerator(e, t, p));
		generatorMap.put(Type.IdNotion,      (e, t, p) -> new IdNotionPropertyGenerator(e, t, p));
		generatorMap.put(Type.Boolean,       (e, t, p) -> new BooleanPropertyGenerator(e, t, p));
		generatorMap.put(Type.Integer,       (e, t, p) -> new IntegerPropertyGenerator(e, t, p));
		generatorMap.put(Type.String,        (e, t, p) -> new StringPropertyGenerator(e, t, p));
		generatorMap.put(Type.Double,        (e, t, p) -> new DoublePropertyGenerator(e, t, p));
		generatorMap.put(Type.Custom,        (e, t, p) -> new CustomPropertyGenerator(e, t, p));
		generatorMap.put(Type.Notion,        (e, t, p) -> new NotionPropertyGenerator(e, t, p));
		generatorMap.put(Type.Cypher,        (e, t, p) -> new CypherPropertyGenerator(e, t, p));
		generatorMap.put(Type.Long,          (e, t, p) -> new LongPropertyGenerator(e, t, p));
		generatorMap.put(Type.Enum,          (e, t, p) -> new EnumPropertyGenerator(e, t, p));
		generatorMap.put(Type.Date,          (e, t, p) -> new DatePropertyGenerator(e, t, p));
		generatorMap.put(Type.Count,         (e, t, p) -> new CountPropertyGenerator(e, t, p));
		generatorMap.put(Type.Join,          (e, t, p) -> new JoinPropertyGenerator(e, t, p));

		sortIndexMap.put(Type.ZonedDateTime,  0);
		sortIndexMap.put(Type.BooleanArray,   1);
		sortIndexMap.put(Type.IntegerArray,   2);
		sortIndexMap.put(Type.DoubleArray,    3);
		sortIndexMap.put(Type.StringArray,    4);
		sortIndexMap.put(Type.Encrypted,      5);
		sortIndexMap.put(Type.DateArray,      6);
		sortIndexMap.put(Type.EnumArray,      7);
		sortIndexMap.put(Type.LongArray,      8);
		sortIndexMap.put(Type.ByteArray,      9);
		sortIndexMap.put(Type.Password,      10);
		sortIndexMap.put(Type.Boolean,       11);
		sortIndexMap.put(Type.Integer,       12);
		sortIndexMap.put(Type.String,        13);
		sortIndexMap.put(Type.Double,        14);
		sortIndexMap.put(Type.Long,          15);
		sortIndexMap.put(Type.Enum,          16);
		sortIndexMap.put(Type.Date,          17);
		sortIndexMap.put(Type.Function,      18);
		sortIndexMap.put(Type.Cypher,        19);
		sortIndexMap.put(Type.Count,         20);
		sortIndexMap.put(Type.Custom,        21);
		sortIndexMap.put(Type.Join,          22);
		sortIndexMap.put(Type.IdNotion,      23);
		sortIndexMap.put(Type.Notion,        24);
	}

	public enum Type {
		String, StringArray, DateArray, ByteArray, LongArray, DoubleArray, IntegerArray, BooleanArray, Integer, Long, Double, Boolean, Enum, EnumArray, Date, ZonedDateTime, Count, Function, Notion, IdNotion, Cypher, Join, Thumbnail, Password, Custom, Encrypted;
	}

	/*
	 * Tries to normalize (and singularize) the given string so that it
	 * resolves to an existing entity type.
	 *
	 * @param possibleEntityString
	 * @return the normalized entity name in its singular form
	 */
	public static String normalizeEntityName(final String possibleEntityString) {

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

		return begin.concat(WORD_SEPARATOR).concat(lastWord);
	}

	public static String cleanPropertyName(final String propertyName) {

		if (propertyName != null) {
			return propertyName.replaceAll("[^\\w]+", "");
		}

		return null;
	}

	public static List<GraphObjectMap> getSchemaTypeInfo(final SecurityContext securityContext, final String type, final String propertyView) throws FrameworkException {

		final ConfigurationProvider config    = StructrApp.getConfiguration();
		final List<GraphObjectMap> resultList = new LinkedList<>();
		final Traits traits                   = Traits.of(type);
		final boolean isServiceClass          = traits.isServiceClass();

		if (traits != null) {

			if (propertyView != null) {

				for (final Map.Entry<String, Object> entry : getPropertiesForView(securityContext, traits, propertyView).entrySet()) {

					final GraphObjectMap property = new GraphObjectMap();

					for (final Map.Entry<String, Object> prop : ((Map<String, Object>) entry.getValue()).entrySet()) {

						property.setProperty(new GenericProperty(prop.getKey()), prop.getValue());
					}

					resultList.add(property);
				}

			} else {

				final GraphObjectMap schema = new GraphObjectMap();

				resultList.add(schema);

				final String url    = "/".concat(type);
				final boolean isRel = traits.isRelationshipType();

				schema.setProperty(SchemaResource.urlProperty, url);
				schema.setProperty(SchemaResource.typeProperty, type);
				schema.setProperty(SchemaResource.nameProperty, type);
				schema.setProperty(SchemaResource.classNameProperty, type);
				schema.setProperty(SchemaResource.traitsProperty, traits.getAllTraits());
				schema.setProperty(SchemaResource.isBuiltinProperty, traits.isBuiltinType());
				schema.setProperty(SchemaResource.isServiceClassProperty, traits.isServiceClass());
				schema.setProperty(SchemaResource.isRelProperty, isRel);
				schema.setProperty(SchemaResource.isAbstractProperty, traits.isAbstract());
				schema.setProperty(SchemaResource.isInterfaceProperty, traits.isInterface());
				schema.setProperty(SchemaResource.flagsProperty, SecurityContext.getResourceFlags(type));

				if (!isServiceClass) {

					final Set<String> propertyViews = new LinkedHashSet<>(traits.getViewNames());

					// list property sets for all views
					Map<String, Map<String, Object>> views = new TreeMap();
					schema.setProperty(SchemaResource.viewsProperty, views);

					for (final String view : propertyViews) {

						views.put(view, getPropertiesForView(securityContext, traits, view));
					}
				}

				if (isRel) {

					schema.setProperty(new GenericProperty("relInfo"), SchemaResource.relationToMap(config, traits.getRelation()));
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

		final Trait declaringTrait = property.getDeclaringTrait();
		if (declaringTrait != null) {

			map.put("declaringClass", declaringTrait.getLabel());
		}

		map.put("defaultValue", property.defaultValue());

		if (property instanceof StringProperty) {
			map.put("contentType", ((StringProperty) property).contentType());
		}

		map.put("format", property.format());
		map.put("readOnly", property.isReadOnly());
		map.put("system", property.isSystemInternal());
		map.put("indexed", property.isIndexed());
		map.put("indexedWhenEmpty", property.isIndexedWhenEmpty());
		map.put("compound", property.isCompound());
		map.put("unique", property.isUnique());
		map.put("notNull", property.isNotNull());
		map.put("dynamic", property.isDynamic());
		map.put("category", property.category());
		map.put("serializationDisabled", property.serializationDisabled());

		final String relatedType = property.relatedType();
		if (relatedType != null) {

			map.put("relatedType", relatedType);
			map.put("type",        relatedType);
			map.put("uiType",      relatedType + (property.isCollection() ? "[]" : ""));

		} else {

			map.put("type", property.typeName());
			map.put("uiType", property.typeName() + (property.isCollection() ? "[]" : ""));
		}

		map.put("isCollection", property.isCollection());

		//if (declaringClass != null && ("org.structr.dynamic".equals(declaringClass.getPackage().getName()))) {
		if (declaringTrait != null && property instanceof RelationProperty) {

			Relation relation = ((RelationProperty) property).getRelation();
			if (relation != null) {

				map.put("relationshipType", relation.name());
			}
		}

		return map;
	}

	public static Map<String, Object> getPropertiesForView(final SecurityContext securityContext, final Traits type, final String propertyView) throws FrameworkException {

		final Set<PropertyKey> properties              = new LinkedHashSet<>(type.getPropertyKeysForView(propertyView));
		final Map<String, Object> propertyConverterMap = new LinkedHashMap<>();

		for (PropertyKey property : properties) {

			propertyConverterMap.put(property.jsonName(), getPropertyInfo(securityContext, property));
		}

		return propertyConverterMap;
	}

	private static class ReverseTypeComparator implements Comparator<Type> {

		@Override
		public int compare(final Type o1, final Type o2) {
			return o2.name().compareTo(o1.name());
		}
	}

	public static ServiceResult reloadSchema(final ErrorBuffer errorBuffer, final String initiatedBySessionId, final boolean forceFullReload, final boolean notifyCluster) {
		return SchemaService.reloadSchema(errorBuffer, initiatedBySessionId, forceFullReload, notifyCluster);
	}

	public static PropertyGenerator getPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition propertyDefinition) throws FrameworkException {

		final String propertyName              = propertyDefinition.getPropertyName();
		final Type propertyType                = propertyDefinition.getPropertyType();
		final PropertyGeneratorFactory factory = generatorMap.get(propertyType);

		if (factory != null) {

			final PropertyGenerator generator = factory.newInstance(errorBuffer, className, propertyDefinition);
			if (generator != null) {

				return generator;
			}
		}

		errorBuffer.add(new InvalidPropertySchemaToken(StructrTraits.SCHEMA_PROPERTY, propertyName, propertyName, "invalid_property_definition", "Unknow value type " + propertyType + ", options are " + Arrays.asList(Type.values()) + "."));
		throw new FrameworkException(422, "Invalid property definition for property ‛" + propertyDefinition.getPropertyName() + "‛", errorBuffer);
	}
}
