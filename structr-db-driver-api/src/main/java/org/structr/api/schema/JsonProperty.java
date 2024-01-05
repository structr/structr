/*
 * Copyright (C) 2010-2024 Structr GmbH
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

	public URI getId();

	public JsonType getParent();

	public String getName();
	public String getType();
	public String getFormat();
	public String getHint();
	public String getCategory();

	public String getDefaultValue();

	public boolean isCompoundUnique();
	public boolean isRequired();
	public boolean isUnique();
	public boolean isIndexed();
	public boolean isReadOnly();
	public Set<String> getTransformators();
	public Set<String> getValidators();

	public JsonProperty setHint(final String hint);
	public JsonProperty setCategory(final String category);
	public JsonProperty setFormat(final String format);
	public JsonProperty setName(final String name);
	public JsonProperty setRequired(final boolean isRequired);
	public JsonProperty setCompound(final boolean isCompoundUnique);
	public JsonProperty setUnique(final boolean isUnique);
	public JsonProperty setIndexed(final boolean isIndexed);
	public JsonProperty setReadOnly(final boolean isReadOnly);
	public JsonProperty setDefaultValue(final String defaultValue);

	public JsonProperty addValidator(final String fqcn);
	public JsonProperty addTransformer(final String fqcn);
}
