package org.structr.schema.json;

/**
 *
 * @author Christian Morgner
 */
public interface JsonIntegerProperty extends JsonProperty {

	public Integer getMinimum();
	public Integer getMaximum();

	public boolean isExclusiveMinimum();
	public boolean isExclusiveMaximum();

	public JsonIntegerProperty setExclusiveMinimum(final boolean exclusiveMinimum);
	public JsonIntegerProperty setExclusiveMaximum(final boolean exclusiveMaximum);

	public JsonIntegerProperty setMinimum(final int minimum);
	public JsonIntegerProperty setMinimum(final int minimum, final boolean exclusive);
	public JsonIntegerProperty setMaximum(final int maximum);
	public JsonIntegerProperty setMaximum(final int maximum, final boolean exclusive);
}