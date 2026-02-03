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

import org.structr.docs.Parameter;
import org.structr.docs.ontology.ConceptType;

import java.util.ArrayList;
import java.util.List;

public class OnOAuthLogin extends LifecycleBase {

	public OnOAuthLogin() {
		super("onOAuthLogin");
	}

	@Override
	public String getShortDescription() {
		return "Called when a user authenticates with oAuth.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `onOAuthLogin()` lifecycle method is called when users create login via oAuth.
		
		To receive this callback, you must create a **user-defined function** called `onStructrLogin`, instance methods or static methods will not be called.
		
		This method will be called with the following arguments:
		
		| Name | Description |
		| --- | --- |
		| provider | The name of the oAuth provider that handled the login |
		| userinfo | The map of user information sent by the oAuth server |
		
		Note: You cannot prevent a user from logging in with this method. If you throw an error in this method, or the method contains a syntax error, the error will be logged but the login will *not* fail.
		""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"This lifecycle method must be defined on the type `User` or its descendants.",
			"See also: `onStructrLogin()`, `onStructrLogout()`."
		);
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> linkedConcepts = super.getLinkedConcepts();

		linkedConcepts.add(Link.to("ispartof", ConceptReference.of(ConceptType.SystemType, "User")));

		return linkedConcepts;
	}
}
