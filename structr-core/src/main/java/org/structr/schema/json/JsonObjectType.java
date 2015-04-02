package org.structr.schema.json;

import java.net.URI;
import java.net.URISyntaxException;
import org.structr.core.entity.Relation;

/**
 *
 * @author Christian Morgner
 */
public interface JsonObjectType extends JsonType {

	public JsonReferenceType relate(final JsonObjectType type) throws URISyntaxException;
	public JsonReferenceType relate(final URI externalTypeReference) throws URISyntaxException;
	public JsonReferenceType relate(final JsonObjectType type, final String relationship) throws URISyntaxException;
	public JsonReferenceType relate(final URI externalTypeReference, final String relationship) throws URISyntaxException;
	public JsonReferenceType relate(final JsonObjectType type, final String relationship, final Relation.Cardinality cardinality) throws URISyntaxException;
	public JsonReferenceType relate(final URI externalTypeReference, final String relationship, final Relation.Cardinality cardinality) throws URISyntaxException;
	public JsonReferenceType relate(final JsonObjectType type, final String relationship, final Relation.Cardinality cardinality, final String sourceAttributeName, final String targetAttributeName) throws URISyntaxException;
	public JsonReferenceType relate(final URI externalTypeReference, final String relationship, final Relation.Cardinality cardinality, final String sourceAttributeName, final String targetAttributeName) throws URISyntaxException;
}
