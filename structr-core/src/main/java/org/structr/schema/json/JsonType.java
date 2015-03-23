package org.structr.schema.json;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Christian Morgner
 */
public interface JsonType extends Comparable<JsonType> {

	public URI getId();

	public String getName();
	public JsonType setName(final String name);

	public Set<JsonProperty> getProperties();
	public Map<String, Set<String>> getViews();
	public Set<String> getRequiredProperties();

	public JsonStringProperty addStringProperty(final String name, final String... views) throws URISyntaxException;
	public JsonStringProperty addDateProperty(final String name, final String... views) throws URISyntaxException;
	public JsonNumberProperty addNumberProperty(final String name, final String... views) throws URISyntaxException;
	public JsonBooleanProperty addBooleanProperty(final String name, final String... views) throws URISyntaxException;
	public JsonScriptProperty addScriptProperty(final String name, final String...views) throws URISyntaxException;
	public JsonEnumProperty addEnumProperty(final String name, final String...views) throws URISyntaxException;

	public JsonObjectProperty addReference(final String name, final JsonType otherType, final String... views) throws URISyntaxException;
	public JsonObjectProperty addReference(final String name, final JsonObjectProperty otherProperty, final String... views) throws URISyntaxException;

	public JsonArrayProperty addArrayReference(final String name, final JsonType otherType, final String... views) throws URISyntaxException;
	public JsonArrayProperty addArrayReference(final String name, final JsonArrayProperty referencedProperty, final String... views) throws URISyntaxException;
}
