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
package org.structr.schema.export;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.schema.JsonEnumProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.schema.SchemaHelper.Type;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class StructrEnumProperty extends StructrStringProperty implements JsonEnumProperty {

	protected Set<String> enums = new LinkedHashSet<>();

	public StructrEnumProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);
	}

	@Override
	public JsonEnumProperty setFormat(final String format) {

		super.setFormat(format);

		for (final String value : format.split("[, ]+")) {
			enums.add(value.trim());
		}

		return this;
	}

	@Override
	public Set<String> getEnums() {
		return enums;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		map.put(JsonSchema.KEY_ENUM, enums);
		map.remove(JsonSchema.KEY_FORMAT);

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final List<String> enumValues = getListOrNull(source.get(JsonSchema.KEY_ENUM));

		if (enumValues != null && !enumValues.isEmpty()) {

			enums.addAll((List)enumValues);
		}
	}

	@Override
	public String getFormat() {
		return StringUtils.join(getEnums(), ", ");
	}

	// ----- protected methods -----
	@Override
	protected Type getTypeToSerialize() {
		return Type.Enum;
	}

	// ----- private methods -----
	private List<String> getListOrNull(final Object o) {

		if (o instanceof List) {

			return (List<String>)o;
		}

		return null;
	}
}
