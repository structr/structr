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
import org.structr.docs.Language;

import java.util.List;

public class PredicateHint extends KeywordHint {

	@Override
	public String getName() {
		return "predicate";
	}

	@Override
	public String getShortDescription() {
		return "Refers to the set of query predicates for advanced `find()`.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `$.predicate` keyword allows you to access a set of query predicates for advanced `find()`.

		The following predicates are available.

		| Name | Description |
		| --- | --- |
		| `$.predicate.and()` | combine other predicates with AND |
		| `$.predicate.or()` | combine other predicates with OR |
		| `$.predicate.contains()` | contains query |
		| `$.predicate.page()` | database-based pagination |
		| `$.predicate.sort()` | database-based sorting |
		| ... | ... |
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript("""
			${{
				let users = $.find('User', { eMail: $.predicate.contains('structr.com') });
			}}
			""")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This keyword is defined in JavaScript only."
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.JavaScript);
	}
}
