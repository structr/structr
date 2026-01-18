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

public class IdHint extends KeywordHint {

	@Override
	public String getName() {
		return "id";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the id of the object returned by URI Object Resolution, if available.";
	}

	@Override
	public String getLongDescription() {
		return "When a valid UUID is present in the URL of a page, Structr automatically retrieves the object associated with that UUID and makes its UUID available to all scripts, templates, and logic executed during the page rendering process under the keyword `id`.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
		);
	}
}
