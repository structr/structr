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

public class ParentHint extends PageKeywordHint {

	@Override
	public String getName() {
		return "parent";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the parent element of the current in a page rendering context.";
	}

	@Override
	public String getLongDescription() {
		return "The `parent` keyword allows you to access the parent of the HTML element that is currently rendering.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.html("""
			<!DOCTYPE html>
			<html>
				<body>
					<h1>Heading</h1>
					<p>Paragraph below ${parent.id}</p>
				</body>
			</html>
			""", "Outputs a paragraph with the UUID of the H1 element above")
		);
	}
}
