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
import io.restassured.response.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.UriCompliance;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.rest.service.HttpService;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.PagePathTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Random;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/*
 * This class basically adds some tests to DynamicPathsTest for cases where URL Violations need to be allowed
 */
public class DynamicPathsTestWithAllowedViolations extends DeploymentTestBase {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		super.setup(testDatabaseConnection);

		final HttpService httpService = Services.getInstance().getServiceImplementation(HttpService.class);

		// set this after test setup because otherwise... fubar
		HttpService.UriComplianceAllowedViolations.setValue(String.join(" ", List.of(
				UriCompliance.Violation.AMBIGUOUS_EMPTY_SEGMENT.getName(),		// for empty parts "//"
				UriCompliance.Violation.AMBIGUOUS_PATH_SEPARATOR.getName(),		// for "%2f" => "/"
				UriCompliance.Violation.AMBIGUOUS_PATH_ENCODING.getName()		// for "%25" => "%"
		)));

		// attempt restart of HttpService to get the updated violations
		if (httpService != null) {

			httpPort = httpService.getAllocatedPort();
			final String serviceName = "HttpService";

			if (httpService.isRunning()) {

				httpService.shutdown();

				try {

					Services.getInstance().startService(serviceName);

				} catch (FrameworkException fex) {

					fail(fex.getMessage());
				}
			}

			Settings.HttpPort.setValue(httpPort);
		}
	}

	@Test
	public void test001DynamicPathResolution() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String notFoundPageContent = "404 NOT FOUND";

		try (final Tx tx = app.tx()) {

			// create 404 page
			{
				final Page errorPage = Page.createNewPage(securityContext, "404-page");
				errorPage.setProperty(errorPage.getTraits().key(PageTraitDefinition.SHOW_ON_ERROR_CODES_PROPERTY), "404");
				final Template errorTemplate = app.create(StructrTraits.TEMPLATE).as(Template.class);

				errorPage.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
				errorPage.appendChild(errorTemplate);

				errorTemplate.setContent(notFoundPageContent);
				errorTemplate.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			}

			final Page page         = Page.createNewPage(securityContext, "test001");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${key1},${key2},${key3}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test1/prefix_{key1}/{key2}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "defaultValue1")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key2"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Integer"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);
			}

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test2/{key1}_{key2}_{key3}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "defaultValue2")
				);
			}

			{
				app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test3/{key1}/{key2}/{key3}")
				);
			}

			{
				app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/{key1}/test4/{key2}/{key3}")
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// /test1/prefix_{key1}/{key2} with both parameters defined, default values "defaultValue1" and 1
		assertEquals("Invalid path resolution result", "one,5,",    getContent(200, "/structr/html/test1/prefix_one/5/three"));
		assertEquals("Invalid path resolution result", "one,1,",    getContent(200, "/structr/html/test1/prefix_one/two/three/four/five"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1/"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1//"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1///"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1////"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1/////"));
		assertEquals("Invalid path resolution result", "value1,1,", getContent(200, "/structr/html/test1/prefix_value1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1/value1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1/value1/1234"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test1/value1/two"));

		// /test2/{key1}_{key2}_{key3} with only one parameter defined, default value "defaultValue2"
		assertEquals("Invalid path resolution result", "one,two,three",           getContent(200, "/structr/html/test2/one_two_three"));
		assertEquals("Invalid path resolution result", "one_two_three,four,five", getContent(200, "/structr/html/test2/one_two_three_four_five"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2/"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2//"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2///"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2////"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2/////"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2/"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2/_"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",         getContent(200, "/structr/html/test2/__"));
		assertEquals("Invalid path resolution result", "_,,",                     getContent(200, "/structr/html/test2/___"));
		assertEquals("Invalid path resolution result", "__,,",                    getContent(200, "/structr/html/test2/____"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2/value1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2/value1/1234"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test2/value1/two"));

		// /test3/{key1}/{key2}/{key3} with no parameters defined
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/test3/one/two/three"));
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/test3/one/two/three/four/five"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test3"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test3/"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test3//"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test3///"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test3////"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test3/////"));
		assertEquals("Invalid path resolution result", "value1,,",      getContent(200, "/structr/html/test3/value1"));
		assertEquals("Invalid path resolution result", "value1,1234,",  getContent(200, "/structr/html/test3/value1/1234"));
		assertEquals("Invalid path resolution result", "value1,two,",   getContent(200, "/structr/html/test3/value1/two"));

		// /{key1}/test4/{key2}/{key3} with no parameters defined
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/one/test4/two/three"));
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/one/test4/two/three/four/five"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test4"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test4"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test4//"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test4///"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test4////"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test4/////"));
		assertEquals("Invalid path resolution result", "value1,,",      getContent(200, "/structr/html/value1/test4"));
		assertEquals("Invalid path resolution result", "value1,1234,",  getContent(200, "/structr/html/value1/test4/1234"));
		assertEquals("Invalid path resolution result", "value1,two,",   getContent(200, "/structr/html/value1/test4/two"));

		// status code check only!
		getContent(404, "/structr/html/test4");
		getContent(404, "/structr/html/test4/");
	}

	@Test
	public void test002DynamicPathPerformance() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String notFoundPageContent = "404 NOT FOUND";

		try (final Tx tx = app.tx()) {

			// create 404 page
			{
				final Page errorPage = Page.createNewPage(securityContext, "404-page");
				errorPage.setProperty(errorPage.getTraits().key(PageTraitDefinition.SHOW_ON_ERROR_CODES_PROPERTY), "404");
				final Template errorTemplate = app.create(StructrTraits.TEMPLATE).as(Template.class);

				errorPage.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
				errorPage.appendChild(errorTemplate);

				errorTemplate.setContent(notFoundPageContent);
				errorTemplate.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			}

			// create 100 pages with 4 paths each!
			for (int i=0; i<100; i++) {

				final String pageNumber = StringUtils.leftPad(Integer.toString(i), 3, "0");
				final String pageName   = "test" + pageNumber;
				final Page page         = Page.createNewPage(securityContext, pageName);
				final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

				page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
				page.appendChild(template);

				template.setContent("${key1},${key2},${key3}");
				template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

				{
					final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test" + pageNumber + "_1/prefix_{key1}/{key2}")
					);

					app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "defaultValue1")
					);

					app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key2"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Integer"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
					);
				}

				{
					final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test" + pageNumber + "_2/{key1}_{key2}_{key3}")
					);

					app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "defaultValue2")
					);
				}

				{
					app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test" + pageNumber + "_3/{key1}/{key2}/{key3}")
					);
				}

				{
					app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/{key1}/test" + pageNumber + "_4/{key2}/{key3}")
					);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		final int randomPageNumber = new Random().nextInt(100);
		final String pageNumber    = StringUtils.leftPad(Integer.toString(randomPageNumber), 3, "0");

		// /test1/prefix_{key1}/{key2} with both parameters defined, default values "defaultValue1" and 1
		assertEquals("Invalid path resolution result", "one,5,",    getContent(200, "/structr/html/test" + pageNumber + "_1/prefix_one/5/three"));
		assertEquals("Invalid path resolution result", "one,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1/prefix_one/two/three/four/five"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1/"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1//"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1///"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1////"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1/////"));
		assertEquals("Invalid path resolution result", "value1,1,", getContent(200, "/structr/html/test" + pageNumber + "_1/prefix_value1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1/value1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1/value1/1234"));
		assertEquals("Invalid path resolution result", notFoundPageContent,  getContent(404, "/structr/html/test" + pageNumber + "_1/value1/two"));

		// /test2/{key1}_{key2}_{key3} with only one parameter defined, default value "defaultValue2"
		assertEquals("Invalid path resolution result", "one,two,three",           getContent(200, "/structr/html/test" + pageNumber + "_2/one_two_three"));
		assertEquals("Invalid path resolution result", "one_two_three,four,five", getContent(200, "/structr/html/test" + pageNumber + "_2/one_two_three_four_five"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2/"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2//"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2///"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2////"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2/////"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2/"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2/_"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",         getContent(200, "/structr/html/test" + pageNumber + "_2/__"));
		assertEquals("Invalid path resolution result", "_,,",                     getContent(200, "/structr/html/test" + pageNumber + "_2/___"));
		assertEquals("Invalid path resolution result", "__,,",                    getContent(200, "/structr/html/test" + pageNumber + "_2/____"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2/value1"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2/value1/1234"));
		assertEquals("Invalid path resolution result", notFoundPageContent,                getContent(404, "/structr/html/test" + pageNumber + "_2/value1/two"));

		// /test3/{key1}/{key2}/{key3} with no parameters defined
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/test" + pageNumber + "_3/one/two/three"));
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/test" + pageNumber + "_3/one/two/three/four/five"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test" + pageNumber + "_3"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test" + pageNumber + "_3/"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test" + pageNumber + "_3//"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test" + pageNumber + "_3///"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test" + pageNumber + "_3////"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html/test" + pageNumber + "_3/////"));
		assertEquals("Invalid path resolution result", "value1,,",      getContent(200, "/structr/html/test" + pageNumber + "_3/value1"));
		assertEquals("Invalid path resolution result", "value1,1234,",  getContent(200, "/structr/html/test" + pageNumber + "_3/value1/1234"));
		assertEquals("Invalid path resolution result", "value1,two,",   getContent(200, "/structr/html/test" + pageNumber + "_3/value1/two"));

		// /{key1}/test4/{key2}/{key3} with no parameters defined
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/one/test" + pageNumber + "_4/two/three"));
		assertEquals("Invalid path resolution result", "one,two,three", getContent(200, "/structr/html/one/test" + pageNumber + "_4/two/three/four/five"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test" + pageNumber + "_4"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test" + pageNumber + "_4"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test" + pageNumber + "_4//"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test" + pageNumber + "_4///"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test" + pageNumber + "_4////"));
		assertEquals("Invalid path resolution result", ",,",            getContent(200, "/structr/html//test" + pageNumber + "_4/////"));
		assertEquals("Invalid path resolution result", "value1,,",      getContent(200, "/structr/html/value1/test" + pageNumber + "_4"));
		assertEquals("Invalid path resolution result", "value1,1234,",  getContent(200, "/structr/html/value1/test" + pageNumber + "_4/1234"));
		assertEquals("Invalid path resolution result", "value1,two,",   getContent(200, "/structr/html/value1/test" + pageNumber + "_4/two"));

		// status code check only!
		getContent(404, "/structr/html/test4");
		getContent(404, "/structr/html/test4/");

		// check some nonexisting pages
		getContent(404, "/structr/html/nonexisting");
		getContent(404, "/structr/html/error");
	}

	@Test
	public void testPagePathParameterDefaultValue() {

		final String userUUID = createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "pagePathDefaultValueTest");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${key1},${key2},${key3},${key4Node}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/defaultValueTest_1/prefix_{key1}/prefix_{key2}/prefix_{key3}/prefix_{key4Node}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT1")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key2"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT2")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key3"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      2),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT3")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key4Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      3),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), userUUID)
				);
			}

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/defaultValueTest_2/{key1}/{key2}/{key3}/{key4Node}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT1")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key2"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT2")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key3"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      2),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT3")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key4Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      3),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), userUUID)
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		assertEquals("Invalid path resolution result: no value should return the default value", "value1,DEFAULT2,value3," + userUUID, getContent(200, "/structr/html/defaultValueTest_1/prefix_value1/prefix_/prefix_value3/prefix_"));
		assertEquals("Invalid path resolution result: a space character (%20) should be treated as a value", "value1, ,value3,", getContent(200, "/structr/html/defaultValueTest_1/prefix_value1/prefix_%20/prefix_value3/prefix_ANYTHING"));

		// this requires AMBIGUOUS_EMPTY_SEGMENT violation to be allowed
		assertEquals("Invalid path resolution result: no value should return the default value", "value1,DEFAULT2,value3," + userUUID, getContent(200, "/structr/html/defaultValueTest_2/value1//value3/"));
		assertEquals("Invalid path resolution result: a space character (%20) should be treated as a value", "value1, ,value3,", getContent(200, "/structr/html/defaultValueTest_2/value1/%20/value3/ANYTHING"));
	}

	@Test
	public void testPagePathParameterDefaultValuesWhenValueParsingFails() {

		final String userUUID = createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "pagePathParseFailDefaultValueFallbackTest");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${kInt},${kFloat},${kDouble},${kLong}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/defaultValueTest_1/int_{kInt}/float_{kFloat}/double_{kDouble}/long_{kLong}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kInt"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Integer"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1234"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kFloat"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Float"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "123.45"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kDouble"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      2),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Double"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "234.56"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kLong"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      3),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Long"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1234567890123456789"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);
			}

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/defaultValueTest_2/{kInt}/{kFloat}/{kDouble}/{kLong}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kInt"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Integer"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1234"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kFloat"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Float"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "123.45"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kDouble"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      2),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Double"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "234.56"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kLong"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      3),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Long"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1234567890123456789"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY), true)
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		assertEquals("Invalid path resolution result: no value should return the default value", "1234,123.45,234.56,1234567890123456789", getContent(200, "/structr/html/defaultValueTest_1/int_/float_/double_/long_"));
		assertEquals("Invalid path resolution result: valid values should be parsed and used", "12,23.45,45.67,24682468", getContent(200, "/structr/html/defaultValueTest_1/int_12/float_23.45/double_45.67/long_24682468"));
		assertEquals("Invalid path resolution result: parse failures should return the defaults because we have allowed this explicitly", "1234,123.45,234.56,1234567890123456789", getContent(200, "/structr/html/defaultValueTest_1/int_ONE/float_23,45/double_45,67/long_TWO_MILLION"));

		// this requires AMBIGUOUS_EMPTY_SEGMENT violation to be allowed
		assertEquals("Invalid path resolution result: no value should return the default value", "1234,123.45,234.56,1234567890123456789", getContent(200, "/structr/html/defaultValueTest_2////"));
		assertEquals("Invalid path resolution result: valid values should be parsed and used", "12,23.45,45.67,24682468", getContent(200, "/structr/html/defaultValueTest_2/12/23.45/45.67/24682468"));
		assertEquals("Invalid path resolution result: parse failures should return the defaults because we have allowed this explicitly", "1234,123.45,234.56,1234567890123456789", getContent(200, "/structr/html/defaultValueTest_2/ONE/23,45/45,67/TWO_MILLION"));
	}

	@Test
	public void testPagePathWithURLEncodedSpecialCharacters() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "urlencoded_page_parameters_test_page");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${key1},${key2},${key3}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/urlencodedTest_1/prefix_{key1}/prefix_{key2}/prefix_{key3}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT1")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key2"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT2")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key3"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      2),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT3")
				);
			}

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/urlencodedTest_2/{key1}/{key2}/{key3}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT1")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key2"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT2")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "key3"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      2),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT3")
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// for this to work, the following violations have to be allowed:
		// AMBIGUOUS_PATH_SEPARATOR to allow %2f (which translates to "/") in the URL.
		// AMBIGUOUS_PATH_ENCODING to allow %25 (which translates to "%") in the URL.

		assertEquals("Invalid path resolution result: urlencoded characters should be usable (requires AMBIGUOUS_PATH_SEPARATOR and AMBIGUOUS_PATH_ENCODING)", "1a/1b/1c,10% of 100 = 10,2 + 2 = 4", getContent(200, "/structr/html/urlencodedTest_1/prefix_1a%2f1b%2f1c/prefix_10%25%20of%20100%20=%2010/prefix_2%20+%202%20=%204"));
		assertEquals("Invalid path resolution result: urlencoded characters should be usable (requires AMBIGUOUS_PATH_SEPARATOR and AMBIGUOUS_PATH_ENCODING)", "1a/1b/1c,10% of 100 = 10,2 + 2 = 4", getContent(200, "/structr/html/urlencodedTest_2/1a%2f1b%2f1c/10%25%20of%20100%20=%2010/2%20+%202%20=%204"));
	}

	// ----- private methods -----
	private String getBody(final int statusCode, final String url) {

		// do not use RestAssured here, because it automatically encodes parts of the URL (like "///")
		// and when encoding is disabled, it still normalizes paths so that the results are skewed

		try (final HttpClient client = HttpClient.newHttpClient()) {

			final HttpRequest request = HttpRequest.newBuilder()
												.uri(URI.create(RestAssured.baseURI + url))
												.header("Content-Type", "application/json; charset=UTF-8")
												.header(X_USER_HEADER, "admin")
												.header(X_PASSWORD_HEADER, "admin")
												.GET()
												.build();

			final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			assertEquals(statusCode, response.statusCode());

			return response.body();

		} catch (Throwable t) {

			final String msg = "Exception occurred during request: " + t.getMessage();

			fail(msg);

			return msg;
		}
	}

	private String getContent(final int statusCode, final String url) {
		return getBody(statusCode, url);
	}

	private String getPublicContent(final int statusCode, final String url) {

		final ResponseBody body = RestAssured
			.given()
			.expect()
			.statusCode(statusCode)
			.when()
			.get(URI.create(RestAssured.baseURI + url))

			.andReturn();

		return body.asString();
	}
}
