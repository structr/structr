package org.structr.schema.json;

import java.net.URI;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.schema.json.JsonSchema.Cascade;

/**
 *
 * @author Christian Morgner
 */
public interface JsonReferenceType extends JsonType {

	public JsonReferenceType setRelationship(final String relationship);
	public String getRelationship();

	public JsonReferenceType setCardinality(final Cardinality cardinality);
	public Cardinality getCardinality();

	public Cascade getCascadingDelete();
	public Cascade getCascadingCreate();

	public JsonReferenceType setCascadingDelete(final Cascade cascade);
	public JsonReferenceType setCascadingCreate(final Cascade cascade);

	public URI getSourceType();
	public URI getTargetType();

	public String getSourcePropertyName();
	public String getTargetPropertyName();

	public JsonReferenceType setSourcePropertyName(final String sourcePropertyName);
	public JsonReferenceType setTargetPropertyName(final String targetPropertyName);

	public JsonReferenceProperty getSourceProperty();
	public JsonReferenceProperty getTargetProperty();
}
