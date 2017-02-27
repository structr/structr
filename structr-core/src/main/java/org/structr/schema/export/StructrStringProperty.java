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
package org.structr.schema.export;

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonStringProperty;

/**
 *
 *
 */
public class StructrStringProperty extends StructrPropertyDefinition implements JsonStringProperty {

	private String contentType;
	
	public StructrStringProperty(final StructrTypeDefinition parent, final String name) {
		super(parent, name);
	}

	@Override
	public String getType() {
		return "string";
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		if (source.containsKey(JsonSchema.KEY_CONTENT_TYPE)) {
			this.contentType = (String) source.get(JsonSchema.KEY_CONTENT_TYPE);
		}

		if (source.containsKey(JsonSchema.KEY_FORMAT)) {
			this.format = (String) source.get(JsonSchema.KEY_FORMAT);
		}
	}

	@Override
	void deserialize(final SchemaProperty property) {
		
		super.deserialize(property);
		
		setFormat(property.getFormat());
		setContentType(property.getContentType());
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		if (contentType != null) {
			map.put(JsonSchema.KEY_CONTENT_TYPE, contentType);
		}

		if (format != null) {
			map.put(JsonSchema.KEY_FORMAT, format);
		}

		return map;
	}
	
	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);

		property.setProperty(SchemaProperty.propertyType, Type.String.name());
		property.setProperty(SchemaProperty.format, getFormat());
		property.setProperty(SchemaProperty.contentType, getContentType());

		return property;
	}

	@Override
	public JsonStringProperty setContentType(final String contentType) {

		this.contentType = contentType;

		return this;
	}

	@Override
	public String getContentType() {
		return contentType;
	}
}
