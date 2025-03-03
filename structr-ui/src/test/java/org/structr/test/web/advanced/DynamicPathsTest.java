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

import io.restassured.RestAssured;
import io.restassured.response.ResponseBody;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.basic.FrontendTest;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 */
public class DynamicPathsTest extends FrontendTest {

	@Test
	public void test001DynamicPathResolution() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "test001");
			final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

			page.setProperty(Traits.of(StructrTraits.PAGE).key("contentType"), "text/plain");
			page.appendChild(template);

			template.setContent("${key1},${key2},${key3}");
			template.setProperty(Traits.of(StructrTraits.TEMPLATE).key("contentType"), "text/plain");

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test1/prefix_{key1}/{key2}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("path"),          path),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "key1"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("position"),      0),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("valueType"),     "String"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("defaultValue"),  "defaultValue1")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("path"),          path),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "key2"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("position"),      1),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("valueType"),     "Integer"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("defaultValue"),  "1")
				);
			}

			{
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test2/{key1}_{key2}_{key3}")
				);

				app.create(StructrTraits.PAGE_PATH_PARAMETER,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("path"),          path),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "key1"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("position"),      0),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("valueType"),     "String"),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("defaultValue"),  "defaultValue2")
				);
			}

			{
				app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test3/{key1}/{key2}/{key3}")
				);
			}

			{
				app.create(StructrTraits.PAGE_PATH,
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
					new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/{key1}/test4/{key2}/{key3}")
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// /test1/prefix_{key1}/{key2} with both parameters defined, default values "defaultValue1" and 1
		assertEquals("Invalid path resolution result", "one,5,",              getContent(200, "/structr/html/test1/prefix_one/5/three"));
		assertEquals("Invalid path resolution result", "one,1,",              getContent(200, "/structr/html/test1/prefix_one/two/three/four/five"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1/"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1//"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1///"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1////"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1/////"));
		assertEquals("Invalid path resolution result", "value1,1,",           getContent(200, "/structr/html/test1/prefix_value1"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1/value1"));
		assertEquals("Invalid path resolution result", "defaultValue1,1234,", getContent(200, "/structr/html/test1/value1/1234"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test1/value1/two"));

		// /test2/{key1}_{key2}_{key3} with only one parameter defined, default value "defaultValue2"
		assertEquals("Invalid path resolution result", "one,two,three",           getContent(200, "/structr/html/test2/one_two_three"));
		assertEquals("Invalid path resolution result", "one_two_three,four,five", getContent(200, "/structr/html/test2/one_two_three_four_five"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2//"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2///"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2////"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/////"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/_"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/__"));
		assertEquals("Invalid path resolution result", "_,,",                      getContent(200, "/structr/html/test2/___"));
		assertEquals("Invalid path resolution result", "__,,",                     getContent(200, "/structr/html/test2/____"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/value1"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/value1/1234"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test2/value1/two"));

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

		try (final Tx tx = app.tx()) {

			// create 100 pages with 4 paths each!
			for (int i=0; i<100; i++) {

				final String pageNumber = StringUtils.leftPad(Integer.toString(i), 3, "0");
				final String pageName   = "test" + pageNumber;
				final Page page         = Page.createNewPage(securityContext, pageName);
				final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

				page.setProperty(Traits.of(StructrTraits.PAGE).key("contentType"), "text/plain");
				page.appendChild(template);

				template.setContent("${key1},${key2},${key3}");
				template.setProperty(Traits.of(StructrTraits.TEMPLATE).key("contentType"), "text/plain");

				{
					final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test" + pageNumber + "_1/prefix_{key1}/{key2}")
					);

					app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("path"),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("position"),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("valueType"),     "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("defaultValue"),  "defaultValue1")
					);

					app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("path"),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "key2"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("position"),      1),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("valueType"),     "Integer"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("defaultValue"),  "1")
					);
				}

				{
					final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test" + pageNumber + "_2/{key1}_{key2}_{key3}")
					);

					app.create(StructrTraits.PAGE_PATH_PARAMETER,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("path"),          path),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "key1"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("position"),      0),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("valueType"),     "String"),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key("defaultValue"),  "defaultValue2")
					);
				}

				{
					app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test" + pageNumber + "_3/{key1}/{key2}/{key3}")
					);
				}

				{
					app.create(StructrTraits.PAGE_PATH,
						new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
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
		assertEquals("Invalid path resolution result", "one,5,",              getContent(200, "/structr/html/test" + pageNumber + "_1/prefix_one/5/three"));
		assertEquals("Invalid path resolution result", "one,1,",              getContent(200, "/structr/html/test" + pageNumber + "_1/prefix_one/two/three/four/five"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1/"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1//"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1///"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1////"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1/////"));
		assertEquals("Invalid path resolution result", "value1,1,",           getContent(200, "/structr/html/test" + pageNumber + "_1/prefix_value1"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1/value1"));
		assertEquals("Invalid path resolution result", "defaultValue1,1234,", getContent(200, "/structr/html/test" + pageNumber + "_1/value1/1234"));
		assertEquals("Invalid path resolution result", "defaultValue1,1,",    getContent(200, "/structr/html/test" + pageNumber + "_1/value1/two"));

		// /test2/{key1}_{key2}_{key3} with only one parameter defined, default value "defaultValue2"
		assertEquals("Invalid path resolution result", "one,two,three",           getContent(200, "/structr/html/test" + pageNumber + "_2/one_two_three"));
		assertEquals("Invalid path resolution result", "one_two_three,four,five", getContent(200, "/structr/html/test" + pageNumber + "_2/one_two_three_four_five"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2//"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2///"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2////"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/////"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/_"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/__"));
		assertEquals("Invalid path resolution result", "_,,",                      getContent(200, "/structr/html/test" + pageNumber + "_2/___"));
		assertEquals("Invalid path resolution result", "__,,",                     getContent(200, "/structr/html/test" + pageNumber + "_2/____"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/value1"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/value1/1234"));
		assertEquals("Invalid path resolution result", "defaultValue2,,",          getContent(200, "/structr/html/test" + pageNumber + "_2/value1/two"));

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

			page.setProperty(Traits.of(StructrTraits.PAGE).key("contentType"), "text/plain");

			final Template template1 = app.create(StructrTraits.TEMPLATE).as(Template.class);
			template1.setContent("${render(children)}");
			template1.setProperty(Traits.of(StructrTraits.TEMPLATE).key("contentType"), "text/plain");
			page.appendChild(template1);

			final Template template2 = app.create(StructrTraits.TEMPLATE).as(Template.class);
			template2.setContent("${render(children)}");
			template2.setProperty(Traits.of(StructrTraits.TEMPLATE).key("contentType"), "text/plain");
			template1.appendChild(template2);

			final Template template3 = app.create(StructrTraits.TEMPLATE).as(Template.class);
			template3.setContent("${render(children)}");
			template3.setProperty(Traits.of(StructrTraits.TEMPLATE).key("contentType"), "text/plain");
			template2.appendChild(template3);

			template3.setContent("${key1},${key2}");

			final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key("page"), page),
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

	// ----- private methods -----
	private ResponseBody getBody(final int statusCode, final String url) {

		final ResponseBody body =  RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User",     "admin")
				.header("X-Password", "admin")
			.expect()
				.statusCode(statusCode)
			.when()
				.get(url)
			.andReturn();

		return body;
	}

	private String getContent(final int statusCode, final String url) {
		return getBody(statusCode, url).asString();
	}
}
