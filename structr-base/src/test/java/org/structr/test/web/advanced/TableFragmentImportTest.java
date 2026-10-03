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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.Importer;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * A fragment whose outermost element is a part of a table only parses correctly in the context that
 * part lives in. Parsed as an ordinary body fragment, the parser drops every cell and keeps only its
 * text; parsed in a bare table context, a row comes back wrapped in an implicit tbody.
 *
 * <p>The importer recognises such fragments by a regular expression, which for a long time could not
 * match at all: the escaping was doubled, td was missing, and without DOTALL a fragment spanning more
 * than one line never matched either.
 */
public class TableFragmentImportTest extends StructrUiTest {

	// ----- recognition -----

	@Test
	public void testEveryTablePartIsRecognised() {

		assertTablePart("tr",       "<tr><td>a</td></tr>");
		assertTablePart("td",       "<td>a</td>");
		assertTablePart("th",       "<th>h</th>");
		assertTablePart("thead",    "<thead><tr><th>h</th></tr></thead>");
		assertTablePart("tbody",    "<tbody><tr><td>a</td></tr></tbody>");
		assertTablePart("tfoot",    "<tfoot><tr><td>f</td></tr></tfoot>");
		assertTablePart("caption",  "<caption>c</caption>");
		assertTablePart("colgroup", "<colgroup><col></colgroup>");
	}

	@Test
	public void testLeadingWhitespaceAndLineBreaksAreAccepted() {

		assertTablePart("tr", "\n\t  <tr>\n\t\t<td>a</td>\n\t</tr>\n");
		assertTablePart("td", "   <td class=\"x\">\n a \n</td>");
	}

	@Test
	public void testTagNamesAreCaseInsensitiveAndReportedInLowercase() {

		// Widget compares the reported name with the lowercased node type, so it must be lowercase
		assertTablePart("tr",    "<TR><TD>a</TD></TR>");
		assertTablePart("thead", "<THead><tr><th>h</th></tr></THead>");
	}

	@Test
	public void testOrdinaryFragmentsAreNotTableParts() {

		assertNotTablePart("<div><span>x</span></div>");
		assertNotTablePart("<table><tr><td>a</td></tr></table>");
		assertNotTablePart("text before <tr><td>a</td></tr>");
	}

	@Test
	public void testLookalikeTagsAreNotTableParts() {

		// a prefix of the tag name is not the tag
		assertNotTablePart("<track src=\"x.vtt\">");
		assertNotTablePart("<thing></thing>");
		assertNotTablePart("<tdx>a</tdx>");
		assertNotTablePart("<theadline>a</theadline>");
	}

	// ----- import result -----

	@Test
	public void testCellsAreImportedAsCellsAndNotAsTheirText() {

		// the reported bug: imported outside a table context, only "a" and "b" were left
		assertEquals("<tr><td>a</td><td>b</td></tr>", importInto("tr", "<td>a</td><td>b</td>"));
	}

	@Test
	public void testHeaderCellsAreImportedIntoARow() {

		assertEquals("<tr><th>h1</th><th>h2</th></tr>", importInto("tr", "<th>h1</th><th>h2</th>"));
	}

	@Test
	public void testARowIsImportedWithoutAnExtraTableOrBody() {

		assertEquals("<tbody><tr><td>a</td><td>b</td></tr></tbody>", importInto("tbody", "<tr><td>a</td><td>b</td></tr>"));
	}

	@Test
	public void testSeveralRowsAreImportedInOrder() {

		assertEquals("<tbody><tr><td>1</td></tr><tr><td>2</td></tr><tr><td>3</td></tr></tbody>", importInto("tbody", "<tr><td>1</td></tr><tr><td>2</td></tr><tr><td>3</td></tr>"));
	}

