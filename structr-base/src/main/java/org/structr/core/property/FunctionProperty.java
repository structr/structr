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
package org.structr.core.property;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.config.ScriptConfig;
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

					final ScriptConfig scriptConfig = ScriptConfig.builder()
							.wrapJsInMain(this.readFunctionWrapJS())
							.build();

					Object result = Scripting.evaluate(actionContext, obj, "${".concat(readFunction.trim()).concat("}"), "getProperty(" + jsonName + ")", sourceUuid, scriptConfig);

					PropertyConverter converter = null;

					if (typeHint != null && result != null) {

						final Property tmp = createTempProperty(typeHint.toLowerCase(), jsonName);
						if (tmp != null) {

							converter = tmp.inputConverter(securityContext, false);
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

				logger.warn("Unable to evaluate function property '{}', object was null.", jsonName());
			}

		} catch (Throwable t) {

			final String message = "Exception while evaluating read function in Function property '" + jsonName() + "' for node " + target.getUuid();

			if (Settings.LogFunctionsStackTrace.getValue()) {

				logger.warn(message, t);

			} else {

				logger.warn(message + " (Stacktrace suppressed - see setting " + Settings.LogFunctionsStackTrace.getKey() + ")");
			}
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

			final Property tmp = createTempProperty(typeHint.toLowerCase(), jsonName);
			if (tmp != null) {

				converter = tmp.databaseConverter(securityContext);
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
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext, boolean fromString) {
		return getDatabaseConverter(securityContext);
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject target, final T value) throws FrameworkException {

		final ActionContext ctx = new ActionContext(securityContext);
		final GraphObject obj   = PropertyMap.unwrap(target);
		final String func       = getWriteFunction();
		T result                = null;

		if (StringUtils.isNotBlank(func)) {

			try {

				if (!securityContext.doInnerCallbacks()) {
					return null;
				}

				ctx.setConstant("value", value);

				final ScriptConfig scriptConfig = ScriptConfig.builder()
						.wrapJsInMain(this.writeFunctionWrapJS())
						.build();

				result = (T)Scripting.evaluate(ctx, obj, "${".concat(func.trim()).concat("}"), "setProperty(" + jsonName + ")", sourceUuid, scriptConfig);

			} catch (FrameworkException fex) {

				// catch and re-throw FrameworkExceptions
				throw fex;

			} catch (Throwable t) {

				logger.warn("Exception while evaluating write function in Function property \"{}\": {}", jsonName(), t.getMessage());
			}

		} else {

			logger.warn("FunctionProperty {} has empty write function, value will not be changed.", jsonName());
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

			final Property tmp = createTempProperty(typeHint.toLowerCase(), jsonName);
			if (tmp != null) {

				converter = tmp.inputConverter(securityContext, false);
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

			final Property tmp = createTempProperty(typeHint.toLowerCase(), jsonName);
			if (tmp != null) {

				return tmp.getExampleValue(type, viewName);
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

			final Property tmp = createTempProperty(typeHint.toLowerCase(), jsonName);
			if (tmp != null) {

				return tmp.describeOpenAPIOutputType(type, viewName, level + 1);
			}

		} else {

			return new OpenAPISchemaReference(type + "." + this.dbName + "PropertySchema");

		}

		return Collections.EMPTY_MAP;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		if (typeHint != null) {

			final Property tmp = createTempProperty(typeHint.toLowerCase(), jsonName);
			if (tmp != null) {

				return tmp.describeOpenAPIInputType(type, viewName, level + 1);
			}
		}

		return Collections.EMPTY_MAP;
	}

	// ----- private methods -----
	private Property createTempProperty(final String type, final String name) {

		Property tmp = null;

		switch (type) {
			case "boolean":
				tmp = new BooleanProperty(name);
				break;

			case "int":
				tmp = new IntProperty(name);
				break;

			case "long":
				tmp = new LongProperty(name);
				break;

			case "double":
				tmp = new DoubleProperty(name);
				break;

			case "date":
				tmp = new DateProperty(name);
				break;
		}

		if (tmp != null) {
			tmp.setDeclaringTrait(this.getDeclaringTrait());
		}

		return tmp;
	}
}