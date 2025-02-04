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

import org.structr.api.schema.JsonFunctionProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaHelper.Type;

import java.util.Map;

/**
 *
 *
 */
public class StructrFunctionProperty extends StructrDynamicProperty implements JsonFunctionProperty {

	protected Boolean cachingEnabled	= false;
	protected String readFunction  		= null;
	protected String writeFunction 		= null;
	protected String contentType   		= null;
	protected String openAPIReturnType	= null;

	public StructrFunctionProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);
	}

	@Override
	public JsonFunctionProperty setReadFunction(final String readFunction) {

		this.readFunction = readFunction;
		return this;
	}

	@Override
	public String getType() {
		return "function";
	}

	@Override
	public String getReadFunction() {
		return readFunction;
	}

	@Override
	public JsonFunctionProperty setWriteFunction(String writeFunction) {

		this.writeFunction = writeFunction;
		return this;
	}

	@Override
	public String getWriteFunction() {
		return writeFunction;
	}

	@Override
	public JsonFunctionProperty setIsCachingEnabled(boolean enabled) {

		this.cachingEnabled = enabled;
		return this;
	}

	@Override
	public Boolean getIsCachingEnabled() {
		return this.cachingEnabled;
	}

	@Override
	public JsonFunctionProperty setOpenAPIReturnType(String openAPIReturnType) {
		this.openAPIReturnType = openAPIReturnType;
		return this;
	}

	@Override
	public String getOpenAPIReturnType() {
		return this.openAPIReturnType;
	}

	@Override
	public JsonFunctionProperty setContentType(String contentType) {

		this.contentType = contentType;
		return this;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		map.put(JsonSchema.KEY_IS_CACHING_ENABLED, cachingEnabled);

		if (readFunction != null) {
			map.put(JsonSchema.KEY_READ_FUNCTION, readFunction);
		}

		if (writeFunction != null) {
			map.put(JsonSchema.KEY_WRITE_FUNCTION, writeFunction);
		}

		if (openAPIReturnType != null) {
			map.put(JsonSchema.KEY_OPENAPI_RETURN_TYPE, openAPIReturnType);
		}

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object readFunctionValue = source.get(JsonSchema.KEY_READ_FUNCTION);
		if (readFunctionValue != null) {

			if (readFunctionValue instanceof String) {

				this.readFunction = (String)readFunctionValue;

			} else {

				throw new IllegalStateException("Invalid readFunction for property " + name + ", expected string.");
			}
		}

		final Object openAPIReturnTypeValue = source.get(JsonSchema.KEY_OPENAPI_RETURN_TYPE);
		if (openAPIReturnTypeValue != null) {

			if (openAPIReturnTypeValue instanceof String) {

				this.openAPIReturnType = (String)openAPIReturnTypeValue;

			} else {

				throw new IllegalStateException("Invalid openAPIReturnType for property " + name + ", expected string.");
			}
		}

		final Object writeFunctionValue = source.get(JsonSchema.KEY_WRITE_FUNCTION);
		if (writeFunctionValue != null) {

			if (writeFunctionValue instanceof String) {

				this.writeFunction = (String)writeFunctionValue;

			} else {

				throw new IllegalStateException("Invalid writeFunction for property " + name + ", expected string.");
			}
		}

		final Object cachingEnabledValue = source.get(JsonSchema.KEY_IS_CACHING_ENABLED);
		if (cachingEnabledValue != null) {

			if (cachingEnabledValue instanceof String) {

				this.cachingEnabled = Boolean.valueOf((String)cachingEnabledValue);
			} else if (cachingEnabledValue instanceof Boolean) {

				this.cachingEnabled = (Boolean)cachingEnabledValue;
			}

		}

		final Object contentTypeValue = source.get(JsonSchema.KEY_CONTENT_TYPE);
		if (contentTypeValue != null) {

			if (contentTypeValue instanceof String) {

				this.contentType = (String)contentTypeValue;

			} else {

				throw new IllegalStateException("Invalid contentType for property " + name + ", expected string.");
			}
		}
	}

	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty property) {

		super.deserialize(schemaNodes, property);

		setReadFunction(property.getReadFunction());
		setWriteFunction(property.getWriteFunction());
		setIsCachingEnabled(property.isCachingEnabled());
		setContentType(property.getSourceContentType());
		setOpenAPIReturnType(property.getOpenAPIReturnType());
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final Traits traits           = Traits.of("SchemaProperty");
		final PropertyMap properties  = new PropertyMap();

		properties.put(traits.key("readFunction"),  readFunction);
		properties.put(traits.key("writeFunction"), writeFunction);
		properties.put(traits.key("isCachingEnabled"), cachingEnabled);

		property.setProperties(SecurityContext.getSuperUserInstance(), properties);

		return property;
	}

	// ----- protected methods -----
	@Override
	protected Type getTypeToSerialize() {

		if (contentType != null) {

			switch (contentType) {

				case "application/x-structr-javascript":
				case "application/x-structr-script":
					return Type.Function;

				case "application/x-cypher":
					return Type.Cypher;

			}
		}

		return Type.Function;
	}
}
