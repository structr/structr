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
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.web.basic.FrontendTest;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

/**
 */
public class DynamicPathsTest extends FrontendTest {

	@Test
	public void test001DynamicPaths() {

		String path1Id = null;
		String path2Id = null;
		String path3Id = null;

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "test001");
			final Template template = app.create(Template.class);

			page.setProperty(StructrApp.key(Page.class, "contentType"), "text/plain");
			page.appendChild(template);

			template.setContent("${key1},${key2},${key3}");
			template.setProperty(StructrApp.key(Template.class, "contentType"), "text/plain");

			{
				final PagePath path = app.create(PagePath.class,
					new NodeAttribute<>(StructrApp.key(PagePath.class, "page"), page),
					new NodeAttribute<>(StructrApp.key(PagePath.class, "name"), "/test1/prefix_{key1}/{key2}")
				);

				app.create(PagePathParameter.class,
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "path"),          path),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "name"),          "key1"),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "position"),      0),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "valueType"),     "String"),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "defaultValue"),  "defaultValue1")
				);

				app.create(PagePathParameter.class,
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "path"),          path),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "name"),          "key2"),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "position"),      1),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "valueType"),     "Integer"),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "defaultValue"),  "1")
				);

				path1Id = path.getUuid();
			}

			{
				final PagePath path = app.create(PagePath.class,
					new NodeAttribute<>(StructrApp.key(PagePath.class, "page"), page),
					new NodeAttribute<>(StructrApp.key(PagePath.class, "name"), "/test2/{key1}_{key2}_{key3}")
				);

				app.create(PagePathParameter.class,
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "path"),          path),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "name"),          "key1"),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "position"),      0),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "valueType"),     "String"),
					new NodeAttribute<>(StructrApp.key(PagePathParameter.class, "defaultValue"),  "defaultValue2")
				);

				path2Id = path.getUuid();
			}

			{
				final PagePath path = app.create(PagePath.class,
					new NodeAttribute<>(StructrApp.key(PagePath.class, "page"), page),
					new NodeAttribute<>(StructrApp.key(PagePath.class, "name"), "/test3/{key1}/{key2}/{key3}")
				);

				path3Id = path.getUuid();
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// verify that the path is split into parts correctly in onCreate and onSave
		//assertEquals("Path was not split correctly", "test1,prefix_(.*),(.*)", StringUtils.join(getBody(200, "/PagePath/" + path1Id).jsonPath().getList("result.parts"), ","));
		//assertEquals("Path was not split correctly", "test2,(.*)_(.*)_(.*)",   StringUtils.join(getBody(200, "/PagePath/" + path2Id).jsonPath().getList("result.parts"), ","));
		//assertEquals("Path was not split correctly", "test3,(.*),(.*),(.*)",   StringUtils.join(getBody(200, "/PagePath/" + path3Id).jsonPath().getList("result.parts"), ","));

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
		assertEquals("Invalid path resolution result", ",,",                       getContent(200, "/structr/html/test2/__"));
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
		assertEquals("Invalid path resolution result", "value1,,",      getContent(200, "/structr/html/test3/value1"));
		assertEquals("Invalid path resolution result", "value1,1234,",  getContent(200, "/structr/html/test3/value1/1234"));
		assertEquals("Invalid path resolution result", "value1,two,",   getContent(200, "/structr/html/test3/value1/two"));
	}

	// ----- private methods -----
	private ResponseBody getBody(final int statusCode, final String url) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User",     "admin")
				.header("X-Password", "admin")
			.expect()
				.statusCode(statusCode)
			.when()
				.get(url)
			.andReturn();
	}

	private String getContent(final int statusCode, final String url) {
		return getBody(statusCode, url).asString();
	}
}
