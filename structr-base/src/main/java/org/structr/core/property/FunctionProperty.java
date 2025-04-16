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
package org.structr.core.property;

import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.openapi.common.OpenAPISchemaReference;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 */
public class FunctionProperty<T> extends Property<T> {

	private static final Logger logger             = LoggerFactory.getLogger(FunctionProperty.class.getName());
	private static final Map<String, String> cache = new ConcurrentHashMap<>();
	private static final BooleanProperty pBoolean  = new BooleanProperty("pBoolean");
	private static final IntProperty pInt          = new IntProperty("pInt");
	private static final LongProperty pLong        = new LongProperty("pLong");
	private static final DoubleProperty pDouble    = new DoubleProperty("pDouble");
	private static final DateProperty pDate        = new DateProperty("pDate");

	public FunctionProperty(final String name) {
		super(name);
	}

	public FunctionProperty(final String name, final String dbName) {
		super(name, dbName);
	}

	@Override
	public Property<T> indexed() {

		super.indexed();
		super.passivelyIndexed();

		return this;
	}

	@Override
	public Property<T> setSourceUuid(final String sourceUuid) {
		this.sourceUuid = sourceUuid;
		return this;
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject target, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		try {

			if (!securityContext.doInnerCallbacks()) {
				return null;
			}

			final GraphObject obj     = PropertyMap.unwrap(target);
			final String readFunction = getReadFunction();

			if (obj != null) {

				// ignore empty read function, don't log error message (it's not an error)
				if (readFunction != null) {

					if (cachingEnabled) {

						Object cachedValue = securityContext.getContextStore().retrieveFunctionPropertyResult(obj.getUuid(), jsonName);

						if (cachedValue != null) {
							return (T) cachedValue;
						}
					}

					final ActionContext actionContext = new ActionContext(securityContext);

					// don't ignore predicate
					actionContext.setPredicate(predicate);

					Object result = Scripting.evaluate(actionContext, obj, "${".concat(readFunction.trim()).concat("}"), "getProperty(" + jsonName + ")", sourceUuid, true);

					PropertyConverter converter = null;

					if (typeHint != null && result != null) {

						switch (typeHint.toLowerCase()) {

							case "boolean":
								converter = pBoolean.inputConverter(securityContext);
								break;
							case "int":
								converter = pInt.inputConverter(securityContext);
								break;
							case "long":
								converter = pLong.inputConverter(securityContext);
								break;
							case "double":
								converter = pDouble.inputConverter(securityContext);
								break;
							case "date":
								converter = pDate.inputConverter(securityContext);
								break;
						}

						if (converter != null) {
							{
								try {

									Object convertedResult = converter.convert(result);
									if (convertedResult != null) {
										result = convertedResult;
									}
								} catch (FrameworkException ex) {

									logger.warn("Could not convert value of function property. Conversion type: :%s, Raw value: " + result.toString(), ex);
								}
							}
						}
					}

					securityContext.getContextStore().storeFunctionPropertyResult(obj.getUuid(), jsonName, result);

					return (T)result;
				}

			} else {

				logger.warn("Unable to evaluate function property {}, object was null.", jsonName());
			}

		} catch (Throwable t) {

			logger.warn("Exception while evaluating read function in Function property '" + jsonName() + "'", t);
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public Class valueType() {

		if (typeHint != null) {

			switch (typeHint.toLowerCase()) {

				case "boolean": return Boolean.class;
				case "string":  return String.class;
				case "int":     return Integer.class;
				case "long":    return Long.class;
				case "double":  return Double.class;
				case "date":    return Date.class;
			}
		}

		return Object.class;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return value;
	}

	@Override
	public String typeName() {
		return valueType().getSimpleName();
	}


	private PropertyConverter getDatabaseConverter(final SecurityContext securityContext) {

		if (typeHint != null) {

			PropertyConverter converter = null;

			switch (typeHint.toLowerCase()) {

				case "boolean": converter = pBoolean.databaseConverter(securityContext); break;
				case "int":     converter = pInt.databaseConverter(securityContext); break;
				case "long":    converter = pLong.databaseConverter(securityContext); break;
				case "double":  converter = pDouble.databaseConverter(securityContext); break;
				case "date":    converter = pDate.databaseConverter(securityContext); break;
			}

			return converter;
		}

		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return getDatabaseConverter(securityContext);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return getDatabaseConverter(securityContext);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return getDatabaseConverter(securityContext);
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject target, final T value) throws FrameworkException {

		final ActionContext ctx = new ActionContext(securityContext);
		final GraphObject obj   = PropertyMap.unwrap(target);
		final String func       = getWriteFunction();
		T result                = null;

		if (func != null) {

			try {

				if (!securityContext.doInnerCallbacks()) {
					return null;
				}

				ctx.setConstant("value", value);

				result = (T)Scripting.evaluate(ctx, obj, "${".concat(func.trim()).concat("}"), "setProperty(" + jsonName + ")", sourceUuid, true);

			} catch (FrameworkException fex) {

				// catch and re-throw FrameworkExceptions
				throw fex;

			} catch (Throwable t) {

				logger.warn("Exception while evaluating write function in Function property \"{}\": {}", jsonName(), t.getMessage());
			}
		}

		if (ctx.hasError()) {

			throw new FrameworkException(422, "Server-side scripting error", ctx.getErrorBuffer());
		}

		return result;
	}

	@Override
	public Property<T> format(final String format) {
		this.readFunction = format;
		return this;
	}

	@Override
	public T convertSearchValue(final SecurityContext securityContext, final String requestParameter) throws FrameworkException {

		if (typeHint != null) {

			PropertyConverter converter = null;

			switch (typeHint.toLowerCase()) {

				case "boolean": converter = pBoolean.inputConverter(securityContext); break;
				case "int":     converter = pInt.inputConverter(securityContext); break;
				case "long":    converter = pLong.inputConverter(securityContext); break;
				case "double":  converter = pDouble.inputConverter(securityContext); break;
				case "date":    converter = pDate.inputConverter(securityContext); break;
			}

			if (converter != null) {

				return (T)converter.convert(requestParameter);
			}
		}

		// fallback
		return super.convertSearchValue(securityContext, requestParameter);
	}

	// ----- private methods -----
	private String getReadFunction() throws FrameworkException {
		return getCachedSourceCode(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), this.readFunction);
	}

	private String getWriteFunction() throws FrameworkException {
		return getCachedSourceCode(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY), this.writeFunction);
	}

