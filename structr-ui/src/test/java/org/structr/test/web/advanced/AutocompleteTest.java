/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.autocomplete.AbstractHintProvider;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;

public class AutocompleteTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(AutocompleteTest.class.getName());

	@Test
	public void testAutocompleteInMixedContent() {

		// StructrScript in the first script block
		assertFirstResult("text", "page", AbstractHintProvider.getHints(securityContext, false, null, "<html><head><title>${pa", "", 0, 0));

		// Non-Script block, no results
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "<html><head><title>${page.name}this.", "", 0, 0));

		// StructrScript in the second script block
		assertFirstResult("text", "titleize(str)", AbstractHintProvider.getHints(securityContext, false, null, "<html><head><title>${page.name}</title></head><body><h1>${titl", "", 0, 0));

		// Javascript in the third script block
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "<html><head><title>${page.name}</title></head><body><h1>${titleize(page.name)}</h1><div>${{\n\n\tlet self = $.", "", 0, 0));

		// Javascript in the third script block
		assertFirstResult("text", "this", AbstractHintProvider.getHints(securityContext, false, null, "<html><head><title>${page.name}</title></head><body><h1>${titleize(page.name)}</h1><div>${{\n\n\tlet self = $.th", "", 0, 0));

		// Non-Script block, no results
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "<html><head><title>${page.name}</title></head><body><h1>${titleize(page.name)}</h1><div>${{\n\n\tlet self = $.this;}n}}</div>this.", "", 0, 0));
	}

	@Test
	public void testStructrscriptAutocomplete() {

		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${", "", 0, 0));
		assertFirstResult("text", "localize(key [, domain ])", AbstractHintProvider.getHints(securityContext, false, null, "${locali", "", 0, 0));

		// patterns that should produce the full list of autocomplete results
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${\n\t", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${\n\tcontains(", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${contains(", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${contains(", ")", 0, 0));

		// current is at least an AbstractNode
		assertFirstResult("text", "base",      AbstractHintProvider.getHints(securityContext, false, null, "${current.", "", 0, 0));
		assertFirstResult("text", "createdBy", AbstractHintProvider.getHints(securityContext, false, null, "${current.c", "", 0, 0));

		// patterns that should not produce any autocomplete results
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${cu.", "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${localize(.loc", "", 0, 0));

		// verify that autocomplete is disabled for cursor positions inside a string
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${", "x", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${contains(", "x", 0, 0));
	}

	@Test
	public void testJavascriptAutocomplete() {

		// patterns that should produce the full list of autocomplete results
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{$.", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\tStructr.", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{Structr.", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\tlet test = $.", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.contains($.", "", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.contains($.", " ", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.contains($.", ";", 0, 0));
		assertFullResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.contains($.", ")", 0, 0));

		// current is at least an AbstractNode
		assertFirstResult("text", "base",      AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.current.", "", 0, 0));
		assertFirstResult("text", "createdBy", AbstractHintProvider.getHints(securityContext, false, null, "${{\n\tlet test = $.current.c", "", 0, 0));

		// patterns that should not produce any autocomplete results
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.cu.", "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.contains(", "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t $.localize(.loc", "", 0, 0));

		// verify that autocomplete is disabled for cursor positions inside a string
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.", "x", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{$.", "x", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\tStructr.", "x", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{Structr.", "x", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\tlet test = $.", "x", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.contains($.", "x", 0, 0));
	}

	@Test
	public void testJavascriptAutocompleteForKeywords() {

		assertFirstResult("text", "base",    AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.page.",  "", 0, 0));
		assertFirstResult("text", "name",    AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.page.n", "", 0, 0));

		assertFirstResult("text", "base",    AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.me.",   "", 0, 0));
		assertFirstResult("text", "isAdmin", AbstractHintProvider.getHints(securityContext, false, null, "${{\n\t$.me.isA", "", 0, 0));
	}

	@Test
	public void testStructrscriptAutocompleteForKeywords() {

		assertFirstResult("text", "base",    AbstractHintProvider.getHints(securityContext, false, null, "${page.",  "", 0, 0));
		assertFirstResult("text", "name",    AbstractHintProvider.getHints(securityContext, false, null, "${page.n", "", 0, 0));

		assertFirstResult("text", "base",    AbstractHintProvider.getHints(securityContext, false, null, "${me.",   "", 0, 0));
		assertFirstResult("text", "isAdmin", AbstractHintProvider.getHints(securityContext, false, null, "${me.isA", "", 0, 0));
	}

	@Test
	public void testJavascriptAutocompleteFunctionContextHints() {

		assertFirstResult("text", "'A'", AbstractHintProvider.getHints(securityContext, false, null, "${{ $.find(",  "", 0, 0));
		assertFirstResult("text", "'A'", AbstractHintProvider.getHints(securityContext, false, null, "${{ $.find('",  "", 0, 0));
		assertFirstResult("text", "\"A\"", AbstractHintProvider.getHints(securityContext, false, null, "${{ $.find(\"",  "", 0, 0));
		assertFirstResult("text", "'User'", AbstractHintProvider.getHints(securityContext, false, null, "${{ $.find('Us",  "", 0, 0));
		assertFirstResult("text", "\"User\"", AbstractHintProvider.getHints(securityContext, false, null, "${{ $.find(\"Us",  "", 0, 0));

		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{ $.find('User'",  "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${{ $.find(\"User\"",  "", 0, 0));
	}

	@Test
	public void testStructrscriptAutocompleteFunctionContextHints() {

		assertFirstResult("text", "'A'", AbstractHintProvider.getHints(securityContext, false, null, "${find(",  "", 0, 0));
		assertFirstResult("text", "'A'", AbstractHintProvider.getHints(securityContext, false, null, "${find('",  "", 0, 0));
		assertFirstResult("text", "\"A\"", AbstractHintProvider.getHints(securityContext, false, null, "${find(\"",  "", 0, 0));
		assertFirstResult("text", "'User'", AbstractHintProvider.getHints(securityContext, false, null, "${find('Us",  "", 0, 0));
		assertFirstResult("text", "\"User\"", AbstractHintProvider.getHints(securityContext, false, null, "${find(\"Us",  "", 0, 0));
		assertFirstResult("text", "'User'", AbstractHintProvider.getHints(securityContext, false, null, "${find('User",  "", 0, 0));
		assertFirstResult("text", "\"User\"", AbstractHintProvider.getHints(securityContext, false, null, "${find(\"User",  "", 0, 0));

		// no results, but no error either!
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${find(\"User\"",  "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${find(\"User\"\"",  "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${find('User'",  "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${find('User''", "", 0, 0));
		assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, null, "${find('User', { ", "", 0, 0));
	}

	@Test
	public void testJavascriptAutocompleteWithMethod() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addStringProperty("description");
			type.addMethod("testMethod", "{ return true; }", "");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final SchemaMethod method = app.nodeQuery(SchemaMethod.class).and(SchemaMethod.name, "testMethod").getFirst();

			// verify that the properties of the Test type are in the autocomplete result
			final List<GraphObject> result1 = AbstractHintProvider.getHints(securityContext, false, method, "{\n\t$.this.", "", 0, 0);
			final Map<String, Object> base        = ((GraphObjectMap)result1.get(0)).toMap();
			final Map<String, Object> createdBy   = ((GraphObjectMap)result1.get(1)).toMap();
			final Map<String, Object> createdDate = ((GraphObjectMap)result1.get(2)).toMap();
			final Map<String, Object> description = ((GraphObjectMap)result1.get(3)).toMap();
			final Map<String, Object> id          = ((GraphObjectMap)result1.get(4)).toMap();
			assertEquals("Invalid autocomplete result", "base",        base.get("text"));
			assertEquals("Invalid autocomplete result", "createdBy",   createdBy.get("text"));
			assertEquals("Invalid autocomplete result", "createdDate", createdDate.get("text"));
			assertEquals("Invalid autocomplete result", "description", description.get("text"));
			assertEquals("Invalid autocomplete result", "id",          id.get("text"));

			// verify that the letter "d" is correctly expanded into "description"
			assertFirstResult("text", "description", AbstractHintProvider.getHints(securityContext, false, method, "{\n\t$.this.d", "", 0, 0));

			// verify that an opening brace resets the results
			assertFullResult(AbstractHintProvider.getHints(securityContext, false, method, "{\n\t$.contains($.", "", 0, 0));

			// verify that this. does not produce results
			assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, method, "{\n\tthis.", "", 0, 0));

			assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, method, "{\n\t$.this.abc", "", 0, 0));
			assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, method, "{\n\t$.this.ma.", "", 0, 0));
			assertEmptyResult(AbstractHintProvider.getHints(securityContext, false, method, "{\n\tlet test = $.contains(t", "", 0, 0));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	// ----- private methods -----
	void assertFullResult(final List<GraphObject> result) {

		final Map<String, Object> current  = ((GraphObjectMap)result.get(0)).toMap();
		final Map<String, Object> locale   = ((GraphObjectMap)result.get(1)).toMap();
		final Map<String, Object> me       = ((GraphObjectMap)result.get(2)).toMap();
		final Map<String, Object> page     = ((GraphObjectMap)result.get(3)).toMap();
		final Map<String, Object> request  = ((GraphObjectMap)result.get(4)).toMap();
		final Map<String, Object> response = ((GraphObjectMap)result.get(5)).toMap();
		final Map<String, Object> thisObj  = ((GraphObjectMap)result.get(6)).toMap();
		final Map<String, Object> delete   = ((GraphObjectMap)result.get(7)).toMap();
		final Map<String, Object> get      = ((GraphObjectMap)result.get(8)).toMap();
		final Map<String, Object> head     = ((GraphObjectMap)result.get(9)).toMap();

		assertEquals("Invalid autocomplete result", "current",                                          current.get("text"));
		assertEquals("Invalid autocomplete result", "locale",                                           locale.get("text"));
		assertEquals("Invalid autocomplete result", "me",                                               me.get("text"));
		assertEquals("Invalid autocomplete result", "page",                                             page.get("text"));
		assertEquals("Invalid autocomplete result", "request",                                          request.get("text"));
		assertEquals("Invalid autocomplete result", "response",                                         response.get("text"));
		assertEquals("Invalid autocomplete result", "page",                                             page.get("text"));
		assertEquals("Invalid autocomplete result", "this",                                             thisObj.get("text"));
		assertEquals("Invalid autocomplete result", "DELETE(url [, contentType ])",                     delete.get("text"));
		assertEquals("Invalid autocomplete result", "GET(url [, contentType [, username, password] ])", get.get("text"));
		assertEquals("Invalid autocomplete result", "HEAD(url [, username, password ])",                head.get("text"));

	}

	void assertFirstResult(final String key, final String value, final List<GraphObject> result) {

		final Map<String, Object> data  = ((GraphObjectMap)result.get(0)).toMap();
		assertEquals("Invalid autocomplete result", value, data.get(key));
	}

	void assertEmptyResult(final List<GraphObject> result) {
		assertEquals("Invalid autocomplete result", 0, result.size());
	}
}