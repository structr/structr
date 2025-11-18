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

public class MeHint extends KeywordHint {

	@Override
	public String getName() {
		return "me";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the current user.";
	}

	@Override
	public String getLongDescription() {
		return "The `me` keyword allows you to access the user in the current request. Note that it can be undefined in anonymous requests.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${me.isAdmin}", "Check the `isAdmin` flag of the current user")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
		);
	}
}
