/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonFunctionProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonScriptProperty;

/**
 *
 *
 */
public class StructrFunctionProperty extends StructrStringProperty implements JsonFunctionProperty {

	protected String readFunction  = null;
	protected String writeFunction = null;
	protected String contentType   = null;

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

		if (readFunction != null) {
			map.put(JsonSchema.KEY_READ_FUNCTION, readFunction);
		}

		if (writeFunction != null) {
			map.put(JsonSchema.KEY_WRITE_FUNCTION, writeFunction);
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

		} else {

			throw new IllegalStateException("Missing readFunction value for property " + name);
		}

		final Object writeFunctionValue = source.get(JsonSchema.KEY_WRITE_FUNCTION);
		if (writeFunctionValue != null) {

			if (writeFunctionValue instanceof String) {

				this.writeFunction = (String)writeFunctionValue;

			} else {

				throw new IllegalStateException("Invalid writeFunction for property " + name + ", expected string.");
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
	void deserialize(final SchemaProperty property) {

		super.deserialize(property);

		setReadFunction(property.getReadFunction());
		setWriteFunction(property.getWriteFunction());
		setContentType(property.getSourceContentType());
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final String contentType      = getContentType();

		if (contentType != null) {

			switch (contentType) {

				case "text/javascript":
				case "text/structrscript":
					property.setProperty(SchemaProperty.propertyType, Type.Function.name());
					break;

				case "text/cypher":
					property.setProperty(SchemaProperty.propertyType, Type.Cypher.name());

			}

		} else {

			// default
			property.setProperty(SchemaProperty.propertyType, Type.Function.name());
		}

		property.setProperty(SchemaProperty.readFunction,  readFunction);
		property.setProperty(SchemaProperty.writeFunction, writeFunction);

		return property;
	}
}
