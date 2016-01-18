/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.common.error;

/**
 * Abstract base class for all error tokens.
 *
 *
 */
public abstract class ErrorToken {

	private String property = null;
	private String type     = null;
	private Object detail   = null;
	private String token    = null;

	public ErrorToken(final String type, final String property, final String token, final Object detail) {

		this.type     = type;
		this.property = property;
		this.token    = token;
		this.detail   = detail;
	}

	public String getProperty() {
		return property;
	}

	public String getType() {
		return type;
	}

	public String getToken() {
		return token;
	}

	public Object getDetail() {
		return detail;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		if (type != null) {

			buf.append(type);
		}

		if (property != null) {

			buf.append(".");
			buf.append(property);
		}

		if (token != null) {

			buf.append(" ");
			buf.append(token);
		}

		if (detail != null) {

			buf.append(" ");
			buf.append(detail);
		}

		return buf.toString();
	}
}
