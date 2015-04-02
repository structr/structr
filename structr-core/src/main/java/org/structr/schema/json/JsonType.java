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
	public JsonSchema getSchema();

	public String getName();
	public JsonType setName(final String name);

	public JsonType addMethod(final String name, final String source);
	public JsonType setExtends(final JsonType superType);
	public JsonType setExtends(final URI externalReference);
	public URI getExtends();

	public Set<JsonProperty> getProperties();
	public Set<String> getRequiredProperties();
	public Set<String> getViewNames();
	public Set<String> getViewPropertyNames(final String viewName);
	public Map<String, String> getMethods();

	public JsonType addViewProperty(final String viewName, final String propertyName);

	public JsonStringProperty addStringProperty(final String name, final String... views) throws URISyntaxException;
	public JsonStringProperty addDateProperty(final String name, final String... views) throws URISyntaxException;
	public JsonIntegerProperty addIntegerProperty(final String name, final String... views) throws URISyntaxException;
	public JsonLongProperty addLongProperty(final String name, final String... views) throws URISyntaxException;
	public JsonNumberProperty addNumberProperty(final String name, final String... views) throws URISyntaxException;
	public JsonBooleanProperty addBooleanProperty(final String name, final String... views) throws URISyntaxException;
	public JsonScriptProperty addScriptProperty(final String name, final String...views) throws URISyntaxException;
	public JsonEnumProperty addEnumProperty(final String name, final String...views) throws URISyntaxException;

	public JsonReferenceProperty addReferenceProperty(final String name, final JsonReferenceProperty referencedProperty, final String... views);
}
