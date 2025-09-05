/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.api.util.html;

/**
 */
public class InputField extends Empty {

	public InputField(final Tag parent, final String type, final String name, final String value) {
		this(parent, type, name, value, null);
	}

	public InputField(final Tag parent, final String type, final String id, final String value, final String placeholder) {

		super(parent, "input");

		attr(new Attr("type", type));
		attr(new Attr("id", id));
		attr(new Attr("value", value));

		if (placeholder != null) {

			attr(new Attr("placeholder", placeholder));
		}
	}
}
