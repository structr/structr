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
package org.structr.test.common;

import org.structr.core.function.SearchVectorFunction;
import org.structr.docs.Badges;
import org.structr.docs.formatter.markdown.MarkdownTableWithDetailsFormatter;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The Enterprise badge of a function in the reference of the built-in functions: on the passage of its
 * details, and inline next to its name in the table.
 */
public class EnterpriseBadgeTest {

	@Test
	public void theDetailsOfAFunctionAreOneMarkedPassage() {

		final List<String> details = List.of("### documents.vectorSearch", "Searches by meaning.", "", "#### Parameters", "- query", "#### Examples", "code");
		final List<String> marked  = Badges.markFirstSection(details);

		assertEquals("### documents.vectorSearch {enterprise}", marked.getFirst());
		assertEquals("only the first heading is marked", details.subList(1, details.size()), marked.subList(1, marked.size()));

		final String html = String.join("\n", Badges.toHtml(marked));

		// one passage around everything, the subheadings included, and the heading without the marker
		assertEquals(1, count(html, "badge-passage badge-enterprise"));
		assertTrue(html.indexOf("badge-passage") < html.indexOf("### documents.vectorSearch"));
		assertTrue(html.lastIndexOf("</div>") > html.indexOf("code"));
		assertFalse(html.contains("{enterprise}"));
	}

	@Test
	public void aTableRowOfAnEnterpriseFunctionHasTheInlineBadge() {

		final Map<String, String> headers = new LinkedHashMap<>();
		final List<String> lines          = new LinkedList<>();

		headers.put("Name", "`displayName`");
		headers.put("Description", "shortDescription");

		MarkdownTableWithDetailsFormatter.formatMarkdownTable(lines, headers, List.of(
			Map.of("displayName", "documents.vectorSearch", "shortDescription", "Searches by meaning.", "badge", Badges.inlineHtml()),
			Map.of("displayName", "searchVector", "shortDescription", "Searches a vector index.")
		));

		// an empty line, the header and the separator come first
		assertTrue(lines.get(3).contains("<span class=\"badge-line\"><code id=\"documents.vectorSearch\">documents.vectorSearch</code><span class=\"badge-inline badge-enterprise\""));
		assertFalse(lines.get(4).contains("badge"));
	}

	@Test
	public void functionsAreNotEnterpriseByDefault() {

		assertFalse(new SearchVectorFunction().isEnterprise());
	}

	// ----- private methods -----
	private int count(final String text, final String part) {

		return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
	}
}
