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
package org.structr.docs;

public enum DocumentableType {

	BuiltInFunction("Built-in function", true, true),
	Keyword("Keyword", true, true),
	Method("Method", true, true),
	Property("Property", false, false),
	TypeName("Type name", false, false),
	UserDefinedFunction("User-defined function", false, false),
	MaintenanceCommand("Maintenance command", false, false),
	SystemType("System type", false, false),
	LifecycleMethod("Lifecycle method", false, true),
	Service("Service", false, false),
	Setting("Setting", false, false),
	Hidden(null, false, false);

	private final boolean supportsLanguages;
	private final boolean supportsExamples;
	private final String displayName;

	DocumentableType(final String displayName, final boolean supportsLanguages, final boolean supportsExamples) {

		this.supportsLanguages = supportsLanguages;
		this.supportsExamples  = supportsExamples;
		this.displayName       = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean supportsLanguages() {
		return supportsLanguages;
	}

	public boolean supportsExamples() {
		return supportsExamples;
	}
}
