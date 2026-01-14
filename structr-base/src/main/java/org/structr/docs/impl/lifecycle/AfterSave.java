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
package org.structr.docs.impl.lifecycle;

import java.util.List;

public class AfterSave extends LifecycleBase {

	public AfterSave() {
		super("afterSave");
	}

	@Override
	public String getShortDescription() {
		return "Called after an existing object of this type is modified.";
	}

	@Override
	public String getLongDescription() {
		return "The `afterSave()` lifecycle method is called after an existing object of this type is modified. This method runs after the modifying transaction is committed, so you can be sure that the validation was successful and the object is stored in the database.";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also: `onSave()`."
		);
	}
}