	@Test
	public void testTableSectionsAreImportedIntoATable() {

		assertEquals("<table><thead><tr><th>h</th></tr></thead></table>",     importInto("table", "<thead><tr><th>h</th></tr></thead>"));
		assertEquals("<table><tbody><tr><td>a</td></tr></tbody></table>",     importInto("table", "<tbody><tr><td>a</td></tr></tbody>"));
		assertEquals("<table><tfoot><tr><td>f</td></tr></tfoot></table>",     importInto("table", "<tfoot><tr><td>f</td></tr></tfoot>"));
		assertEquals("<table><caption>c</caption></table>",                   importInto("table", "<caption>c</caption>"));
	}

	@Test
	public void testAColumnGroupKeepsItsColumns() {

		final String html = importInto("table", "<colgroup><col><col></colgroup>");

		assertTrue("the column group must be imported: " + html, html.startsWith("<table><colgroup><col"));
		assertEquals("both columns must be imported: " + html, 2, html.split("<col[ >/]", -1).length - 1);
	}

	@Test
	public void testAMultiLineFragmentIsImportedLikeASingleLineOne() {

		final String source = """

			<tr>
				<td>a</td>
				<td>b</td>
			</tr>
			""";

		assertEquals("<tbody><tr><td>a</td><td>b</td></tr></tbody>", importInto("tbody", source));
	}

	@Test
	public void testAnUppercaseFragmentIsImportedInLowercase() {

		assertEquals("<tr><td>a</td><td>b</td></tr>", importInto("tr", "<TD>a</TD><TD>b</TD>"));
	}

	@Test
	public void testCellAttributesAndNestedMarkupSurvive() {

		final String html = importInto("tr", "<td class=\"num\" colspan=\"2\"><b>42</b></td>");

		assertTrue("the cell must keep its attributes: " + html, html.contains("class=\"num\"") && html.contains("colspan=\"2\""));
		assertTrue("the cell must keep its content: " + html,    html.contains("<b>42</b>"));
	}

	@Test
	public void testOrdinaryFragmentsAreImportedAsBefore() {

		assertEquals("<div><p>x</p><span>y</span></div>", importInto("div", "<p>x</p><span>y</span>"));
	}

	// ----- through the scripting function that reaches this code path -----

	@Test
	public void testImportHtmlFunctionImportsCellsIntoARow() {

		try (final Tx tx = app.tx()) {

			final DOMNode row           = createContainer("tr");
			final ActionContext context = new ActionContext(securityContext);

			context.setConstant("row", row);

			Scripting.evaluate(context, null, "${importHtml(row, '<td>a</td><td>b</td>')}", "test");

			assertEquals("<tr><td>a</td><td>b</td></tr>", render(row));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- private methods -----

	private void assertTablePart(final String expectedTag, final String source) {

		assertEquals("wrong table part recognised for " + source.strip(), expectedTag, parse(source).getTableChildElement());
	}

	private void assertNotTablePart(final String source) {

		assertNull("no table part must be recognised for " + source.strip(), parse(source).getTableChildElement());
	}

	private Importer parse(final String source) {

		try (final Tx tx = app.tx()) {

			final Importer importer = new Importer(securityContext, source, null, null, false, false, false, false);

			importer.parse(true);

			tx.success();

			return importer;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		return null;
	}

	/**
	 * Imports the source as a fragment into a fresh element with the given tag, the way importHtml()
	 * does, and returns that element's markup with the whitespace between tags removed.
	 */
	private String importInto(final String containerTag, final String source) {

		try (final Tx tx = app.tx()) {

			final DOMNode container = createContainer(containerTag);
			final Importer importer = new Importer(securityContext, source, null, null, false, false, false, false);

			importer.parse(true);
			importer.createChildNodes(container, container.getOwnerDocument(), true);

			final String html = render(container);

			tx.success();

			return html;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		return null;
	}

	private DOMNode createContainer(final String tag) throws FrameworkException {

		final Page page         = Page.createSimplePage(securityContext, "tables-" + System.nanoTime());
		final DOMNode body      = page.getElementsByTagName("body").get(0);
		final DOMNode container = page.createElement(tag);

		body.appendChild(container);

		return container;
	}

	private String render(final DOMNode node) throws FrameworkException {

		return node.getContent(RenderContext.EditMode.NONE).replaceAll(">\\s+<", "><").strip();
	}
}
