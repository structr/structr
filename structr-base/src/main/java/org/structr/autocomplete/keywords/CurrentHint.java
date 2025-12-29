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

import org.structr.autocomplete.GeneralKeywordHint;
import org.structr.docs.Example;

import java.util.List;

public class CurrentHint extends GeneralKeywordHint {

	@Override
	public String getName() {
		return "current";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the object returned by URI Object Resolution, if available.";
	}

	@Override
	public String getLongDescription() {
		return "When a valid UUID is present in the URL of a page, Structr automatically retrieves the object associated with that UUID and makes it available to all scripts, templates, and logic executed during the page rendering process under the keyword `current`.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.html("""
			<!DOCTYPE html>
			<html>
				<head>
					<title>${current.name}</title>
				</head>
				<body>
					<h1>${current.name}</h1>
				</body>
			</html>
			""", "Print the name of the current object in page title and heading")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
		);
	}
}
