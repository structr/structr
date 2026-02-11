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
package org.structr.docs.documentables.lifecycle;

import java.util.List;

public class OnNodeCreation extends LifecycleBase {

	public OnNodeCreation() {
		super("onNodeCreation");
	}

	@Override
	public String getShortDescription() {
		return "Called at the moment when a new object of this type is created.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `onNodeCreation()` lifecycle method is called at the moment when a new object of this type is created. In contrast to `onCreate()`, this method runs at the same time that the object is created.
		
		If you throw an error in this method, the enclosing transaction will be rolled back and nothing will be written to the database.
		
		- If you want to execute code at the end of the transaction, implement the `onCreate()` callback method.
		- If you want to execute code after successful validation, implement the `afterCreate()` callback method.
		""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"This lifecycle method can be defined on any node type.",
			"See also: `onCreate()`, `afterCreate()`, `error()` and `assert()`."
		);
	}
}
