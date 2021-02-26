/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.files.cmis.repository;

import java.util.LinkedList;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.structr.cmis.common.CMISExtensionsData;

/**
 *
 *
 */
public class StructrPropertyData<T> extends CMISExtensionsData implements PropertyData<T> {

	private final List<T> values = new LinkedList<>();
	private String displayName   = null;
	private String name          = null;
	private String id            = null;

	public StructrPropertyData(final String id, final String name, final String displayName, final T... values) {

		this.displayName = displayName;
		this.name        = name;
		this.id          = id;

		for (final T value : values) {
			this.values.add(value);
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getLocalName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getQueryName() {
		return getLocalName();
	}

	@Override
	public List<T> getValues() {
		return values;
	}

	@Override
	public T getFirstValue() {

		if (!values.isEmpty()) {
			return values.get(0);
		}

		return null;
	}

}
