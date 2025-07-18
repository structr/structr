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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.html.attr.Context;

/**
 *
 *
 */
public class Attr {

	private String key   = null;
	private Object value = null;

	public Attr(final String key, final Object value) {
		this.key = key;
		this.value = value;
	}

	public String format(final Context context) {
		return key + "=\"" + escapeAttributeValue(value.toString()) + "\"";
	}

	private String escapeAttributeValue(final String attrValue) {
		return StringUtils.replaceEach(attrValue, new String[]{"&", "<", ">", "\""}, new String[]{"&amp;", "&lt;", "&gt;", "&quot;"});
	}

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}
}
