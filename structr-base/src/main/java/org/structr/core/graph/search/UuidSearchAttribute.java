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
package org.structr.core.graph.search;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.search.Occurrence;
import org.structr.api.search.UuidQuery;
import org.structr.core.GraphObject;

/**
 * Represents an attribute for textual search, used in {@link SearchNodeCommand}.
 */
public class UuidSearchAttribute extends SearchAttribute<String> implements UuidQuery {

	public UuidSearchAttribute(final String value, final Occurrence occur) {
		super(occur, GraphObject.id, value);
	}

	@Override
	public String toString() {
		return "UuidSearchAttribute(" + super.toString() + ")";
	}

	@Override
	public Class getQueryType() {
		return UuidQuery.class;
	}

	@Override
	public boolean isExactMatch() {
		return true;
	}

	@Override
	public void setExactMatch(final boolean exact) {
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {

		String nodeValue     = entity.getProperty(getKey());
		Occurrence occur     = getOccurrence();
		String searchValue   = getValue();

		if (occur.equals(Occurrence.FORBIDDEN)) {

			if ((nodeValue != null) && compare(nodeValue, searchValue) == 0) {

				// don't add, do not check other results
				return false;
			}

		} else {

			if (nodeValue != null) {

				if (compare(nodeValue, searchValue) != 0) {
					return false;
				}

			} else {

				if (searchValue != null && StringUtils.isNotBlank(searchValue)) {
					return false;
				}
			}
		}

		return true;
	}

	// ----- interface UuidQuery -----
	@Override
	public String getUuid() {
		return getValue();
	}

	// ----- private methods -----
	private int compare(final String nodeValue, final String searchValue) {
		return nodeValue.compareTo(searchValue);
	}
}
