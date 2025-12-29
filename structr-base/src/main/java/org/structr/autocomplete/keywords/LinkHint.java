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

import org.structr.autocomplete.PageKeywordHint;
import org.structr.docs.Example;

import java.util.List;

public class LinkHint extends PageKeywordHint {

	@Override
	public String getName() {
		return "link";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the linked filesystem element of an HTML element in a Page.";
	}

	@Override
	public String getLongDescription() {
		return """
		Only works in `a`, `link`, `script` or `img` tags/nodes. See Filesystem and Pages Tree View for more info.

		The `link` keyword can only be accessed if a node of the above types is actually linked to a filesystem element. It can be linked via the link icon which is displayed when hovering over a node.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.html("""
			<!doctype html>
			<html>
				<body>
					<a href="${link.path}">Download ${link.name}</a>
				</body>
			</html>
			""", "Provide a download link for a linked file"),
			Example.html("""
			<!doctype html>
			<html>
				<body>
					<img src="/${link.id}" />
				</body>
			</html>
			""", "Display a linked image")
		);
	}
}
