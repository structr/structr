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

import org.structr.docs.Example;

import java.util.List;

public class OnStructrLogout extends LifecycleBase {

	public OnStructrLogout() {
		super("onStructrLogout");
	}

	@Override
	public String getShortDescription() {
		return "Called when a user finishes a new session by logging out.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `onStructrLogout()` lifecycle method is called when users finish a session by logging themselves out, **OR** when the session times out.
		
		To receive this callback, you must create a **user-defined function** called `onStructrLogout`, instance methods or static methods will not be called.
		
		Note: You cannot prevent a user from logging out with this method. If you throw an error in this method, or the method contains a syntax error, the error will be logged but the logout will *not* fail.
		""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also: `onStructrLogin()`."
		);
	}
}
