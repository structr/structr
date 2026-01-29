/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.api.schema.JsonProperty;
import org.structr.api.schema.JsonReferenceProperty;
import org.structr.api.schema.JsonType;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 *
 */
public abstract class StructrReferenceProperty extends StructrPropertyDefinition implements JsonReferenceProperty {

	protected final Set<String> properties = new TreeSet<>();

	StructrReferenceProperty(final JsonType parent, final String name) {
		super(parent, name);
	}

	@Override
	public JsonReferenceProperty setProperties(String... propertyNames) {

		for (final String name : propertyNames) {
			properties.add(name);
		}

		return this;
	}

	@Override
	public Set<String> getProperties() {
		return properties;
	}

	@Override
	public int compareTo(final JsonProperty o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public String getFormat() {
		return format;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean isRequired() {
		return required;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public JsonProperty setFormat(String format) {

		this.format = format;
		return this;
	}

	@Override
	public JsonProperty setRequired(boolean isRequired) {

		this.required = isRequired;
		return this;
	}

	@Override
	public JsonProperty setUnique(boolean isUnique) {

		this.unique = isUnique;
		return this;
	}

	@Override
	public JsonProperty setDefaultValue(String defaultValue) {

		this.defaultValue = defaultValue;
		return this;
	}

	// ----- package methods -----
	@Override
	void initializeReferences() {
	}


	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty schemaProperty) {
		super.deserialize(schemaNodes, schemaProperty);
	}

}
