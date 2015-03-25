package org.structr.schema.json;

import java.util.Set;
import org.structr.schema.json.JsonSchema.Direction;

/**
 *
 * @author Christian Morgner
 */

public interface JsonReferenceProperty extends JsonProperty {

	public String getReference();
	public JsonReferenceProperty setReference(final String ref);

	public Direction getDirection();
	public JsonReferenceProperty setDirection(final JsonSchema.Direction direction);

	public String getRelationship();
	public JsonReferenceProperty setRelationship(final String relationship);

	public Set<String> getProperties();
	public JsonReferenceProperty setProperties(final String... properties);

	public JsonSchema.Cascade getCascadingDelete();
	public JsonReferenceProperty setCascadingDelete(final JsonSchema.Cascade cascade);

	public JsonSchema.Cascade getCascadingCreate();
	public JsonReferenceProperty setCascadingCreate(final JsonSchema.Cascade cascade);
}
