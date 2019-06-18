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
package org.structr.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.util.Arrays;
import org.testng.annotations.Test;
import org.structr.core.graph.Tx;
import org.structr.flow.impl.FlowAction;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowReturn;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.equalTo;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.graph.NodeAttribute;
import org.structr.flow.impl.FlowTypeQuery;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.Title;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;
import org.w3c.dom.Node;

public class FlowTest extends StructrUiTest {

	@Test
	public void testFlowWithIterablesAndScripting() {

		final Map<String, Object> flowParameters         = new HashMap<>();
		Map<String,Object> resultMap                     = null;

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(FlowContainer.class, "testFlow");

			resultMap = container.evaluate(flowParameters);

			assertNotNull(resultMap);
			assertNull(resultMap.get("result"));

			FlowAction action = app.create(FlowAction.class, "createAction");
			action.setProperty(FlowAction.script, "{ ['a','b','c'].forEach( data => Structr.create('User','name',data)) }");
			action.setProperty(FlowAction.flowContainer, container);

			FlowDataSource ds = app.create(FlowDataSource.class, "ds");
			ds.setProperty(FlowDataSource.query, "find('User')");
			ds.setProperty(FlowAction.flowContainer, container);

			FlowReturn ret = app.create(FlowReturn.class, "ds");
			ret.setProperty(FlowReturn.dataSource, ds);
			ret.setProperty(FlowAction.flowContainer, container);

			action.setProperty(FlowAction.next, ret);

			container.setProperty(FlowContainer.startNode, action);

			resultMap = container.evaluate(flowParameters);
			assertNotNull(resultMap);
			assertNotNull(resultMap.get("result"));

			ds.setProperty(FlowDataSource.query, "size(find('User'))");

			resultMap = container.evaluate(flowParameters);
			assertNotNull(resultMap);
			assertNotNull(resultMap.get("result"));

		} catch (Throwable ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}


	}

	@Test
	public void testFlowRepeater() {

		try (final Tx tx = app.tx()) {

			// create admin user
			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			// create some test data
			createTestNode(Group.class, new NodeAttribute<>(Group.name, "group1"));
			createTestNode(Group.class, new NodeAttribute<>(Group.name, "group2"));
			createTestNode(Group.class, new NodeAttribute<>(Group.name, "group3"));
			createTestNode(Group.class, new NodeAttribute<>(Group.name, "group4"));

			// create flow
			final FlowContainer flowContainer = app.create(FlowContainer.class, "test");
			final FlowTypeQuery query         = app.create(FlowTypeQuery.class, "query");
			final FlowReturn    ret           = app.create(FlowReturn.class, "return");

			query.setProperty(StructrApp.key(FlowTypeQuery.class, "dataType"), "Group");
			query.setProperty(StructrApp.key(FlowTypeQuery.class, "query"),    "{\"type\":\"group\",\"op\":\"and\",\"operations\":[],\"queryType\":\"Group\"}");
			query.setProperty(FlowAction.flowContainer, flowContainer);

			ret.setProperty(FlowReturn.dataSource, query);
			ret.setProperty(FlowAction.flowContainer, flowContainer);

			flowContainer.setProperty(FlowContainer.startNode, ret);

			// evaluate flow
			final Map<String, Object> map = flowContainer.evaluate(new LinkedHashMap<>());
			final Iterable result         = (Iterable)map.get("result");
			final List<Group> groups      = Iterables.toList(result);

			assertEquals("Invalid number of groups in flow result", 4, groups.size());

			// create a page
			final Page newPage = Page.createNewPage(securityContext, "flowRepeaterTestPage");

			newPage.setVisibility(true, true);

			final Html html    = createElement(newPage, newPage, "html");
			final Head head    = createElement(newPage, html, "head");
			final Title title  = createElement(newPage, head, "title", "Test Page for flow-based repeater");
			final Body body    = createElement(newPage, html, "body");
			final Div div1     = createElement(newPage, body, "div", "${group.name}");

			flowContainer.setProperty(FlowContainer.repeaterNodes, Arrays.asList(div1));
			div1.setProperty(StructrApp.key(Div.class, "dataKey"), "group");

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}


		RestAssured.basePath = htmlUrl;

		RestAssured
		.given()
			.accept("text/html")
			.headers("X-User", "admin" , "X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))

		.expect()
			.statusCode(200)
			.contentType("text/html;charset=utf-8")

			.body("html.body.div[0]", equalTo("group1"))

		.when()
			.get("/flowRepeaterTestPage");



	//"Strict-Transport-Security:max-age=60,X-Content-Type-Options:nosniff,X-Frame-Options:SAMEORIGIN,X-XSS-Protection:1;mode=block", "List of custom response headers that will be added to every HTTP response");

	}

	private <T extends Node> T createElement(final Page page, final DOMNode parent, final String tag, final String... content) {

		final T child = (T)page.createElement(tag);
		parent.appendChild((DOMNode)child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final Node node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}
}
