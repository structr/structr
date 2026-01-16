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
package org.structr.autocomplete.keywords;

import org.structr.autocomplete.KeywordHint;
import org.structr.docs.Example;

import java.util.List;

public class SessionHint extends KeywordHint {

	@Override
	public String getName() {
		return "session";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the current HTTP session.";
	}

	@Override
	public String getLongDescription() {
		return """
			The `session` keyword allows you to access the HTTP session, store data in it and query session metadata like the session ID, the creation time etc.
			
			The following keys are read-only and return session metadata, all other keys can be used to store arbitrary data in the session.
			
			| Name | Description | Type |
			| ---| --- | --- |
			| id | session ID | string |
			| creationTime | creation timestamp (in milliseconds since epoch) | long |
			| isNew | if the session was just created | boolean |
			| lastAccessedTime | last access timestamp (milliseconds since epoch) | long |
			""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${log(session.id)}", "Log the session ID of the current request"),
			Example.javaScript("""
			${{
				$.session.myData = 'test';
				$.session.cart = [ { name: 'item1', amount: 3 } ];
			}}
			""", "Store some arbitrary data in the current session")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"Only available in a context where Structr is responding to an HTTP request from the outside."
		);
	}
}
