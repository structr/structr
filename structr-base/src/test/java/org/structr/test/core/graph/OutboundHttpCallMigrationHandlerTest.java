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
package org.structr.test.core.graph;

import org.structr.core.graph.OutboundHttpCallMigrationHandler;
import org.structr.core.graph.OutboundHttpCallMigrationHandler.Verdict;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * How the 7.0 migration report classifies a call.
 *
 * The classifier only ever reports; the value of a test here is that it must not call something
 * automatically migratable when it is not, because a person reading the report would then trust a
 * rewrite that moves a value into the wrong option.
 */
public class OutboundHttpCallMigrationHandlerTest {

	@Test
	public void testCallsAreFoundInSurroundingCode() {

		final String source = "${{ $.log('before'); const r = $.POST('http://x/', body, 'application/json', 'UTF-8'); $.GET('http://y/'); }}";
		final List<String> calls = OutboundHttpCallMigrationHandler.findCalls(source);

		assertEquals("both calls must be found", 2, calls.size());
		assertEquals("POST('http://x/', body, 'application/json', 'UTF-8')", calls.get(0));
		assertEquals("GET('http://y/')", calls.get(1));
	}

	@Test
	public void testNestedCallsAndCommasDoNotConfuseTheSplitter() {

		// a comma inside a nested call, a string and an object must not split the argument list
		final String call = "POST(concat('http://x/', id), '{ \"a\": 1, \"b\": 2 }', 'application/json', { timeout: 30, redirects: true })";

		assertEquals(4, OutboundHttpCallMigrationHandler.splitArguments(call.substring(5, call.length() - 1)).size());
	}

	@Test
	public void testNewStyleCallsAreUpToDate() {

		assertEquals(Verdict.UP_TO_DATE, verdict("POST('http://x/', body, 'application/json')"));
		assertEquals(Verdict.UP_TO_DATE, verdict("POST('http://x/', body, 'application/json', { timeout: 30 })"));
		assertEquals(Verdict.UP_TO_DATE, verdict("GET('http://x/')"));
		assertEquals(Verdict.UP_TO_DATE, verdict("GET('http://x/', 'text/html', { selector: 'div' })"));
		assertEquals(Verdict.UP_TO_DATE, verdict("DELETE('http://x/')"));
		assertEquals(Verdict.UP_TO_DATE, verdict("DELETE('http://x/', null, null, { parseResponse: true })"));
	}

	@Test
	public void testLiteralTailsCanBeMigratedAutomatically() {

		// charset in the fourth position, the classic pre-7.0 call
		assertEquals(Verdict.AUTOMATIC, verdict("POST('http://x/', 'body', 'application/json', 'UTF-8')"));

		// username and password behind it
		assertEquals(Verdict.AUTOMATIC, verdict("POST('http://x/', 'body', 'application/json', 'UTF-8', 'user', 'pass')"));

		// GET with credentials
		assertEquals(Verdict.AUTOMATIC, verdict("GET('http://x/', 'application/json', 'user', 'pass')"));
	}

	@Test
	public void testExpressionsNeedAPerson() {

		// the charset comes from a variable, so it cannot be folded into the content type mechanically
		assertEquals(Verdict.MANUAL, verdict("POST('http://x/', 'body', 'application/json', charset)"));

		// a concatenation only looks like a literal at its start
		assertEquals(Verdict.MANUAL, verdict("POST('http://x/', 'body', 'application/json', 'UTF' + '-8')"));

		// and a function call in the tail
		assertEquals(Verdict.MANUAL, verdict("GET('http://x/', 'application/json', getUser(), 'pass')"));
	}

	@Test
	public void testGetIsAmbiguousWhenTheContentTypeIsAnExpression() {

		// with text/html the third argument is a CSS selector, otherwise a user name. If the content
		// type is not visible in the source, which of the two it is cannot be decided.
		assertEquals(Verdict.MANUAL, verdict("GET('http://x/', contentType, 'div.content')"));

		// spelled out, it is decidable
		assertEquals(Verdict.AUTOMATIC, verdict("GET('http://x/', 'text/html', 'div.content')"));
	}

