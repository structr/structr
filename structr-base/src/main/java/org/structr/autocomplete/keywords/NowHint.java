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

public class NowHint extends KeywordHint {

	@Override
	public String getName() {
		return "now";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the current timestamp.";
	}

	@Override
	public String getLongDescription() {
		return "The `now` keyword allows you to access the current time and use it in calculations etc. This keyword is mainly used in StructrScript, because in JavaScript you can simply use `new Date()`.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${date_format(now, 'dd.MM.yyyy')}", "Display the current date, for example in an HTML attribute")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
		);
	}
}
