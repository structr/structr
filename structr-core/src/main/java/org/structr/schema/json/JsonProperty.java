package org.structr.schema.json;

import java.net.URI;

/**
 *
 * @author Christian Morgner
 */
public interface JsonProperty extends Comparable<JsonProperty> {

	public URI getId();

	public JsonType getParent();

	public String getName();
	public String getType();
	public String getFormat();

	public String getDefaultValue();

	public boolean isRequired();
	public boolean isUnique();

	public JsonProperty setFormat(final String format);
	public JsonProperty setName(final String name);
	public JsonProperty setRequired(final boolean isRequired);
	public JsonProperty setUnique(final boolean isUnique);
	public JsonProperty setDefaultValue(final String defaultValue);
}
