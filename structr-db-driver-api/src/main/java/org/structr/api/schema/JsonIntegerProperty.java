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

/**
 *
 *
 */
public interface JsonIntegerProperty extends JsonProperty {

	public Integer getMinimum();
	public Integer getMaximum();

	public boolean isExclusiveMinimum();
	public boolean isExclusiveMaximum();

	public JsonIntegerProperty setExclusiveMinimum(final boolean exclusiveMinimum);
	public JsonIntegerProperty setExclusiveMaximum(final boolean exclusiveMaximum);

	public JsonIntegerProperty setMinimum(final int minimum);
	public JsonIntegerProperty setMinimum(final int minimum, final boolean exclusive);
	public JsonIntegerProperty setMaximum(final int maximum);
	public JsonIntegerProperty setMaximum(final int maximum, final boolean exclusive);
}