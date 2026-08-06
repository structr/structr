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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * What a Content node does with the text a script produces, depending on HOW the script produced it.
 *
 * A text/plain Content node post-processes its output in exactly two ways (see
 * RenderContextContentHandler.transform): it escapes HTML, and it turns newlines into &lt;br&gt;. Both
 * apply to the value a script RETURNS. Neither applies to what a script writes with $.print, so the
 * same string comes out escaped or raw depending on which form the author used - and the escaping half
 * of that is a hole, not a cosmetic difference: a printed value renders as live HTML in a node whose
 * content type promises the opposite.
 *
 * The tests are grouped on purpose:
 *
 *   - the RETURNED-value and literal-text cases hold today and are the net for what already works;
 *   - the PRINTED cases assert the same guarantees and FAIL until print output is passed through the
 *     same transform. They are the bug, stated as the behaviour that should replace it;
 *   - the text/html case pins what must NOT change, so a fix does not start escaping content whose
 *     type says it is markup.
 */
public class ContentScriptOutputTest extends StructrUiTest {

	/** puts the body into the single Content node of a simple page and returns the rendered html */
	private String render(final String content, final String contentType) {

		return render("page1", content, contentType);
	}

	/**
	 * The same, on a NAMED page. Two renders in one test method need two pages: rendering twice under one
	 * name left the second call resolving to the FIRST page, so a test comparing the two outputs compared
	 * a string with itself and passed without checking anything.
	 */
	private String render(final String pageName, final String content, final String contentType) {

		try (final Tx tx = app.tx()) {

			if (app.nodeQuery(StructrTraits.USER).name(ADMIN_USERNAME).getFirst() == null) {

				createAdminUser();
			}

			final Page page   = Page.createSimplePage(securityContext, pageName);
			final DOMNode div = page.getElementsByTagName("div").get(0);
			final Content text = div.getFirstChild().as(Content.class);

			text.setProperty(Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), contentType);
			text.setProperty(Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_PROPERTY), content);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("could not build the page: " + fex.getMessage());
		}

		RestAssured.basePath = "/";

		return RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			.when()
			.get("/html/" + pageName)
			.body()
			.asString();
	}

	/* ---- what holds today: the value a script returns, and literal text --------------------- */

	@Test
	public void theReturnedValueHasItsNewlinesTurnedIntoBreaks() {

		final String html = render("${{ 'a\\nb'; }}", "text/plain");

		assertTrue("a newline in the returned value must render as a break: " + snippet(html), html.contains("a<br>b"));
	}

	@Test
	public void theReturnedValueIsEscaped() {

		final String html = render("${{ '<b>x</b>&y'; }}", "text/plain");

		assertTrue("the returned value must be escaped: " + snippet(html), html.contains("&lt;b&gt;x&lt;/b&gt;&amp;y"));
		assertTrue("and must not reach the page as markup: " + snippet(html), !html.contains("<b>x</b>"));
	}

	@Test
	public void literalTextHasItsNewlinesTurnedIntoBreaks() {

		// the same post-processing, on content with no script in it at all
		final String html = render("a\nb", "text/plain");

		assertTrue("a newline in literal text must render as a break: " + snippet(html), html.contains("a<br>b"));
	}

	/* ---- THE BUG: the same guarantees for printed output ------------------------------------- */

	@Test
	public void printedOutputIsEscapedLikeAReturnedValue() {

		/* FAILS until print output goes through RenderContextContentHandler.transform(). This is the
		   serious half: a script that prints anything a user typed - $.print($.this.name) - renders it as
		   live HTML, while the identical $.this.name as the script's value is escaped. */
		final String html = render("${{ $.print('<b>x</b>&y'); }}", "text/plain");

		assertTrue("printed output must be escaped in a text/plain node: " + snippet(html), html.contains("&lt;b&gt;x&lt;/b&gt;&amp;y"));
		assertTrue("printed markup must not reach the page as markup: " + snippet(html), !html.contains("<b>x</b>"));
	}

	@Test
	public void aPrintedScriptTagIsNotExecutable() {

		// FAILS today: the tag arrives verbatim, so printed user data is an injection point
		final String html = render("${{ $.print('<script>window.__probe=1</script>'); }}", "text/plain");

		assertTrue("a printed script tag must not survive into the page: " + snippet(html), !html.contains("<script>window.__probe=1</script>"));
		assertTrue("it must be escaped instead: " + snippet(html), html.contains("&lt;script&gt;"));
	}

	@Test
	public void printedNewlinesBecomeBreaksLikeAReturnedValue() {

		// FAILS today: the newline stays a raw newline, which the browser collapses to a space
		final String html = render("${{ $.print('a\\nb'); }}", "text/plain");

		assertTrue("a newline in printed output must render as a break: " + snippet(html), html.contains("a<br>b"));
	}

	@Test
	public void printedAndReturnedOutputAgree() {

		/* the invariant the two above are instances of: how the text was emitted must not change what
		   the page shows. Stated on its own so a partial fix - escaping but not newlines, or the other
		   way round - still fails something. */
		final String printed  = render("printed-page",  "${{ $.print('<i>a</i>\\nb'); }}", "text/plain");
		final String returned = render("returned-page", "${{ '<i>a</i>\\nb'; }}", "text/plain");
		final String printedBody  = between(printed);
		final String returnedBody = between(returned);

		assertTrue("print and return must render identically, got\n  printed:  " + printedBody
			+ "\n  returned: " + returnedBody, printedBody.equals(returnedBody));
	}

	/* ---- what must NOT change: a content type that says markup ------------------------------ */

	@Test
	public void aTextHtmlNodeStaysUnescapedBothWays() {

		/* text/html means the author wants markup, so NEITHER form is escaped and no newline becomes a
		   break. A fix for the printed path must not start escaping here. */
		final String returned = render("returned-html", "${{ '<b>x</b>'; }}", "text/html");
		final String printed  = render("printed-html",  "${{ $.print('<b>x</b>'); }}", "text/html");

		assertTrue("a returned value in a text/html node stays markup: " + snippet(returned), returned.contains("<b>x</b>"));
		assertTrue("and so does printed output: " + snippet(printed), printed.contains("<b>x</b>"));
	}

	/* ---- helpers ---------------------------------------------------------------------------- */

	/** the rendered div, which is where the Content node sits, for readable failure messages */
	private static String between(final String html) {

		final int from = html.indexOf("<div>");
		final int to   = html.indexOf("</div>", from);

		return from < 0 || to < 0 ? html.trim() : html.substring(from + "<div>".length(), to).trim();
	}

	private static String snippet(final String html) {

		return "\n---\n" + between(html) + "\n---";
	}
}
