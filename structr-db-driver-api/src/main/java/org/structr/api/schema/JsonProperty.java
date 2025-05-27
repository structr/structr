/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import java.util.Set;

/**
 *
 *
 */
public interface JsonProperty extends Comparable<JsonProperty> {

	URI getId();

	JsonType getParent();

	String getName();
	String getType();
	String getFormat();
	String getHint();
	String getCategory();

	String getDefaultValue();

	boolean isCompoundUnique();
	boolean isRequired();
	boolean isUnique();
	boolean isIndexed();
	boolean isReadOnly();
	boolean isAbstract();
	boolean isSerializationDisabled();
	Set<String> getTransformators();
	Set<String> getValidators();

	JsonProperty setHint(final String hint);
	JsonProperty setCategory(final String category);
	JsonProperty setFormat(final String format);
	JsonProperty setName(final String name);
	JsonProperty setAbstract(final boolean isAbstractMethod);
	JsonProperty setRequired(final boolean isRequired);
	JsonProperty setCompound(final boolean isCompoundUnique);
	JsonProperty setUnique(final boolean isUnique);
	JsonProperty setIndexed(final boolean isIndexed);
	JsonProperty setReadOnly(final boolean isReadOnly);
	JsonProperty setSerializationDisabled(final boolean serializationDisabled);
	JsonProperty setDefaultValue(final String defaultValue);

	JsonProperty addValidator(final String fqcn);
	JsonProperty addTransformer(final String fqcn);

}
