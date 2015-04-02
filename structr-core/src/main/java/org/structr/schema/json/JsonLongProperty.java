package org.structr.schema.json;

/**
 *
 * @author Christian Morgner
 */
public interface JsonLongProperty extends JsonProperty {

	public Long getMinimum();
	public Long getMaximum();

	public boolean isExclusiveMinimum();
	public boolean isExclusiveMaximum();

	public JsonLongProperty setExclusiveMinimum(final boolean exclusiveMinimum);
	public JsonLongProperty setExclusiveMaximum(final boolean exclusiveMaximum);

	public JsonLongProperty setMinimum(final long minimum);
	public JsonLongProperty setMinimum(final long minimum, final boolean exclusive);
	public JsonLongProperty setMaximum(final long maximum);
	public JsonLongProperty setMaximum(final long maximum, final boolean exclusive);
}