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

import java.util.LinkedList;
import java.util.List;

public class ChannelInput {

	private final List<String> sortKeys = new LinkedList<>();
	private final String transform;

	public ChannelInput() {
		this(null);
	}

	public ChannelInput(final String transform) {
		this.transform = transform;
	}

	public List<String> sortKeys() {
		return sortKeys;
	}

	public String transform() {
		return transform;
	}
}
