/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.api.schema;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 *
 *
 */
public interface JsonType extends Comparable<JsonType> {

	URI getId();
	JsonSchema getSchema();

	String getName();
	JsonType setName(final String name);

	String getCategory();
	JsonType setCategory(final String category);

	String getIcon();
	JsonType setIcon(final String icon);

	boolean isAbstract();
	JsonType setIsAbstract();

	boolean isInterface();
	JsonType setIsInterface();

	boolean isServiceClass();
	JsonType setIsServiceClass();

	boolean isChangelogDisabled();
	JsonType setIsChangelogDisabled();

	JsonType setVisibleForPublicUsers();
	JsonType setVisibleForAuthenticatedUsers();

	JsonMethod addMethod(final String name, final String source);

	JsonType addTrait(final String name);

	Set<JsonProperty> getProperties();
	Set<String> getRequiredProperties();
	Set<String> getViewNames();
	Set<String> getViewPropertyNames(final String viewName);
	List<JsonMethod> getMethods();
	List<JsonGrant> getGrants();

	Set<String> getTags();
	void addTags(final String... tags);

	String getSummary();
	JsonType setSummary(final String summary);

	String getDescription();
	JsonType setDescription(final String description);

	boolean includeInOpenAPI();
	JsonType setIncludeInOpenAPI(final boolean includeInOpenAPI);

	JsonType addViewProperty(final String viewName, final String propertyName);

	JsonStringProperty addStringProperty(final String name, final String... views);
	JsonStringProperty addEncryptedProperty(final String name, final String... views);
	JsonDateProperty addDateProperty(final String name, final String... views);
	JsonIntegerProperty addIntegerProperty(final String name, final String... views);
	JsonLongProperty addLongProperty(final String name, final String... views);
	JsonDoubleProperty addDoubleProperty(final String name, final String... views);
	JsonBooleanProperty addBooleanProperty(final String name, final String... views);
	JsonFunctionProperty addFunctionProperty(final String name, final String...views);
	JsonEnumProperty addEnumProperty(final String name, final String...views);
	JsonDynamicProperty addCustomProperty(final String name, final String fqcn, final String... views);
	JsonStringArrayProperty addStringArrayProperty(final String name, final String... views);
	JsonDateArrayProperty addDateArrayProperty(final String name, final String... views);
	JsonIntegerArrayProperty addIntegerArrayProperty(final String name, final String... views);
	JsonLongArrayProperty addLongArrayProperty(final String name, final String... views);
	JsonDoubleArrayProperty addDoubleArrayProperty(final String name, final String... views);
	JsonBooleanArrayProperty addBooleanArrayProperty(final String name, final String... views);
	JsonByteArrayProperty addByteArrayProperty(final String name, final String... views);

	JsonReferenceProperty addReferenceProperty(final String name, final JsonReferenceProperty referencedProperty, final String... views);
}
