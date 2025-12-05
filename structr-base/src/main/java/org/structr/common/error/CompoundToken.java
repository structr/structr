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
package org.structr.common.error;

import org.structr.core.property.PropertyKey;

import java.util.Set;

/**
 * Indicates that a specific property value already exists in the database.
 */
public class CompoundToken extends ErrorToken {

	public CompoundToken(final String type, final Set<PropertyKey> keys, final String uuid) {

		super("already_taken");

		withType(type);
		withDetail(uuid);

		with("keys", keys);
	}

	@Override
	public Object getValue() {
		return data.get("keys");
	}
}
