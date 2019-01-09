/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.web.frontend.selenium;

import org.junit.*;

public class PagesAreaTest extends SeleniumTest {

	static {

		activeBrowser = SupportedBrowsers.FIREFOX;
	}

	@Test
	public void testCloneAndDeleteElement() throws Exception {

		login("admin", "admin");
		area("pages");

		// open pages tree flyout
		id("pagesTab").click();
		delay();

		id("add_page").click();
		delay(2000);

		Assert.assertEquals("Only one div should exist prior to cloning", 1, getCountOfType("Div"));

		// click "Clone node" icon on first <div> element in pages tree
		hover(xpath("//b[text()='div']"));
		xpath("//b[text()='div']/../i[3]").click();
		delay(2000);

		Assert.assertEquals("Two divs should exist after cloning", 2, getCountOfType("Div"));

		// delete cloned element
		hover(xpath("(//b[text()='div'])[2]"));
		xpath("(//b[text()='div'])[2]/../i[5]").click();
		delay(2000);

		// doesn't work, div element still exists in "unused elements"..
		//Assert.assertEquals("Only one div should exist after cloning and deletion", 1, getCountOfType("Div"));

		logout();
	}

	/* disabled, drag and drop does not work well..
	@Test
	public void testMoveElement() throws Exception {

		login("admin", "admin");
		area("pages");

		// open pages tree flyout
		id("pagesTab").click();
		delay();

		id("add_page").click();
		delay(2000);

		try (final Tx tx = app.tx()) {

			final Body body              = (Body)app.nodeQuery(StructrApp.getConfiguration().getNodeEntityClass("Body")).getFirst();
			final List<DOMNode> children = Iterables.toList((Iterable)body.getChildren());

			Assert.assertEquals("First child of <body> should be <h1> before drag and drop action",   "h1",  children.get(0).getProperty("tag"));
			Assert.assertEquals("Second child of <body> should be <div> before drag and drop action", "div", children.get(1).getProperty("tag"));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// drag div node upwards to change position with h1
		hover(xpath("//div[@id='id_" + getUuidOfFirstElement("Div") + "']"));
		dragAndDrop(xpath("//div[@id='id_" + getUuidOfFirstElement("Div") + "']"), 0, -40);
		delay(2000);

		try (final Tx tx = app.tx()) {

			final Body body              = (Body)app.nodeQuery(StructrApp.getConfiguration().getNodeEntityClass("Body")).getFirst();
			final List<DOMNode> children = Iterables.toList((Iterable)body.getChildren());

			Assert.assertEquals("First child of <body> should be <div> after drag and drop action", "div", children.get(0).getProperty("tag"));
			Assert.assertEquals("Second child of <body> should be <h1> after drag and drop action", "h1",  children.get(1).getProperty("tag"));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		logout();
	}
	*/
}
