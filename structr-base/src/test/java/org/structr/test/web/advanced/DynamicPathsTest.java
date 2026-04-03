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
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.basic.FrontendTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.PagePathTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 */
public class DynamicPathsTest extends DeploymentTestBase {

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
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1")
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
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1")
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
	public void test003NestedTemplates() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext, "test001");

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			final Template template1 = app.create(StructrTraits.TEMPLATE).as(Template.class);
			template1.setContent("${render(children)}");
			template1.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template1);

			final Template template2 = app.create(StructrTraits.TEMPLATE).as(Template.class);
			template2.setContent("${render(children)}");
			template2.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			template1.appendChild(template2);

			final Template template3 = app.create(StructrTraits.TEMPLATE).as(Template.class);
			template3.setContent("${render(children)}");
			template3.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			template2.appendChild(template3);

			template3.setContent("${key1},${key2}");

			final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test1/{key1}/{key2}")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// /test1/prefix_{key1}/{key2} with both parameters defined, default values "defaultValue1" and 1
		assertEquals("Path parameters are not available through nested templates!", "one,5", getContent(200, "/structr/html/test1/one/5"));
	}

	@Test
	public void test004ShowOnErrorCodeWithPaths() {

		// In this test we create a non-public page with a page path and a login page
		// with showOnErrorCode 404 so that we can expect to be redirected to the login page.

		// create public page
		try (final Tx tx = app.tx()) {

			final Page page = Page.createSimplePage(securityContext, "test004");
			page.setVisibilityRecursively(false, true);

			final Page loginPage = Page.createSimplePage(securityContext, "login");
			loginPage.setVisibilityRecursively(true, false);
			loginPage.setProperty(loginPage.getTraits().key(PageTraitDefinition.SHOW_ON_ERROR_CODES_PROPERTY), "401, 404");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		final String expected = """
				<!DOCTYPE html>
				<html>
					<head>
						<title>Login</title>
					</head>
					<body>
						<h1>Login</h1>
						<div>Initial body text</div>
					</body>
				</html>""";


		// verify that the page is visible
		assertEquals("Invalid precondition", expected, getPublicContent(404, "/test004/"));

		// now we create a page path and expect the same result
		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.nodeQuery(StructrTraits.PAGE).name("test004").getFirst();

			app.create(StructrTraits.PAGE_PATH,
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test004/")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		assertEquals("Existing page path prevents showOnErrorCode redirect!", expected, getPublicContent(404, "/test004/"));
	}

	@Test
	public void testPageAndFileWithSameNameAsAdmin() {

		// create page and file in nested folder structure
		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page = Page.createSimplePage(securityContext, "file");

			final NodeInterface folder = FileHelper.createFolderPath(securityContext, "/level_one/level_two/level_three");
			final NodeInterface file   = FileHelper.createFile(securityContext, "testContent".getBytes(StandardCharsets.UTF_8), "text/plain", StructrTraits.FILE);

			file.setName("file");
			file.setProperty(file.getTraits().key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder);

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		RestAssured
			.given().header("x-user", "admin").header("x-password", "admin")
			.expect().statusCode(200)
			.body(equalTo("testContent"))
			.when().get("/level_one/level_two/level_three/file");
	}

	@Test
	public void testPagePathStrictness() {

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

			final Page page         = Page.createNewPage(securityContext, "pagePathParameterTest");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${key1},${key2}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/prefixed/prefix_{key1}")
			);

			app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/suffixed/{key1}_suffix")
			);

			app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/both/prefix_{key1}_suffix")
			);

			app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/both_two_keys/prefix_{key1}_{key2}_suffix")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		assertEquals("Invalid path resolution result", notFoundPageContent, getContent(404, "/structr/html/prefixed/USED_TO_WORK_prefix_SHOULD_NOT_WORK"));
		assertEquals("Invalid path resolution result", notFoundPageContent, getContent(404, "/structr/html/prefixed/USED_TO_WORK_SHOULD_NOT_WORK"));
		assertEquals("Invalid path resolution result", "value1,",  getContent(200, "/structr/html/prefixed/prefix_value1"));

		assertEquals("Invalid path resolution result", notFoundPageContent, getContent(404, "/structr/html/suffixed/USED_TO_WORK_value1_SHOULD_NOT_WORK"));
		assertEquals("Invalid path resolution result", notFoundPageContent, getContent(404, "/structr/html/suffixed/USED_TO_WORK_SHOULD_NOT_WORK"));
		assertEquals("Invalid path resolution result", "value2,",  getContent(200, "/structr/html/suffixed/value2_suffix"));

		assertEquals("Invalid path resolution result", notFoundPageContent, getContent(404, "/structr/html/both/USED_TO_WORK_prefix_value1_suffix_SHOULD_NOT_WORK"));
		assertEquals("Invalid path resolution result", notFoundPageContent, getContent(404, "/structr/html/both/USED_TO_WORK_SHOULD_NOT_WORK"));
		assertEquals("Invalid path resolution result", "value3,",  getContent(200, "/structr/html/both/prefix_value3_suffix"));
	}

	@Test
	public void testPagePathPriority() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		NodeInterface pagePath1 = null;
		NodeInterface pagePath2 = null;
		NodeInterface pagePath3 = null;

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "pagePathPriorityTest");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${key1}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			pagePath1 = app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/priorityTest/prefix_{key1}")
			);

			pagePath2 = app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/priorityTest/{key1}_suffix")
			);

			pagePath3 = app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/priorityTest/prefix_{key1}_suffix")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// set order to prefix, suffix, both
		{
			try (final Tx tx = app.tx()) {

				pagePath1.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 1);
				pagePath2.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 2);
				pagePath3.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 3);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}
			assertEquals("Invalid path resolution result", "value_suffix", getContent(200, "/structr/html/priorityTest/prefix_value_suffix"));
		}

		// set order to suffix, both, prefix
		{
			try (final Tx tx = app.tx()) {

				pagePath1.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 3);
				pagePath2.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 1);
				pagePath3.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 2);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}
			assertEquals("Invalid path resolution result", "prefix_value", getContent(200, "/structr/html/priorityTest/prefix_value_suffix"));
		}

		{
			// set order to suffix, both, prefix
			try (final Tx tx = app.tx()) {

				pagePath1.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 2);
				pagePath2.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 3);
				pagePath3.setProperty(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PRIORITY_PROPERTY), 1);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}
			assertEquals("Invalid path resolution result", "value", getContent(200, "/structr/html/priorityTest/prefix_value_suffix"));
		}
	}

	@Test
	public void testPagePathParameterDefaultValue() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "pagePathDefaultValueTest");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${key1},${key2},${key3}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/defaultValueTest_1/prefix_{key1}/prefix_{key2}/prefix_{key3}")
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
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/defaultValueTest_2/{key1}/{key2}/{key3}")
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

		// no value should return the default value
		assertEquals("Invalid path resolution result", "value1,DEFAULT2,value3", getContent(200, "/structr/html/defaultValueTest_1/prefix_value1/prefix_/prefix_value3"));

		// a space character (%20) should be treated as a value
		assertEquals("Invalid path resolution result", "value1, ,value3", getContent(200, "/structr/html/defaultValueTest_1/prefix_value1/prefix_%20/prefix_value3"));

		// this requires AMBIGUOUS_EMPTY_SEGMENT violation to be allowed
		// no value should return the default value
		assertEquals("Invalid path resolution result", "value1,DEFAULT2,value3", getContent(200, "/structr/html/defaultValueTest_2/value1//value3"));

		// a space character (%20) should be treated as a value
		assertEquals("Invalid path resolution result", "value1, ,value3", getContent(200, "/structr/html/defaultValueTest_2/value1/%20/value3"));
	}

	@Test
	public void testPagePathParameterDefaultValueConversionAndDeployment() {

		final String userUUID = createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "pagePathDefaultValueConversionTest");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("""
					${{ $.print($.kString + ' - ' + typeof $.kString); }}
					${{ $.print($.kInteger + ' - ' + typeof $.kInteger); }}
					${{ $.print($.kLong + ' - ' + typeof $.kLong); }}
					${{ $.print($.kDouble + ' - ' + typeof $.kDouble); }}
					${{ $.print($.kFloat + ' - ' + typeof $.kFloat); }}
					${{ $.print($.kDate + ' - ' + typeof $.kDate); }}
					${{ $.print($.kBool + ' - ' + typeof $.kBool); }}
					${{ $.print($.kNodeByUUID.name + ' - ' + typeof $.kNodeByUUID); }}
					${{ $.print($.kNodeByName.name + ' - ' + typeof $.kNodeByName); }}
					""");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/html");

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/defaultValueConversionTest_1/{kString}/{kInteger}/{kLong}/{kDouble}/{kFloat}/{kDate}/{kBool}/{kNodeByUUID}/{kNodeByName}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kString"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "DEFAULT_string")
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kInteger"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Integer"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "1337")
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kLong"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      2),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Long"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "42")
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kDouble"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      3),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Double"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "42.1337")
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kFloat"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      4),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Float"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "42.5")
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kDate"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      5),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Date"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.FORMAT_PROPERTY),        "dd.MM.yyyy HH:mm:ss"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "13.04.2026 12:34:56")
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kBool"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      6),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Boolean"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "true")
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kNodeByUUID"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      7),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), userUUID)
				);
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kNodeByName"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      8),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "admin")
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// omit all variables and expect the default value strings to be converted correctly
		assertEquals("Invalid path resolution result", """
				DEFAULT_string - string
				1337 - number
				42 - number
				42.1337 - number
				42.5 - number
				Mon Apr 13 12:34:56 UTC 2026 - object
				true - boolean
				admin - object
				admin - object
				""", getContent(200, "/structr/html/defaultValueConversionTest_1/"));

		doImportExportRoundtrip(true, null, false);

		// check that everything still works after deployment
		assertEquals("Invalid path resolution result", """
				DEFAULT_string - string
				1337 - number
				42 - number
				42.1337 - number
				42.5 - number
				Mon Apr 13 12:34:56 UTC 2026 - object
				true - boolean
				admin - object
				admin - object
				""", getContent(200, "/structr/html/defaultValueConversionTest_1/"));
	}

	@Test
	public void testPagePathWithURLEncodedCharacters() {

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
		// AMBIGUOUS_PATH_ENCODING to allow %25 (which translates to "%") in the URL. This could possibly be used in double-encoding attacks (if the target does not ensure this does not happen)

		// urlencoded characters with prefixed path parts
		assertEquals("Invalid path resolution result", "1a/1b/1c,10% of 100 = 10,2 + 2 = 4", getContent(200, "/structr/html/urlencodedTest_1/prefix_1a%2f1b%2f1c/prefix_10%25%20of%20100%20=%2010/prefix_2%20+%202%20=%204"));

		// urlencoded characters with unprefixed path parts
		assertEquals("Invalid path resolution result", "1a/1b/1c,10% of 100 = 10,2 + 2 = 4", getContent(200, "/structr/html/urlencodedTest_2/1a%2f1b%2f1c/10%25%20of%20100%20=%2010/2%20+%202%20=%204"));
	}

	@Test
	public void testPagePathParameterTypeNode() {

		final String userUUID = createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		String pageUUID = null;

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "pagePathNodeTypeTest");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			pageUUID = page.getUuid();

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${{ $.print(($.kNode?.name ?? 'NO NODE!') + ' - ' + typeof $.kNode); }}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/html");

			page.setVisibilityRecursively(true, true);

			// path 1: any node (the user can see) can be used
			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/nodeParameterTest_1/{kNode}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kNode"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Node")
				);
			}

			// path 2: any node (with default being the admin user)
			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/nodeParameterTest_2/{kNode}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kNode"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), userUUID)
				);
			}

			// path 3: only nodes of type "Principal" (and inheriting types) can be used
			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/nodeParameterTest_3/{kNode}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kNode"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Node"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.FORMAT_PROPERTY),        "Principal")
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// check with admin user
		assertEquals("Invalid path resolution result", "admin - object",                getContent(200, "/structr/html/nodeParameterTest_1/" + userUUID));
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getContent(200, "/structr/html/nodeParameterTest_1/"));
		assertEquals("Invalid path resolution result", "pagePathNodeTypeTest - object", getContent(200, "/structr/html/nodeParameterTest_1/" + pageUUID));

		assertEquals("Invalid path resolution result", "admin - object",                getContent(200, "/structr/html/nodeParameterTest_2/" + userUUID));
		assertEquals("Invalid path resolution result", "admin - object",                getContent(200, "/structr/html/nodeParameterTest_2/"));
		assertEquals("Invalid path resolution result", "pagePathNodeTypeTest - object", getContent(200, "/structr/html/nodeParameterTest_2/" + pageUUID));

		assertEquals("Invalid path resolution result", "admin - object",                getContent(200, "/structr/html/nodeParameterTest_3/" + userUUID));
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getContent(200, "/structr/html/nodeParameterTest_3/"));
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getContent(200, "/structr/html/nodeParameterTest_3/" + pageUUID));


		// check without user
		// - admin can NOT be seen by public users. Not by ID and also not as the default value
		// - the page can be seen
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getPublicContent(200, "/structr/html/nodeParameterTest_1/" + userUUID));
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getPublicContent(200, "/structr/html/nodeParameterTest_1/"));
		assertEquals("Invalid path resolution result", "pagePathNodeTypeTest - object", getPublicContent(200, "/structr/html/nodeParameterTest_1/" + pageUUID));

		assertEquals("Invalid path resolution result", "NO NODE! - object",             getPublicContent(200, "/structr/html/nodeParameterTest_2/" + userUUID));
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getPublicContent(200, "/structr/html/nodeParameterTest_2/"));
		assertEquals("Invalid path resolution result", "pagePathNodeTypeTest - object", getPublicContent(200, "/structr/html/nodeParameterTest_2/" + pageUUID));

		assertEquals("Invalid path resolution result", "NO NODE! - object",             getPublicContent(200, "/structr/html/nodeParameterTest_3/" + userUUID));
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getPublicContent(200, "/structr/html/nodeParameterTest_3/"));
		assertEquals("Invalid path resolution result", "NO NODE! - object",             getPublicContent(200, "/structr/html/nodeParameterTest_3/" + pageUUID));
	}

	@Test
	public void testPagePathParameterTypeDateWithFormat() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "pagePathParameterTypeDateWithFormat");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");
			page.appendChild(template);

			template.setContent("${{ $.print($.kDate + ' - ' + typeof $.kDate); }}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/html");

			// 1. Basic date without format (requires on of multiple ISO date formats)
			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/dateParameter_1/{kDate}")
				);

				// {kDate}
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kDate"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Date")
				);
			}

			// 2. Date with format and default
			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/dateParameter_2/{kDate}")
				);

				// {kDate}
				app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "kDate"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.POSITION_PROPERTY),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "Date"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.FORMAT_PROPERTY),        "dd.MM.yyyy HH:mm:ss"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "13.04.2026 12:34:56")
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		assertEquals("Invalid path resolution result", "null - object",                         getContent(200, "/structr/html/dateParameter_1/"));
		assertEquals("Invalid path resolution result", "Mon Dec 15 12:34:56 UTC 2025 - object", getContent(200, "/structr/html/dateParameter_1/2025-12-15T12:34:56.000Z"));

		assertEquals("Invalid path resolution result", "Mon Dec 15 09:12:34 UTC 2025 - object", getContent(200, "/structr/html/dateParameter_2/15.12.2025%2009:12:34"));
		assertEquals("Invalid path resolution result", "Mon Apr 13 12:34:56 UTC 2026 - object", getContent(200, "/structr/html/dateParameter_2/"));
	}

	@Test
	public void testPageAndFileWithSameName() {

		// create page and file in nested folder structure
		try (final Tx tx = app.tx()) {

			final Page page = Page.createSimplePage(securityContext, "file");
			page.setVisibilityRecursively(true, true);

			final NodeInterface folder = FileHelper.createFolderPath(securityContext, "/level_one/level_two/level_three");
			final NodeInterface file   = FileHelper.createFile(securityContext, "testContent".getBytes(StandardCharsets.UTF_8), "text/plain", StructrTraits.FILE);

			file.setName("file");
			file.setVisibility(true, true);
			file.setProperty(file.getTraits().key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder);

			for (final NodeInterface f : app.nodeQuery(StructrTraits.FOLDER).getResultStream()) {

				f.as(Folder.class).setVisibility(true, true);
			}

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		RestAssured
			.expect().statusCode(200)
			.body(equalTo("testContent"))
			.when().get("/level_one/level_two/level_three/file");
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
