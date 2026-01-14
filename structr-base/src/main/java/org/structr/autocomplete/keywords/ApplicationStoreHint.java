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

public class ApplicationStoreHint extends KeywordHint {

	@Override
	public String getName() {
		return "applicationStore";
	}

	@Override
	public String getShortDescription() {
		return "Application-wide data store.";
	}

	@Override
	public String getLongDescription() {
		return "The application store can be used to store data in-memory as long as the instance is running. It can be accessed like a simple JavaScript object and can store primitive data and objects / arrays. Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript("""
			${{
				if (!$.applicationStore['code_was_run']) {
					$.log('running some code only once...');
					$.applicationStore['code_was_run'] = true;
				}
			}}
			""")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.",
			"The keyword was introduced in version 4.0 and is not available in 3.x releases.",
			"Be aware that this consumes memory - storing big amounts of data is not recommended."
		);
	}
}
