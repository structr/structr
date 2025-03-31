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

import org.structr.api.schema.JsonDateProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.schema.SchemaHelper;

import java.util.Map;

public class StructrZonedDateTimeProperty extends StructrStringProperty implements JsonDateProperty {

	private String datePattern = null;

	public StructrZonedDateTimeProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);

		setFormat(JsonSchema.FORMAT_DATE_TIME);
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
			this.datePattern = (String) source.get(JsonSchema.KEY_DATE_PATTERN);
		}
	}

	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty property) {

		super.deserialize(schemaNodes, property);

		this.datePattern = property.getFormat();
	}

	@Override
	public String getFormat() {
		return datePattern;
	}

	// ----- protected methods -----
	@Override
	protected SchemaHelper.Type getTypeToSerialize() {
		return SchemaHelper.Type.ZonedDateTime;
	}
}
