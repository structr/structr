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

public class LocaleHint extends GeneralKeywordHint {

	@Override
	public String getName() {
		return "locale";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the current locale.";
	}

	@Override
	public String getLongDescription() {
		return """
		The locale of a request is determined like this in descending priority:

		1. Request parameter `locale`
		2. User locale
		3. Cookie `locale`
		4. Browser locale
		5. Default locale which was used to start the java process (evaluated via `java.util.Locale.getDefault();`)
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript("""
			${{
				$.log('Current locale is: ' + $.locale);
			}}
			""", "Print the current locale of a request to the log file")
		);
	}
}
