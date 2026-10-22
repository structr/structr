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
package org.structr.test.web.basic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.restassured.RestAssured;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * A page whose main area rewrites its links with a regular expression literal containing an odd number of quotes:
 * the scanner used to read the quotes as strings, never found the end of the block, and the main area and
 * everything after it rendered without being evaluated.
 */
public class TemplateRegexLiteralTest extends StructrUiTest {

	private String templateId = null;

	@Test
	public void testBlockWithQuotesInARegularExpressionIsRendered() {

		// the production form: a quote inside the regular expression, and no further slash after it on the line
		final String main = """
			<main>${{
				const html = '<a href="/news">News</a>';
				html.replace(/href="([^"]+)'/g, 'x').replace(/<a href="/g, '<a class="link" href="');
			}}</main>""";

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "links");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.appendChild(template);

			template.setContent(main + "<footer>${concat('Foot', 'er')}</footer>");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/html");

			makePublic(page, template);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		final String html = RestAssured.given().expect().statusCode(200).when().get("/links").getBody().asString();

		assertTrue(html, html.contains("<main><a class=\"link\" href=\"/news\">News</a></main>"));
		assertTrue("the expression after the block is evaluated too: " + html, html.contains("<footer>Footer</footer>"));
		assertFalse("no unevaluated expression is left in the page: " + html, html.contains("${"));
	}

	@Test
	public void testDivisionIsNotAComment() {

		// the second slash used to start a // comment, so the block never closed
		assertEquals("<p>1|2</p>", render("division", "<p>${{ 6 / 2 / 3 }}|${{ const a = 8; a / 4 }}</p>"));
	}

	@Test
	public void testUnclosedBlockInATemplateIsLogged() {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(ContentTraitDefinition.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			render("unclosed", "<p>${{ $.log('unclosed); }}</p>");

		} finally {

			logger.detachAppender(appender);
		}

		final List<String> warnings = appender.list.stream().filter(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("is never closed")).map(ILoggingEvent::getFormattedMessage).toList();

		assertEquals(warnings.toString(), 1, warnings.size());
		assertTrue(warnings.get(0), warnings.get(0).contains("Template " + templateId));
		assertTrue(warnings.get(0), warnings.get(0).contains("inside a single-quoted string"));
	}

	// ----- private methods -----
	private String render(final String pageName, final String content) {

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, pageName);
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.appendChild(template);

			template.setContent(content);
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/html");

			makePublic(page, template);

			templateId = template.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		return RestAssured.given().expect().statusCode(200).when().get("/" + pageName).getBody().asString().trim();
	}
}
