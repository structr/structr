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
package org.structr.common.trace;

import java.util.Map;

/**
 * An HTTP request: its method and target, such as <code>HTTP GET /structr/html/page</code>.
 */
public class RequestFrame extends EntryFrame {

	private final String method;

	public RequestFrame(final String method, final String target, final String sessionId) {

		super(target, sessionId);

		this.method = method;
	}

	@Override
	public String kind() {
		return "HTTP";
	}

	@Override
	public String describe() {
		return "HTTP " + method + " " + getName();
	}

	@Override
	public Map<String, Object> toMap() {

		final Map<String, Object> map = super.toMap();

		map.put("method", method);

		return map;
	}
}
