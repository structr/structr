package org.structr.schema.json;

/**
 *
 * @author Christian Morgner
 */
public interface JsonObjectProperty extends JsonProperty {

	public String getReference();
	public JsonObjectProperty setReference(final String ref);

	public String getDirection();
	public JsonObjectProperty setDirection(final String direction);

	public String getRelationship();
	public JsonObjectProperty setRelationship(final String relationship);
}
