/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.Site;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 *
 *
 */
public class SitesTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(SitesTest.class.getName());

	public void testSites() {

		try (final Tx tx = app.tx()) {

			// setup two pages and two sites
			// page one -> site one, listens on one:8875
			// page two -> site two, listens on two:8875
			final Page pageOne = app.create(Page.class, "page-one");

			try {
				final Element html = pageOne.createElement("html");
				((DOMNode) html).setProperty(DOMNode.visibleToPublicUsers, true);

				final Text textNode = pageOne.createTextNode("page-1");
				((DOMNode) textNode).setProperty(DOMNode.visibleToPublicUsers, true);

				pageOne.appendChild(html);
				html.appendChild(textNode);

			} catch (DOMException dex) {
				logger.warn("", dex);
				throw new FrameworkException(422, dex.getMessage());
			}

			final Page pageTwo = app.create(Page.class, "page-two");

			try {
				final Element html = pageTwo.createElement("html");
				((DOMNode) html).setProperty(DOMNode.visibleToPublicUsers, true);
				
				final Text textNode = pageTwo.createTextNode("page-2");
				((DOMNode) textNode).setProperty(DOMNode.visibleToPublicUsers, true);

				pageTwo.appendChild(html);
				html.appendChild(textNode);

			} catch (DOMException dex) {
				logger.warn("", dex);
				throw new FrameworkException(422, dex.getMessage());
			}

			final Site siteOne = app.create(Site.class, "site-one");
			siteOne.setProperty(Site.visibleToPublicUsers, true);
			
			final Site siteTwo = app.create(Site.class, "site-two");
			siteTwo.setProperty(Site.visibleToPublicUsers, true);

			pageOne.setProperty(Page.site, siteOne);
			pageOne.setProperty(Page.visibleToPublicUsers, true);
			pageOne.setProperty(Page.position, 10);

			pageTwo.setProperty(Page.site, siteTwo);
			pageTwo.setProperty(Page.visibleToPublicUsers, true);
			pageTwo.setProperty(Page.position, 10);

			siteOne.setProperty(Site.hostname, "localhost");
			siteOne.setProperty(Site.port, 8875);

			siteTwo.setProperty(Site.hostname, "127.0.0.1");
			siteTwo.setProperty(Site.port, 8875);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}


		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("text/html")
			.statusCode(200)
			.body(Matchers.containsString("page-1"))
			.when()
			.get("http://localhost:8875/");

		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("text/html")
			.statusCode(200)
			.body(Matchers.containsString("page-2"))
			.when()
			.get("http://127.0.0.1:8875/");

	}

}
