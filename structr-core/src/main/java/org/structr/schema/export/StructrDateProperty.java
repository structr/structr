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
import org.structr.schema.json.JsonDateProperty;
import org.structr.schema.json.JsonSchema;

/**
 *
 *
 */
public class StructrDateProperty extends StructrStringProperty implements JsonDateProperty {

	private String datePattern = null;

	public StructrDateProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);

		setFormat("date-time");
	}

	// ----- public methods -----

	@Override
	public JsonDateProperty setDatePattern(final String datePattern) {

		this.datePattern = datePattern;
		return this;
	}

	@Override
	public String getDatePattern() {
		return datePattern;
	}


	// ----- package methods -----
	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		if (datePattern != null) {
			map.put(JsonSchema.KEY_DATE_PATTERN, datePattern);
		}

		return map;
	}


	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		if (source.containsKey(JsonSchema.KEY_DATE_PATTERN)) {
			this.datePattern = (String)source.get(JsonSchema.KEY_DATE_PATTERN);
		}
	}

	@Override
	void deserialize(final SchemaProperty property) {

		super.deserialize(property);

		this.datePattern = property.getProperty(SchemaProperty.format);
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);

		property.setProperty(SchemaProperty.propertyType, Type.Date.name());
		property.setProperty(SchemaProperty.format, datePattern);

		return property;
	}
}
