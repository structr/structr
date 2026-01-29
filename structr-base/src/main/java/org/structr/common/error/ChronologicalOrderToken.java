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
package org.structr.common.error;

/**
 * Indicates that two given date properties must be ordered chronologically.
 */
public class ChronologicalOrderToken extends SemanticErrorToken {

	public ChronologicalOrderToken(final String type, final String propertyKey1, final String propertyKey2) {

		super(type, propertyKey1, "must_lie_before");

		withDetail(propertyKey2);
	}
}
