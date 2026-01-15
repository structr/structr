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

public class OnStructrLogin extends LifecycleBase {

	public OnStructrLogin() {
		super("onStructrLogin");
	}

	@Override
	public String getShortDescription() {
		return "Called when a user starts a new session.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `onStructrLogin()` lifecycle method is called when users create new sessions by authenticating themselves with any of the login mechanisms.
		
		To receive this callback, you must create a **user-defined function** called `onStructrLogin`, instance methods or static methods will not be called.
		
		Note: You cannot prevent a user from logging in with this method. If you throw an error in this method, or the method contains a syntax error, the error will be logged but the login will *not* fail.
		""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also: `onStructrLogout()`."
		);
	}
}
