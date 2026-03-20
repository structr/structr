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
import org.structr.core.GraphObject;

import java.util.LinkedList;
import java.util.List;

public class ChannelInput implements Predicate<GraphObject> {

	private final List<String> sortKeys = new LinkedList<>();
	private final String transform;
	private final int pageSize;
	private final int page;

	public ChannelInput() {
		this(null);
	}

	public ChannelInput(final String transform) {
		this(null, Integer.MAX_VALUE, 1);
	}

	public ChannelInput(final String transform, final int pageSize, final int page) {

		this.transform = transform;
		this.pageSize  = pageSize;
		this.page      = page;
	}

	public List<String> sortKeys() {
		return sortKeys;
	}

	public String transform() {
		return transform;
	}

	public int pageSize() {
		return pageSize;
	}

	public int page() {
		return page;
	}

	@Override
	public boolean accept(final GraphObject value) {
		return true;
	}
}