	private String getOpenAPIReturnType() throws FrameworkException {
		return getCachedSourceCode(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.OPEN_API_RETURN_TYPE_PROPERTY), this.openAPIReturnType);
	}

	public String getCachedSourceCode(final PropertyKey<String> key, final String defaultValue) throws FrameworkException {

		final String cacheKey = sourceUuid + "." + key.jsonName();
		final String src      = cache.get(cacheKey);

		if (src == null) {

			final NodeInterface property = getCodeSource();
			if (property != null) {

				final String value = property.getProperty(key);
				if (value != null) {

					cache.put(cacheKey, value);

					return value;
				}
			}

		} else {

			return src;
		}

		return defaultValue;
	}

	public static void clearCache() {
		cache.clear();
	}

	public NodeInterface getCodeSource() throws FrameworkException {
		return StructrApp.getInstance().getNodeById(StructrTraits.SCHEMA_PROPERTY, sourceUuid);
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {

		if (typeHint != null) {

			switch (typeHint.toLowerCase()) {

				case "boolean": return pBoolean.getExampleValue(type, viewName);
				case "int":     return pInt.getExampleValue(type, viewName);
				case "long":    return pLong.getExampleValue(type, viewName);
				case "double":  return pDouble.getExampleValue(type, viewName);
				case "date":    return pDate.getExampleValue(type, viewName);
			}
		}

		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(final String type, final String viewName) {
		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Map<String, Object> schemaFromJsonString = new LinkedHashMap<>();
			final String returnType = getOpenAPIReturnType();
			if (returnType != null) {
				schemaFromJsonString.putAll(new GsonBuilder().create().fromJson(returnType, Map.class));
			}

			tx.success();

			return schemaFromJsonString;

		} catch (FrameworkException e) {

			logger.warn("Unable to create schema output for openAPIReturnType {}", e.getMessage());
		}

		return Collections.EMPTY_MAP;
	}


	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level)  {

		if (typeHint != null) {

			switch (typeHint.toLowerCase()) {

				case "boolean":
					return pBoolean.describeOpenAPIOutputType(type, viewName, level + 1);
				case "int":
					return pInt.describeOpenAPIOutputType(type, viewName, level + 1);
				case "long":
					return pLong.describeOpenAPIOutputType(type, viewName, level + 1);
				case "double":
					return pDouble.describeOpenAPIOutputType(type, viewName, level + 1);
				case "date":
					return pDate.describeOpenAPIOutputType(type, viewName, level + 1);
			}
		} else {

			return new OpenAPISchemaReference(type + "." + this.dbName + "PropertySchema");

		}

		return Collections.EMPTY_MAP;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		if (typeHint != null) {

			switch (typeHint.toLowerCase()) {

				case "boolean": return pBoolean.describeOpenAPIInputType(type, viewName, level + 1);
				case "int":     return pInt.describeOpenAPIInputType(type, viewName, level + 1);
				case "long":    return pLong.describeOpenAPIInputType(type, viewName, level + 1);
				case "double":  return pDouble.describeOpenAPIInputType(type, viewName, level + 1);
				case "date":    return pDate.describeOpenAPIInputType(type, viewName, level + 1);
			}
		}

		return Collections.EMPTY_MAP;
	}
}