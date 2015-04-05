package org.structr.schema.json;

/**
 *
 * @author Christian Morgner
 */
public interface JsonNumberProperty extends JsonProperty {

	public Double getMinimum();
	public Double getMaximum();

	public boolean isExclusiveMinimum();
	public boolean isExclusiveMaximum();

	public JsonNumberProperty setExclusiveMinimum(final boolean exclusiveMinimum);
	public JsonNumberProperty setExclusiveMaximum(final boolean exclusiveMaximum);

	public JsonNumberProperty setMinimum(final double minimum);
	public JsonNumberProperty setMinimum(final double minimum, final boolean exclusive);
	public JsonNumberProperty setMaximum(final double maximum);
	public JsonNumberProperty setMaximum(final double maximum, final boolean exclusive);
}