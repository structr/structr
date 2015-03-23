package org.structr.schema.json;

import java.util.Set;

/**
 *
 * @author Christian Morgner
 */
public interface JsonArrayProperty extends JsonProperty {

	public String getReference();
	public JsonArrayProperty setReference(final String ref);

	public String getDirection();
	public JsonArrayProperty setDirection(final String direction);

	public String getRelationship();
	public JsonArrayProperty setRelationship(final String relationship);

	public Set<String> getProperties();
	public JsonArrayProperty setProperties(final String... properties);

}
