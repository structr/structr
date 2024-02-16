/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.diff.InvertibleModificationOperation;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Div;
import org.structr.web.importer.Importer;
import org.testng.annotations.Test;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class DiffTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(DiffTest.class.getName());

	@Test
	public void testReplaceContent() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", (String from) -> from.replace("Test", "Wurst"));

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>Wurst</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testInsertHeading() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", (String from) -> from.replace("Test", "<h1>Title text</h1>"));

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>\n" +
			"		<h1>Title text</h1>\n" +
			"	</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testInsertDivBranch() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", (String from) -> from.replace("Test", "<div><h1>Title text</h1></div>"));

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>\n" +
			"		<div>\n" +
			"			<h1>Title text</h1>\n" +
			"		</div>\n" +
			"	</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testInsertDivBranch2() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", (String from) -> from.replace("Test", "<div><div><h1>Title text</h1><p>paragraph</p></div></div>"));

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>\n" +
			"		<div>\n" +
			"			<div>\n" +
			"				<h1>Title text</h1>\n" +
			"				<p>paragraph</p>\n" +
			"			</div>\n" +
			"		</div>\n" +
			"	</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testInsertMultipleTextNodes() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test</body></html>", (String from) -> from.replace("Test", "Test<b>bold</b>between<i>italic</i>Text"));

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>Test<b>bold</b>between<i>italic</i>Text</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testModifyMultipleTextNodes2() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body>Test<b>bold</b>between<i>italic</i>Text</body></html>", (String from) -> {
			String mod = from;

			mod = mod.replace("bold", "BOLD");
			mod = mod.replace("between", "BETWEEN");
			mod = mod.replace("italic", "ITALIC");
			mod = mod.replace("Text", "abcdef");

			return mod;
		});

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>Test<b>BOLD</b>BETWEEN<i>ITALIC</i>abcdef</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testReparentOneLevel() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1></body></html>", (String from) -> {
			final StringBuilder buf = new StringBuilder(from);

			int startPos = buf.indexOf("<h1");
			int endPos   = buf.indexOf("</h1>") + 5;

			// insert from back to front, otherwise insert position changes
			buf.insert(endPos, "</div>");
			buf.insert(startPos, "<div>");

			return buf.toString();
		});

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>\n" +
			"		<div>\n" +
			"			<h1>Title text</h1>\n" +
			"		</div>\n" +
			"	</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testReparentTwoLevels() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1></body></html>", (String from) -> {
			final StringBuilder buf = new StringBuilder(from);

			int startPos = buf.indexOf("<h1");
			int endPos   = buf.indexOf("</h1>") + 5;

			// insert from back to front, otherwise insert position changes
			buf.insert(endPos, "</div></div>");
			buf.insert(startPos, "<div><div>");

			return buf.toString();
		});

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>\n" +
			"		<div>\n" +
			"			<div>\n" +
			"				<h1>Title text</h1>\n" +
			"			</div>\n" +
			"		</div>\n" +
			"	</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testReparentThreeLevels() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1></body></html>", (String from) -> {
			final StringBuilder buf = new StringBuilder(from);

			int startPos = buf.indexOf("<h1");
			int endPos   = buf.indexOf("</h1>") + 5;

			// insert from back to front, otherwise insert position changes
			buf.insert(endPos, "</div></div></div>");
			buf.insert(startPos, "<div><div><div>");

			return buf.toString();
		});

		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>\n" +
			"		<div>\n" +
			"			<div>\n" +
			"				<div>\n" +
			"					<h1>Title text</h1>\n" +
			"				</div>\n" +
			"			</div>\n" +
			"		</div>\n" +
			"	</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testMove() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>Title text</h1><div><h2>subtitle</h2></div></body></html>", (String from) -> {
			final StringBuilder buf = new StringBuilder(from);

			int startPos = buf.indexOf("<h1");
			int endPos   = buf.indexOf("</h1>") + 5;

			// cut out <h1> block
			final String toMove = buf.substring(startPos, endPos);
			buf.replace(startPos, endPos, "");

			// insert after <h2>
			int insertPos = buf.indexOf("</h2>") + 5;

			// insert from back to front, otherwise insert position changes
			buf.insert(insertPos, toMove);

			return buf.toString();
		});


		compare(
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"	<head>\n" +
			"		<title>Title</title>\n" +
			"	</head>\n" +
			"	<body>\n" +
			"		<div>\n" +
			"			<h2>subtitle</h2>\n" +
			"			<h1>Title text</h1>\n" +
			"		</div>\n" +
			"	</body>\n" +
			"</html>",
			result1
		);
	}

	@Test
	public void testSwap() {

		final StringBuilder clipboard = new StringBuilder();

		final String result1 = testDiff("<html><head><title>Title</title></head><body><div><h2>one</h2></div><div><h2>two</h2></div><div><h2>three</h2></div><div><h2>four</h2></div></body></html>", (String from) -> {
			final StringBuilder buf = new StringBuilder(from);

			// cut out <div> block
			int cutStart = buf.indexOf("<h2") - ("data-structr-hash".length() + 46);
			int cutEnd = buf.indexOf("</h2>") + 16;

			clipboard.append(buf.substring(cutStart, cutEnd));
			buf.replace(cutStart, cutEnd, "");

			int insert = buf.indexOf("</h2>") + 16;

			buf.insert(insert, clipboard.toString());

			return buf.toString();
		});

		compare(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head>\n"
			+ "		<title>Title</title>\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<div>\n"
			+ "			<h2>two</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>one</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>three</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>four</h2>\n"
			+ "		</div>\n"
			+ "	</body>\n"
			+ "</html>",
			result1
		);
	}

	@Test
	public void testAddAttributes() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><div><h2>one</h2></div><div><h2>two</h2></div><div><h2>three</h2></div><div><h2>four</h2></div></body></html>", (String from) -> {
			final StringBuilder buf = new StringBuilder(from);

			int insert = buf.indexOf("<div ") + 5;

			buf.insert(insert, " class='test' id='one' ");

			return buf.toString();
		});

		compare(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head>\n"
			+ "		<title>Title</title>\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<div class=\"test\" id=\"one\">\n"
			+ "			<h2>one</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>two</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>three</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>four</h2>\n"
			+ "		</div>\n"
			+ "	</body>\n"
			+ "</html>",
			result1
		);
	}

	@Test
	public void testModifyRemoveAttributes() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><div><h2 class=\"test\" id=\"one\">one</h2></div><div><h2>two</h2></div><div><h2 id=\"three\">three</h2></div><div><h2>four</h2></div></body></html>", (String from) -> {
			String modified = from;

			modified = modified.replace(" class=\"test\"", " class=\"foo\"");
			modified = modified.replace(" id=\"one\"", " id=\"two\"");
			modified = modified.replace(" id=\"three\"", " class=\"test\"");

			return modified;
		});

		compare(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head>\n"
			+ "		<title>Title</title>\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<div>\n"
			+ "			<h2 id=\"two\" class=\"foo\">one</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>two</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2 class=\"test\">three</h2>\n"
			+ "		</div>\n"
			+ "		<div>\n"
			+ "			<h2>four</h2>\n"
			+ "		</div>\n"
			+ "	</body>\n"
			+ "</html>",
			result1
		);
	}

	@Test
	public void testBlockMoveUp() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><div>Text<h2>one</h2><p>two</p></div></body></html>", (String from) -> {
			final StringBuilder clipboard = new StringBuilder();
			final StringBuilder buf = new StringBuilder(from);

			int cutStart = buf.indexOf("<h2");
			int cutEnd = buf.indexOf("</p>") + 7;

			// cut out <h1> block
			clipboard.append(buf.substring(cutStart, cutEnd));
			buf.replace(cutStart, cutEnd, "");

			int insert = buf.indexOf("<div");
			buf.insert(insert, clipboard.toString());

			return buf.toString();
		});

		System.out.println(result1);

		compare(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head>\n"
			+ "		<title>Title</title>\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<h2>one</h2>\n"
			+ "		<p>two</p>\n"
			+ "		<div>Text </div>\n"
			+ "	</body>\n"
			+ "</html>",
			result1
		);
	}

	@Test
	public void testBlockMoveDown() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><div>Text<h2>one</h2><div><p>two</p></div></div></body></html>", (String from) -> {
			final StringBuilder clipboard = new StringBuilder();
			final StringBuilder buf = new StringBuilder(from);

			int cutStart = buf.indexOf("<h2");
			int cutEnd = buf.indexOf("</h2>") + 5;

			// cut out <h1> block
			clipboard.append(buf.substring(cutStart, cutEnd));
			buf.replace(cutStart, cutEnd, "");

			int insert = buf.indexOf("<p ");

			buf.insert(insert, clipboard.toString());

			System.out.println(buf.toString());

			return buf.toString();
		});

		System.out.println(result1);

		compare(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head>\n"
			+ "		<title>Title</title>\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<div>Text \n"
			+ "			<div>\n"
			+ "				<h2>one</h2>\n"
			+ "				<p>two</p>\n"
			+ "			</div>\n"
			+ "		</div>\n"
			+ "	</body>\n"
			+ "</html>",
			result1
		);
	}

	@Test
	public void testSurroundBlock() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>title</h1><p>text</p></body></html>", (String from) -> {
			final StringBuilder buf = new StringBuilder(from);

			int insertStart = buf.indexOf("<h1");
			int insertEnd = buf.indexOf("</p>") + 4;

			buf.insert(insertEnd, "</div>");
			buf.insert(insertStart, "<div>");

			return buf.toString();
		});

		System.out.println(result1);

		compare(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head>\n"
			+ "		<title>Title</title>\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<div>\n"
			+ "			<h1>title</h1>\n"
			+ "			<p>text</p>\n"
			+ "		</div>\n"
			+ "	</body>\n"
			+ "</html>",
			result1
		);
	}

	@Test
	public void testModifyTag() {

		final String result1 = testDiff("<html><head><title>Title</title></head><body><h1>title</h1><p>text</p></body></html>", (String from) -> {
			String modified = from;

			modified = modified.replace("<h1 ", "<h2 ");
			modified = modified.replace("</h1>", "</h2>");

			modified = modified.replace("<p ", "<a ");
			modified = modified.replace("</p>", "</a>");

			return modified;
		});

		System.out.println(result1);

		compare(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "	<head>\n"
			+ "		<title>Title</title>\n"
			+ "	</head>\n"
			+ "	<body>\n"
			+ "		<h2>title</h2>\n"
			+ "		<a>text</a>\n"
			+ "	</body>\n"
			+ "</html>",
			result1
		);
	}

	@Test
	public void testTreeRemovalFix() {

		final String comment = "<!-- comment --->";

		testDiff("<html><head><title>Title</title></head><body><div>" + comment + "<div>" + comment + "<div>test</div>" + comment + "<div></div></div></body></html>", (String from) -> {
			String modified = from;

			modified = modified.replace(comment, "");

			return modified;
		});

		// test result on the node level
		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(Page.class).andName("test").getFirst();

			assertNotNull(page);

			final NodeList nodes = page.getElementsByTagName("div");
			final List<Div> divs = collectNodes(nodes, Div.class);

			assertEquals("Wrong number of divs returned from node query", 4, divs.size());

			// check first div, should have no siblings and one child
			final Div firstDiv = divs.get(0);
			assertEquals("Wrong number of children", 1, firstDiv.getChildRelationships().size());
			assertNull("Node should not have siblings", firstDiv.getNextSibling());

			// check second div, should have no siblings and two children
			final Div secondDiv = divs.get(1);
			assertEquals("Wrong number of children", 2, secondDiv.getChildRelationships().size());
			assertNull("Node should not have siblings", secondDiv.getNextSibling());

			// check third div, should have one sibling and one #text child
			final Div thirdDiv = divs.get(2);
			assertEquals("Wrong number of children", 1, thirdDiv.getChildRelationships().size());
			assertNotNull("Node should have one sibling", thirdDiv.getNextSibling());

			// check fourth div, should have no siblings and no children
			final Div fourthDiv = divs.get(3);
			assertEquals("Wrong number of children", 0, fourthDiv.getChildRelationships().size());
			assertNull("Node should not have siblings", fourthDiv.getNextSibling());







		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}
	}








	private String testDiff(final String source, final Function<String, String> modifier) {

		Settings.JsonIndentation.setValue(true);
		Settings.HtmlIndentation.setValue(true);

		final StringBuilder buf = new StringBuilder();
		String sourceHtml = null;

		try {

			// create page from source
			final Page sourcePage = Importer.parsePageFromSource(securityContext, source, "test");

			// render page into HTML string
			try (final Tx tx = app.tx()) {
				sourceHtml = sourcePage.getContent(RenderContext.EditMode.RAW);
				tx.success();
			}

			// modify HTML string with transformation function
			final String modifiedHtml = modifier.apply(sourceHtml);

			// parse page from modified source
			final Page modifiedPage = Importer.parsePageFromSource(securityContext, modifiedHtml, "Test");

			// create and apply diff operations
			try (final Tx tx = app.tx()) {

				final List<InvertibleModificationOperation> changeSet = Importer.diffNodes(sourcePage, modifiedPage);
				for (final InvertibleModificationOperation op : changeSet) {

					System.out.println(op);

					// execute operation
					op.apply(app, sourcePage, modifiedPage);

					System.out.println("############################################################################################");
//					System.out.println(sourcePage.getContent(RenderContext.EditMode.NONE));
				}

				tx.success();
			}

			// render modified page into buffer
			try (final Tx tx = app.tx()) {
				buf.append(sourcePage.getContent(RenderContext.EditMode.NONE));
				tx.success();
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return buf.toString();
	}


	private <T> List<T> collectNodes(final NodeList source, final Class<T> type) {

		final List<T> list = new ArrayList<>();
		final int len      = source.getLength();

		for (int i=0; i<len; i++) {

			list.add((T)source.item(i));
		}

		return list;
	}

	private void compare(final String expected, final String actual) {

		// creates a flat list of all nodes that the given documents contain
		// and compares the lists node by node in order to avoid different
		// attribute orders to break the tests

		final List<Node> expectedNodes = new ArrayList<>();
		final List<Node> actualNodes   = new ArrayList<>();

		collectNodes(expectedNodes, Jsoup.parse(expected));
		collectNodes(actualNodes, Jsoup.parse(actual));

		for (int i=0; i < expectedNodes.size(); i++) {

			final Node expectedNode = expectedNodes.get(i);
			final Node actualNode   = actualNodes.get(i);
			final String acName     = actualNode.nodeName();

			assertEquals("Tag name mismatch", expectedNode.nodeName(), actualNode.nodeName());
			assertEquals("Attribute mismatch in " + acName, expectedNode.attributes(), actualNode.attributes());
		}
	}

	private void collectNodes(final List<Node> target, final Node current) {

		target.add(current);

		for (final Node child : current.childNodes()) {
			collectNodes(target, child);
		}
	}
}
