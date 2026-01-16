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

/**
 */
public interface JsonParameter extends Comparable<JsonParameter> {

	public URI getId();
	public JsonMethod getParent();

	public String getName();
	public JsonParameter setName(final String name);

	public String getType();
	public JsonParameter setType(final String type);

	public int getIndex();
	public JsonParameter setIndex(final int index);

	public String getDescription();
	public JsonParameter setDescription(final String descString);

	public String getExampleValue();
	public JsonParameter setExampleValue(final String exampleValue);
}
