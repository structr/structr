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
package org.structr.test.web.datasource;

import org.structr.web.common.AsyncBuffer;
import org.structr.web.datasource.TagWithCSSInfo;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * #1579: TagWithCSSInfo.formatStartTag() emitted additional attributes as key="value" without
 * escaping. For structr-component tags the value (e.g. a pagination key from the request) and the
 * name are request-influenced, so an unescaped value broke out of the attribute. The output must
 * now escape values for an HTML attribute context and restrict attribute names to [A-Za-z0-9-].
 */
public class TagWithCSSInfoTest {

	private static String render(final Map<String, String> data) {

		final TagWithCSSInfo tag  = new TagWithCSSInfo("div#content");
		final AsyncBuffer buffer  = new AsyncBuffer();

		tag.formatStartTag(buffer, data, null);

		return String.join("", buffer.getQueue());
	}

	@Test
	public void testAttributeValueIsEscaped() {

		final Map<String, String> data = new LinkedHashMap<>();
		data.put("data-page", "\"><script>alert(document.cookie)</script>");

		final String out = render(data);

		assertFalse("value must not be emitted raw: " + out, out.contains("<script>"));
		assertFalse("value must not close the attribute early: " + out, out.contains("data-page=\"\">"));
		assertTrue("value must be html-attribute-escaped: " + out, out.contains("&lt;script&gt;"));
		assertTrue("quote in value must be escaped: " + out, out.contains("&quot;"));
	}

	@Test
	public void testDottedSortKeyAttributeNameIsPreserved() {

		// #1579 regression guard: a dotted sort key like "project.sort" must survive name sanitization.
		// The client pairs data-structr-target="project.sort" with the data-project.sort carrier, so
		// stripping the dot (to data-projectsort) silently breaks table sorting.
		final Map<String, String> data = new LinkedHashMap<>();
		data.put("data-project.sort", "name<");

		final String out = render(data);

		assertTrue("dotted attribute name must be preserved: " + out, out.contains(" data-project.sort=\""));
		assertTrue("value must still be html-attribute-escaped: " + out, out.contains("name&lt;"));
		assertFalse("value must not be emitted raw: " + out, out.contains("name<\""));
	}

	@Test
	public void testAttributeNameIsSanitized() {

		final Map<String, String> data = new LinkedHashMap<>();
		// a malicious attribute name that tries to break out and add an event handler
		data.put("x\" onmouseover=\"alert(1)", "1");

		final String out = render(data);

		assertFalse("attacker-controlled name must not inject an event handler: " + out, out.contains("onmouseover=\"alert"));
		assertFalse("attacker-controlled name must not contain a raw quote: " + out, out.contains("x\" "));
		// the name is reduced to [A-Za-z0-9-]
		assertTrue("sanitized name must remain: " + out, out.contains(" xonmouseoveralert1=\"1\""));
	}
}
