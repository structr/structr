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
package org.structr.common.error;

/**
 * Indicates that a specific property value already exists in the database.
 *
 *
 */
public class UniqueToken extends SemanticErrorToken {

	public UniqueToken(final String type, final String property, final String uuid, final String existingUuid, final Object value) {

		super(type, property, "already_taken");

		withDetail(uuid);
		withValue(value);

		with("existingNodeUuid", existingUuid);
	}

	public String getExistingUuid() {
		return (String)data.get("existingNodeUuid");
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		if (getType() != null) {

			buf.append(getType());
		}

		if (getProperty() != null) {

			buf.append(".");
			buf.append(getProperty());
		}

		if (getToken() != null) {

			buf.append(" ");
			buf.append(getToken());
		}

		if (getDetail() != null) {

			buf.append(" ");
			buf.append(getDetail());
		}

		if (getExistingUuid() != null) {

			buf.append(". Existing uuid: ");
			buf.append(getExistingUuid());
		}

		if (getValue() != null) {

			buf.append(" Value = ");
			buf.append(getValue());
		}

		return buf.toString();
	}

}
