package org.structr.schema.json;

import java.net.URI;

/**
 *
 * @author Christian Morgner
 */
public interface JsonProperty extends Comparable<JsonProperty> {

	public URI getId();

	public String getName();
	public String getType();
	public String getFormat();

	public Object getDefaultValue();

	public boolean isRequired();
	public boolean isUnique();


	public JsonProperty setType(final String type);
	public JsonProperty setFormat(final String format);
	public JsonProperty setName(final String name);
	public JsonProperty setRequired(final boolean isRequired);
	public JsonProperty setUnique(final boolean isUnique);
	public JsonProperty setDefaultValue(final Object defaultValue);
}
