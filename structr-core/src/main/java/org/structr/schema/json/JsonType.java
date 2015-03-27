package org.structr.schema.json;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import org.structr.core.entity.Relation.Cardinality;

/**
 *
 * @author Christian Morgner
 */
public interface JsonType extends Comparable<JsonType> {

	public URI getId();

	public String getName();
	public JsonType setName(final String name);

	public JsonType addMethod(final String name, final String source);
	public JsonType setExtends(final JsonType superType);
	public JsonType setExtends(final URI externalReference);
	public String getExtends();

	public Map<String, JsonProperty> getProperties();
	public Map<String, Set<String>> getViews();
	public Map<String, String> getMethods();
	public Set<String> getRequiredProperties();

	public JsonStringProperty addStringProperty(final String name, final String... views) throws URISyntaxException;
	public JsonStringProperty addDateProperty(final String name, final String... views) throws URISyntaxException;
	public JsonNumberProperty addNumberProperty(final String name, final String... views) throws URISyntaxException;
	public JsonBooleanProperty addBooleanProperty(final String name, final String... views) throws URISyntaxException;
	public JsonScriptProperty addScriptProperty(final String name, final String...views) throws URISyntaxException;
	public JsonEnumProperty addEnumProperty(final String name, final String...views) throws URISyntaxException;

	public JsonRelationship relate(final JsonType type, final String relationship, final Cardinality cardinality) throws URISyntaxException;
}
