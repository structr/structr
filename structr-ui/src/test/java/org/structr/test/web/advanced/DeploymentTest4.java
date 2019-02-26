/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

public class DeploymentTest4 extends DeploymentTestBase {

	private static final Logger logger = LoggerFactory.getLogger(DeploymentTest4.class.getName());

	@Test
	public void test41CustomAttributes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test41");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test41");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			div1.setProperty(new StringProperty("_custom_html_aria-expanded"), "true");
			div1.setProperty(new StringProperty("_custom_html_aria-controls"), "#test");
			div1.setProperty(new StringProperty("_custom_html_data-target"),   "#target");
			div1.setProperty(new StringProperty("_custom_html_data-node-id"),  "1233");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test42NamedContentElement() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test42");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test42");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			final Content content = createContent(page, div1, "my content text");
			content.setProperty(AbstractNode.name, "myNamedConentElement");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test43HiddenContentElement() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test43");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test43");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			final Content content = createContent(page, div1, "my content text");
			content.setProperty(AbstractNode.hidden, true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test44HiddenPage() {

		final String testName = "test44";

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   testName);
			page.setProperty(AbstractNode.hidden, true);

			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", testName);

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			createContent(page, div1, "my content text");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);


		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(Page.class).and(Page.name, testName).getFirst();

			assertTrue("Expected page to have the hidden flag!", page.isHidden());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test45Schema() {

		final String testName = "test45";

		// setup
		try (final Tx tx = app.tx()) {

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);

	}
}
