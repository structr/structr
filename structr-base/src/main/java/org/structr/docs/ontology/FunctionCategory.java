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
package org.structr.docs.ontology;

import org.structr.api.util.Category;

public enum FunctionCategory implements Category {

	AccessControl("Access Control", "Functions for controlling security and access control features of nodes."),
	Collection("Collection", null),
	Conversion("Conversion", null),
	Database("Database", "Database functions for database use.. FIXME"),
	Predicate("Predicate", ""),
	EMail("EMail", ""),
	Geocoding("Geocoding", ""),
	Http("Http", ""),
	InputOutput("Input Output", ""),
	Logic("Logic", ""),
	Mathematical("Mathematical", ""),
	Miscellaneous("Miscellaneous", ""),
	MQTT("MQTT", ""),
	Rendering("Rendering", ""),
	Security("Security", ""),
	Schema("Schema", ""),
	Scripting("Scripting", ""),
	String("String", ""),
	System("System", ""),
	Validation("Validation", "");

	private final String displayName;
	private final String shortDescription;

	FunctionCategory(final String displayName, final String shortDescription) {

		this.shortDescription = shortDescription;
		this.displayName      = displayName;
	}

	@Override
	public String getName() {
		return name();
	}

	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getShortDescription() {
		return shortDescription;
	}
}
