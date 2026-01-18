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
package org.structr.api.util.html;

/**
 */
public class SelectField extends Block {

	private String value = null;

	public SelectField(final Tag parent, final String id) {

		this(parent, id, null);
	}

	public SelectField(final Tag parent, final String id, final String value) {

		super(parent, "select");

		this.value = value;

		attr(new Attr("id", id));
	}

	public SelectField addOption(final String text, final String value) {

		if (value != null && value.equals(this.value)) {
			this.block("option").text(text).attr(new Attr("value", value), new Attr("selected", "selected"));
		} else {
			this.block("option").text(text).attr(new Attr("value", value));
		}

		return this;
	}
}
