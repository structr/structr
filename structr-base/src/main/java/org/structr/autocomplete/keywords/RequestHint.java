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

public class RequestHint extends GeneralKeywordHint {

	@Override
	public String getName() {
		return "request";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the current set of HTTP request parameters.";
	}

	@Override
	public String getLongDescription() {
		return "The `request` keyword allows you to access the URL parameters that were sent with the current HTTP request. This keyword is available in all custom methods and user-defined functions, as well as in Structr Pages and Dynamic Files.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.html("""
			<!DOCTYPE html>
			<html>
				<head>
					<title>Hello ${request.name}!</title>
				</head>
				<body>
					<h1>Hello ${request.name}!</h1>
				</body>
			</html>
			""", "Access request parameters in a Structr Page")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"Only available in a context where Structr is responding to an HTTP request from the outside."
		);
	}
}
