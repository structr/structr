package org.structr.schema.export;

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonLongProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.parser.LongPropertyParser;

/**
 *
 * @author Christian Morgner
 */
public class StructrLongProperty extends StructrPropertyDefinition implements JsonLongProperty {

	private boolean exclusiveMinimum = false;
	private boolean exclusiveMaximum = false;
	private Long minimum          = null;
	private Long maximum          = null;

	public StructrLongProperty(final StructrTypeDefinition parent, final String name) {
		super(parent, name);
	}

	@Override
	public boolean isExclusiveMinimum() {
		return exclusiveMinimum;
	}

	@Override
	public JsonLongProperty setExclusiveMinimum(final boolean exclusiveMinimum) {

		this.exclusiveMinimum = exclusiveMinimum;
		return this;
	}

	@Override
	public boolean isExclusiveMaximum() {
		return exclusiveMaximum;
	}

	@Override
	public JsonLongProperty setExclusiveMaximum(final boolean exclusiveMaximum) {

		this.exclusiveMaximum = exclusiveMaximum;
		return this;
	}

	@Override
	public Long getMinimum() {
		return minimum;
	}

	@Override
	public JsonLongProperty setMinimum(final long minimum) {
		return setMinimum(minimum, false);
	}

	@Override
	public JsonLongProperty setMinimum(final long minimum, final boolean exclusive) {

		this.exclusiveMinimum = exclusive;
		this.minimum          = minimum;

		return this;
	}

	@Override
	public Long getMaximum() {
		return maximum;
	}

	@Override
	public JsonLongProperty setMaximum(final long maximum) {
		return this.setMaximum(maximum, false);
	}

	@Override
	public JsonLongProperty setMaximum(final long maximum, final boolean exclusive) {

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
			this.minimum = ((Number)_minimum).longValue();
		}

		final Object _maximum = source.get(JsonSchema.KEY_MAXIMUM);
		if (_maximum != null && _maximum instanceof Number) {
			this.maximum = ((Number)_maximum).longValue();
		}
	}

	@Override
	void deserialize(final SchemaProperty property) {

		super.deserialize(property);

		final LongPropertyParser longPropertyParser = property.getLongPropertyParser();
		if (longPropertyParser != null) {

			this.exclusiveMinimum = longPropertyParser.isLowerExclusive();
			this.exclusiveMaximum = longPropertyParser.isUpperExclusive();

			final Number min = longPropertyParser.getLowerBound();
			if (min != null) {
				this.minimum = min.longValue();
			}

			final Number max = longPropertyParser.getUpperBound();
			if (max != null) {
				this.maximum = max.longValue();
			}
		}
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);

		property.setProperty(SchemaProperty.propertyType, Type.Long.name());

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

			property.setProperty(SchemaProperty.format, range.toString());
		}

		return property;
	}

	@Override
	public String getType() {
		return "long";
	}
}