	@Test
	public void testTextHtmlSelectorIsNotMistakenForAUserName() {

		// from a real 6.x method: with text/html the third argument is a CSS selector. Reporting it as a
		// user name would send a reader off to write { username: 'title' }.
		final var finding = OutboundHttpCallMigrationHandler.assess("SchemaMethod", "id", "n", "source",
			"GET(downloadUrl, 'text/html', 'title')");

		assertEquals(Verdict.AUTOMATIC, finding.verdict());
		assertTrue("the selector must be named as a selector, was: " + finding.reason(), finding.reason().contains("selector"));
		assertFalse("the selector must not be called a user name, was: " + finding.reason(), finding.reason().contains("username"));
	}

	@Test
	public void testOctetStreamNoLongerSwitchesTransport() {

		// this call still parses under 7.0, but it silently returns a string where it used to return a
		// stream, so it has to be reported even though nothing about its arguments is wrong
		final var get = OutboundHttpCallMigrationHandler.assess("SchemaMethod", "id", "n", "source",
			"GET(downloadUrl, 'application/octet-stream')");

		// the exact key, not just the word: an unknown option is refused, so advice naming the wrong one
		// produces a call that fails at the very check the split was built for
		assertEquals(Verdict.AUTOMATIC, get.verdict());
		assertTrue("GET must be told binaryResponse, was: " + get.reason(), get.reason().contains("binaryResponse"));

		final var post = OutboundHttpCallMigrationHandler.assess("SchemaMethod", "id", "n", "source",
			"POST(url, body, 'application/octet-stream')");

		assertEquals(Verdict.AUTOMATIC, post.verdict());
		assertTrue("POST must be told binaryBody, was: " + post.reason(), post.reason().contains("binaryBody"));
		assertFalse("POST must not be told the GET key, was: " + post.reason(), post.reason().contains("binaryResponse"));
	}

	@Test
	public void testAnExpressionContentTypeIsReportedForGetAndPost() {

		// the value decides both binary transport and, for GET, what the next argument means
		assertEquals(Verdict.MANUAL, verdict("GET(url, contentType)"));
		assertEquals(Verdict.MANUAL, verdict("POST(url, body, contentType)"));

		// and the key it names there is the verb's own one too
		assertTrue(OutboundHttpCallMigrationHandler.assess("SchemaMethod", "i", "n", "source", "GET(url, contentType)").reason().contains("binaryResponse"));
		assertTrue(OutboundHttpCallMigrationHandler.assess("SchemaMethod", "i", "n", "source", "POST(url, body, contentType)").reason().contains("binaryBody"));
	}

	@Test
	public void testDeletesShiftedOptionsAreReported() {

		// the second argument is the request body now. An options object there is a mechanical move,
		// anything else used to mean "parse the response as JSON if this says application/json".
		final var options = OutboundHttpCallMigrationHandler.assess("SchemaMethod", "id", "n", "source",
			"DELETE(url, { parseResponse: true })");

		assertEquals(Verdict.AUTOMATIC, options.verdict());
		assertTrue("the new position must be shown, was: " + options.reason(), options.reason().contains("null, null"));

		final var contentType = OutboundHttpCallMigrationHandler.assess("SchemaMethod", "id", "n", "source",
			"DELETE(url, 'application/json')");

		assertEquals(Verdict.MANUAL, contentType.verdict());
		assertTrue("parseResponse must be named, was: " + contentType.reason(), contentType.reason().contains("parseResponse"));
	}

	// ----- private methods -----
	private Verdict verdict(final String call) {

		return OutboundHttpCallMigrationHandler.assess("SchemaMethod", "id", "name", "source", call).verdict();
	}
}
