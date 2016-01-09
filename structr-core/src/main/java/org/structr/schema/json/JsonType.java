/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.json;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 *
 *
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
	public JsonDateProperty addDateProperty(final String name, final String... views) throws URISyntaxException;
	public JsonIntegerProperty addIntegerProperty(final String name, final String... views) throws URISyntaxException;
	public JsonLongProperty addLongProperty(final String name, final String... views) throws URISyntaxException;
	public JsonNumberProperty addNumberProperty(final String name, final String... views) throws URISyntaxException;
	public JsonBooleanProperty addBooleanProperty(final String name, final String... views) throws URISyntaxException;
	public JsonScriptProperty addScriptProperty(final String name, final String...views) throws URISyntaxException;
	public JsonFunctionProperty addFunctionProperty(final String name, final String...views) throws URISyntaxException;
	public JsonEnumProperty addEnumProperty(final String name, final String...views) throws URISyntaxException;
	public JsonStringArrayProperty addStringArrayProperty(final String name, final String... views) throws URISyntaxException;

	public JsonReferenceProperty addReferenceProperty(final String name, final JsonReferenceProperty referencedProperty, final String... views);
}
