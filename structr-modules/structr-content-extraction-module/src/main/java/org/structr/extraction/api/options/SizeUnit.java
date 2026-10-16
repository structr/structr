/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.extraction.api.options;

import java.util.Locale;

/**
 * The unit of chunk size and overlap.
 */
public enum SizeUnit {

	TOKENS, CHARACTERS;

	public String getValue() {

		return name().toLowerCase(Locale.ROOT);
	}

	public static SizeUnit fromValue(final String value) {

		if (value != null) {

			for (final SizeUnit unit : values()) {

				if (unit.name().equalsIgnoreCase(value.trim())) {

					return unit;
				}
			}
		}

		throw new IllegalArgumentException("Unknown size unit '" + value + "', expected tokens or characters.");
	}
}
