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

import org.structr.api.schema.JsonDoubleProperty;
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
import org.structr.schema.parser.DoublePropertyGenerator;

import java.util.Map;

/**
 *
 *
 */
public class StructrNumberProperty extends StructrPropertyDefinition implements JsonDoubleProperty {

	private boolean exclusiveMinimum = false;
	private boolean exclusiveMaximum = false;
	private Double minimum          = null;
	private Double maximum          = null;

	public StructrNumberProperty(final StructrTypeDefinition parent, final String name) {
		super(parent, name);
	}

	@Override
	public boolean isExclusiveMinimum() {
		return exclusiveMinimum;
	}

	@Override
	public JsonDoubleProperty setExclusiveMinimum(final boolean exclusiveMinimum) {

		this.exclusiveMinimum = exclusiveMinimum;
		return this;
	}

	@Override
	public boolean isExclusiveMaximum() {
		return exclusiveMaximum;
	}

	@Override
	public JsonDoubleProperty setExclusiveMaximum(final boolean exclusiveMaximum) {

		this.exclusiveMaximum = exclusiveMaximum;
		return this;
	}

	@Override
	public Double getMinimum() {
		return minimum;
	}

	@Override
	public JsonDoubleProperty setMinimum(final double minimum) {
		return setMinimum(minimum, false);
	}

	@Override
	public JsonDoubleProperty setMinimum(final double minimum, final boolean exclusive) {

		this.exclusiveMinimum = exclusive;
		this.minimum          = minimum;

		return this;
	}

	@Override
	public Double getMaximum() {
		return maximum;
	}

	@Override
	public JsonDoubleProperty setMaximum(final double maximum) {
		return this.setMaximum(maximum, false);
	}

	@Override
	public JsonDoubleProperty setMaximum(final double maximum, final boolean exclusive) {

		this.exclusiveMaximum = exclusive;
		this.maximum          = maximum;

		return this;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		if (exclusiveMinimum) {
			map.put(JsonSchema.KEY_EXCLUSIVE_MINIMUM, true);
		}

		if (exclusiveMaximum) {
			map.put(JsonSchema.KEY_EXCLUSIVE_MAXIMUM, true);
		}

		if (minimum != null) {
			map.put(JsonSchema.KEY_MINIMUM, minimum);
		}

		if (maximum != null) {
			map.put(JsonSchema.KEY_MAXIMUM, maximum);
		}

		return map;
	}


	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object _exclusiveMinimum = source.get(JsonSchema.KEY_EXCLUSIVE_MINIMUM);
		if (_exclusiveMinimum != null && Boolean.TRUE.equals(_exclusiveMinimum)) {
			this.exclusiveMinimum = true;
		}

		final Object _exclusiveMaximum = source.get(JsonSchema.KEY_EXCLUSIVE_MAXIMUM);
		if (_exclusiveMaximum != null && Boolean.TRUE.equals(_exclusiveMaximum)) {
			this.exclusiveMaximum = true;
		}

		final Object _minimum = source.get(JsonSchema.KEY_MINIMUM);
		if (_minimum != null && _minimum instanceof Number) {
			this.minimum = ((Number)_minimum).doubleValue();
		}

		final Object _maximum = source.get(JsonSchema.KEY_MAXIMUM);
		if (_maximum != null && _maximum instanceof Number) {
			this.maximum = ((Number)_maximum).doubleValue();
		}
	}

	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty property) {

		super.deserialize(schemaNodes, property);

		final DoublePropertyGenerator doublePropertyParser = property.getDoublePropertyParser();
		if (doublePropertyParser != null) {

			this.exclusiveMinimum = doublePropertyParser.isLowerExclusive();
			this.exclusiveMaximum = doublePropertyParser.isUpperExclusive();

			final Number min = doublePropertyParser.getLowerBound();
			if (min != null) {
				this.minimum = min.doubleValue();
			}

			final Number max = doublePropertyParser.getUpperBound();
			if (max != null) {
				this.maximum = max.doubleValue();
			}
		}
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final Traits traits           = Traits.of("SchemaProperty");
		final PropertyMap properties  = new PropertyMap();

		properties.put(traits.key("propertyType"), Type.Double.name());

		if (minimum != null && maximum != null) {

			final StringBuilder range = new StringBuilder();

			if (exclusiveMinimum) {
				range.append("]");
			} else {
				range.append("[");
			}

			range.append(minimum);
			range.append(",");
			range.append(maximum);

			if (exclusiveMaximum) {
				range.append("[");
			} else {
				range.append("]");
			}

			properties.put(traits.key("format"), range.toString());
		}

		property.getWrappedNode().setProperties(SecurityContext.getSuperUserInstance(), properties);

		return property;
	}

	@Override
	public String getType() {
		return "number";
	}
}
