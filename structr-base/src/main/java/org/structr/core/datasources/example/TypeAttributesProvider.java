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
package org.structr.core.datasources.example;

import java.util.List;
import java.util.Map;

public class TypeAttributesProvider implements ExampleDataProvider {

	private static final Map<String, List<Map<String, Object>>> ExampleAttributes = Map.of(
		"Project", List.of(
			Map.of("name", "name", "type", "string"),
			Map.of("name", "description", "type", "string"),
			Map.of("name", "dueDate", "type", "date"),
			Map.of("name", "active", "type", "boolean")
		),
		"Task", List.of(
			Map.of("name", "name", "type", "string"),
			Map.of("name", "description", "type", "string"),
			Map.of("name", "dueDate", "type", "date"),
			Map.of("name", "active", "type", "boolean")
		)
	);

		@Override
	public List<Map<String, Object>> get(final String inputValue) {

		final List<Map<String, Object>> attributes = ExampleAttributes.get(inputValue);
		if (attributes != null) {

			return attributes;
		}

		return null;
	}
}
