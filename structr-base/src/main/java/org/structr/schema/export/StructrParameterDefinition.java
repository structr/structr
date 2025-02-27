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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonParameter;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 *
 */
public class StructrParameterDefinition implements JsonParameter, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrParameterDefinition.class.getName());

	private JsonMethod parent   = null;
	private String description  = null;
	private String exampleValue = null;
	private String name         = null;
	private String type         = null;
	private int index           = 0;

	StructrParameterDefinition(final JsonMethod parent, final String name) {

		this.parent = parent;
		this.name   = name;
	}

	@Override
	public String toString() {
		return type + " " + name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrParameterDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public URI getId() {

		final URI parentId = parent.getId();
		if (parentId != null) {

			try {
				final URI containerURI = new URI(parentId.toString() + "/");
				return containerURI.resolve("properties/" + getName());

			} catch (URISyntaxException urex) {
				logger.warn("", urex);
			}
		}

		return null;
	}

	@Override
	public JsonMethod getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JsonParameter setName(String name) {

		this.name = name;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public JsonParameter setDescription(final String description) {

		this.description = description;
		return this;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public JsonParameter setType(final String type) {
		this.type = type;
		return this;
	}

	@Override
	public String getExampleValue() {
		return exampleValue;
	}

	@Override
	public JsonParameter setExampleValue(final String exampleValue) {
		this.exampleValue = exampleValue;
		return this;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public JsonParameter setIndex(final int index) {
		this.index = index;
		return this;
	}

	@Override
	public int compareTo(final JsonParameter o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {
		return null;
	}

	// ----- package methods -----
	SchemaMethodParameter createDatabaseSchema(final App app, final SchemaMethod schemaMethod, final int index) throws FrameworkException {

		final Traits traits             = Traits.of(StructrTraits.SCHEMA_METHOD_PARAMETER);
		SchemaMethodParameter parameter = schemaMethod.getSchemaMethodParameter(getName());

		if (parameter == null) {

			final PropertyMap getOrCreateProperties = new PropertyMap();

			getOrCreateProperties.put(traits.key("name"),         getName());
			getOrCreateProperties.put(traits.key("schemaMethod"), schemaMethod);

			parameter = app.create(StructrTraits.SCHEMA_METHOD_PARAMETER, getOrCreateProperties).as(SchemaMethodParameter.class);
		}

		final PropertyMap updateProperties = new PropertyMap();

		updateProperties.put(traits.key("parameterType"), type);
		updateProperties.put(traits.key("description"),   description);
		updateProperties.put(traits.key("exampleValue"),  exampleValue);
		updateProperties.put(traits.key("index"),         index);

		// update properties
		parameter.setProperties(SecurityContext.getSuperUserInstance(), updateProperties);

		// return modified property
		return parameter;
	}


	void deserialize(final Map<String, Object> source) {

		final Object _type = source.get(JsonSchema.KEY_PARAMETER_TYPE);
		if (_type != null && _type instanceof String) {

			this.type = (String)_type;
		}

		final Object _index = source.get(JsonSchema.KEY_PARAMETER_INDEX);
		if (_index != null && _index instanceof Number) {

			this.index = ((Number)_index).intValue();
		}

		final Object _description = source.get(JsonSchema.KEY_DESCRIPTION);
		if (_description != null && _description instanceof String) {

			this.description = (String)_description;
		}

		this.exampleValue = (String)source.get(JsonSchema.KEY_EXAMPLE_VALUE);
	}

	void deserialize(final SchemaMethodParameter method) {

		setName(method.getName());
		setType(method.getParameterType());
		setIndex(method.getIndex());
		setDescription(method.getDescription());
		setExampleValue(method.getExampleValue());
	}

	Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		map.put(JsonSchema.KEY_PARAMETER_TYPE, type);
		map.put(JsonSchema.KEY_PARAMETER_INDEX, index);
		map.put(JsonSchema.KEY_DESCRIPTION, description);
		map.put(JsonSchema.KEY_EXAMPLE_VALUE, exampleValue);

		return map;
	}

	void initializeReferences() {
	}

	// ----- static methods -----
	static StructrParameterDefinition deserialize(final StructrMethodDefinition parent, final String name, final Map<String, Object> source) {

		final StructrParameterDefinition newParameter = new StructrParameterDefinition(parent, name);

		newParameter.deserialize(source);

		return newParameter;
	}

	static StructrParameterDefinition deserialize(final StructrMethodDefinition parent, final SchemaMethodParameter parameter) {

		final StructrParameterDefinition newParameter = new StructrParameterDefinition(parent, parameter.getName());

		newParameter.deserialize(parameter);

		return newParameter;
	}
}
