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

public class AfterCreate extends LifecycleBase {

	public AfterCreate() {
		super("afterCreate");
	}

	@Override
	public String getShortDescription() {
		return "Called after a new object of this type is created.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `afterCreate()` lifecycle method is called after a new object of this type is created. This method runs after the creating transaction is committed, so you can be sure that the validation was successful and the object is stored in the database.

		This method would be the right place to send a welcome email, for example, as you can be sure that the user exists.
		""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"This lifecycle method can be defined on any node type.",
			"See also: `onCreate()`."
		);
	}
}
