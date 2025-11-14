/*
 * Copyright (C) 2010-2025 Structr GmbH
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

public class PageHint extends KeywordHint {

	@Override
	public String getName() {
		return "page";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the current page in a page rendering context.";
	}

	@Override
	public String getLongDescription() {
		return "The `page` keyword allows you to access the current Page object that handles the request in which the current script is executed.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("""
			<!DOCTYPE html>
			<html>
				<head>
					<title>${page.name}</title>
			</html>
			""", "Set that HTML page title to the name of the page that renders it")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
		);
	}
}
