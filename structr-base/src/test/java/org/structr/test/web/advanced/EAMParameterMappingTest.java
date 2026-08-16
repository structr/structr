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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.ParameterMappingTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Integration tests for EAM parameter mapping rendering.
 *
 * Covers all four ParameterType cases (ConstantValue, UserInput, ScriptExpression,
 * PageParam) and all four pagination EventAction variants (prev-page, next-page,
 * first-page, last-page).  Each test creates a minimal page with a trigger button,
 * wires an ActionMapping + one or more ParameterMappings, fetches the rendered HTML
 * and asserts the expected data-* attributes on the button element.
 */
public class EAMParameterMappingTest extends StructrUiTest {

	// -----------------------------------------------------------------------
	// ConstantValue
	// -----------------------------------------------------------------------

	@Test
	public void testConstantValueParameter() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("Submit"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "constant-value"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "myParam"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY), "hello world")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		// "myParam" → LOWER_CAMEL to LOWER_HYPHEN → "my-param"
		assertEquals("ConstantValue: data-my-param must contain the stored constant", "hello world", attrs.get("data-my-param"));
	}

	@Test
	public void testConstantValueParameterHtmlEscaping() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("Submit"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "constant-value"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "myParam"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY), "a & b")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		// Check the raw HTML to confirm the & was escaped — the rendered attribute
		// value must be valid HTML (i.e. the ampersand must be &amp;).
		assertTrue("ConstantValue: & must be escaped to &amp; in rendered HTML", html.contains("data-my-param=\"a &amp; b\""));

		// Jsoup decodes entities on parse, so the parsed attribute value is the original.
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");

		assertEquals("ConstantValue: parsed attribute value must equal the stored constant", "a & b", getAttributes(button).get("data-my-param"));
	}

	// -----------------------------------------------------------------------
	// UserInput
	// -----------------------------------------------------------------------

	@Test
	public void testUserInputParameterWithCssId() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page       = Page.createSimplePage(securityContext, "page1");
			final DOMNode div     = page.getElementsByTagName("div").get(0);
			final DOMElement btn  = page.createElement("button");
			final DOMElement input = page.createElement("input");

			div.appendChild(btn);
			div.appendChild(input);
			btn.appendChild(page.createTextNode("Submit"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");
			input.setProperty(Traits.of("Input").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "my-input");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "user-input"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "myParam"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY), input)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		// When the input element has a CSS id, use the css(#id) selector form.
		assertEquals("UserInput with CSS id: data-my-param must use css(#...) selector", "css(#my-input)", attrs.get("data-my-param"));
	}

	@Test
	public void testUserInputParameterWithoutCssId() {

		final String[] inputUuid = new String[1];

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page       = Page.createSimplePage(securityContext, "page1");
			final DOMNode div     = page.getElementsByTagName("div").get(0);
			final DOMElement btn  = page.createElement("button");
			final DOMElement input = page.createElement("input");

			div.appendChild(btn);
			div.appendChild(input);
			btn.appendChild(page.createTextNode("Submit"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");
			// Intentionally do NOT set an HTML id on the input element.
			inputUuid[0] = input.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "user-input"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "myParam"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY), input)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		// Without a CSS id, fall back to the id(uuid) form.
		assertEquals("UserInput without CSS id: data-my-param must use id(uuid) selector", "id(" + inputUuid[0] + ")", attrs.get("data-my-param"));

		// The id(uuid) form is only resolvable by the frontend if the input element itself renders
		// data-structr-id, which it does because it is the input source of a parameter mapping.
		final Element input = doc.selectFirst("input");

		assertNotNull("UserInput without CSS id: input element must be rendered", input);
		assertEquals("UserInput without CSS id: input element must render data-structr-id so that id(uuid) can be resolved", inputUuid[0], getAttributes(input).get("data-structr-id"));
	}

	// -----------------------------------------------------------------------
	// ScriptExpression
	// -----------------------------------------------------------------------

	@Test
	public void testScriptExpressionParameter() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("Submit"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// The expression ${upper("hello")} evaluates to "HELLO" at render time.
			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "script-expression"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "myParam"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY), "${upper(\"hello\")}")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		// The expression must be evaluated at render time.
		assertEquals("ScriptExpression: expression must be evaluated and result placed in data attribute", "HELLO", attrs.get("data-my-param"));
	}

	// -----------------------------------------------------------------------
	// PageParam — prev-page
	// -----------------------------------------------------------------------

	@Test
	public void testPageParamPrevPage() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("Prev"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "prev-page");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "page-param"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "page")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		// No ?page= request param → default page is 1 → prev = max(1, 1-1) = 1.
		assertEquals("PageParam prev-page: data-structr-target must be the parameter name", "page", attrs.get("data-structr-target"));
		assertEquals("PageParam prev-page: data-page must be max(1, currentPage-1)", "1", attrs.get("data-page"));
	}

	// -----------------------------------------------------------------------
	// PageParam — next-page
	// -----------------------------------------------------------------------

	@Test
	public void testPageParamNextPage() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("Next"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "next-page");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "page-param"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "page")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		// No ?page= request param → default page is 1 → next = 1+1 = 2.
		assertEquals("PageParam next-page: data-structr-target must be the parameter name", "page", attrs.get("data-structr-target"));
		assertEquals("PageParam next-page: data-page must be currentPage+1", "2", attrs.get("data-page"));
	}

	// -----------------------------------------------------------------------
	// PageParam — first-page
	// -----------------------------------------------------------------------

	@Test
	public void testPageParamFirstPage() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("First"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "first-page");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "page-param"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "page")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		assertEquals("PageParam first-page: data-structr-target must be the parameter name", "page", attrs.get("data-structr-target"));
		assertEquals("PageParam first-page: data-page must always be 1", "1", attrs.get("data-page"));
	}

	// -----------------------------------------------------------------------
	// PageParam — last-page
	// -----------------------------------------------------------------------

	@Test
	public void testPageParamLastPage() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("Last"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "last-page");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "page-param"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "page")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		assertEquals("PageParam last-page: data-structr-target must be the parameter name", "page", attrs.get("data-structr-target"));
		// Hardcoded sentinel value — see DOMElementTraitDefinition PageParam/LastPage branch.
		assertEquals("PageParam last-page: data-page must be the hardcoded sentinel 1000", "1000", attrs.get("data-page"));
	}

	// -----------------------------------------------------------------------
	// Multiple parameters on one EAM
	// -----------------------------------------------------------------------

	@Test
	public void testMultipleParameters() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);
			btn.appendChild(page.createTextNode("Submit"));
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "constant-value"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "firstName"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY), "Alice")
			);

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "constant-value"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "lastName"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY), "Smith")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";

		final String html    = fetchPageHtml("/html/page1");
		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		// Both parameters must appear as distinct data attributes (camelCase → hyphen).
		assertEquals("Multiple params: first-name must be rendered", "Alice", attrs.get("data-first-name"));
		assertEquals("Multiple params: last-name must be rendered", "Smith", attrs.get("data-last-name"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private Map<String, String> getAttributes(final Element element) {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final Attribute attr : element.attributes()) {

			map.put(attr.getKey(), attr.getValue());
		}

		return map;
	}

	private String fetchPageHtml(final String path) {

		return RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get(path)
			.andReturn()
				.body().asString();
	}
}
