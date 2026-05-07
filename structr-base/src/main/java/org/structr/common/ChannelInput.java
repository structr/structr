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
package org.structr.common;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.core.GraphObject;
import org.structr.core.datasources.SortInfo;
import org.structr.web.datasource.DataField;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ChannelInput implements Predicate<GraphObject> {

	private final List<SortInfo> sortInfos         = new LinkedList<>();
	private final List<DataField> searchableFields = new LinkedList<>();
	private final String filter;
	private final String transform;
	private final int pageSize;
	private final int page;

	public ChannelInput(final String transform) {
		this(transform, null, null, Integer.MAX_VALUE, 1);
	}

	public ChannelInput(final String transform, final String filter, final List<String> sortStrings, final int pageSize, final int page) {

		this.filter    = filter != null ? filter.toLowerCase() : null;
		this.transform = transform;
		this.pageSize  = pageSize;
		this.page      = page;

		if (sortStrings != null) {

			for (String sortKey : sortStrings) {

				if (sortKey != null) {

					sortInfos.add(SortInfo.fromString(sortKey));
				}
			}
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(sortInfos, searchableFields, filter, transform, pageSize, page);
	}

	@Override
	public boolean equals(final Object other) {
		return hashCode() == other.hashCode();
	}

	public List<SortInfo> sortKeys() {
		return sortInfos;
	}

	public String transform() {
		return transform;
	}

	public String filter() {
		return filter;
	}

	public int pageSize() {
		return pageSize;
	}

	public int page() {
		return page;
	}

	public List<DataField> searchableFields() {
		return searchableFields;
	}

	@Override
	public boolean accept(final GraphObject object) {

		final PropertyContainer container = object.getPropertyContainer();

		if (filter != null) {

			boolean accept = false;

			for (final DataField field : searchableFields) {

				final Object value = container.getProperty(field.getPropertyName());
				if (value != null) {

					if (value.toString().toLowerCase().contains(filter)) {
						return true;
					}
				}
			}

			return accept;
		}

		return true;
	}
}
