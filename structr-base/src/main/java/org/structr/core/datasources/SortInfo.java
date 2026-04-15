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
package org.structr.core.datasources;

import java.util.Objects;

public class SortInfo {

	public final String sortKey;
	public boolean descending;
	public boolean active = false;

	public SortInfo(final String sortKey, final boolean descending) {

		this.sortKey    = sortKey;
		this.descending = descending;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sortKey, descending, active);
	}

	@Override
	public String toString() {
		return sortKey + (descending ? ">" : "<");
	}

	public static SortInfo fromString(final String sortKey) {

		if (sortKey != null) {

			final int length = sortKey.length();

			if (sortKey.endsWith(">")) {

				// sort descending
				return new SortInfo(sortKey.substring(0, length - 1), true);

			} else if (sortKey.endsWith("<")) {

				// sort ascending
				return new SortInfo(sortKey.substring(0, length - 1), false);

			} else {

				// sort ascending
				return new SortInfo(sortKey, false);
			}
		}

		return null;
	}
}
