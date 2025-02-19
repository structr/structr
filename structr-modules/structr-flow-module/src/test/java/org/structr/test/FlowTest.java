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
package org.structr.test;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.flow.impl.*;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.*;
import org.testng.annotations.Test;
import org.w3c.dom.Node;

import java.lang.Object;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.testng.AssertJUnit.*;

public class FlowTest extends StructrUiTest {

	@Test
	public void testFlowWithIterablesAndScripting() {

		final Map<String, Object> flowParameters         = new HashMap<>();
		Iterable<Object> result                          = null;

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(FlowContainer.class, "testFlow");

			result = container.evaluate(securityContext, flowParameters);

			assertNotNull(result);

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

			result = container.evaluate(securityContext, flowParameters);
			assertNotNull(result);

			ds.setProperty(FlowDataSource.query, "size(find('User'))");

			result = container.evaluate(securityContext, flowParameters);
			assertNotNull(result);

			tx.success();

		} catch (Throwable ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}


	}

	@Test
	public void testFlowForEach() {

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(FlowContainer.class, "testFlowForEach");

			FlowForEach forEach = app.create(FlowForEach.class);
			forEach.setProperty(FlowForEach.flowContainer, container);
			container.setProperty(FlowContainer.startNode, forEach);

			FlowDataSource ds = app.create(FlowDataSource.class);
			ds.setProperty(FlowDataSource.query, "{[1,2,3,4,5];}");
			ds.setProperty(FlowDataSource.flowContainer, container);
			forEach.setProperty(FlowForEach.dataSource, ds);

			FlowDataSource ds2 = app.create(FlowDataSource.class);
			ds2.setProperty(FlowDataSource.query, "now");
			ds2.setProperty(FlowDataSource.flowContainer, container);

			FlowAggregate agg = app.create(FlowAggregate.class);
			agg.setProperty(FlowAggregate.flowContainer, container);
			agg.setProperty(FlowAggregate.dataSource, ds2);
			agg.setProperty(FlowAggregate.script, "{let data = $.get('data'); let currentData = $.get('currentData'); if (data === currentData || $.empty(currentData)) { throw 'ForEach scoping problem! Values should not be the same.' } }");
			forEach.setProperty(FlowForEach.loopBody, agg);

			container.evaluate(securityContext, new HashMap<>());

			tx.success();

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
			query.setProperty(StructrApp.key(FlowTypeQuery.class, "query"), "{\"type\":\"group\",\"op\":\"and\",\"operations\":[{\"type\":\"sort\",\"key\":\"name\",\"order\":\"desc\",\"queryType\":\"Group\"}],\"queryType\":\"Group\"}");

			query.setProperty(FlowAction.flowContainer, flowContainer);

			ret.setProperty(FlowReturn.dataSource, query);
			ret.setProperty(FlowAction.flowContainer, flowContainer);

			flowContainer.setProperty(FlowContainer.startNode, ret);

			// evaluate flow
			final Iterable<Object> result = flowContainer.evaluate(securityContext, new LinkedHashMap<>());
			final List<Group> groups      = StreamSupport.stream(result.spliterator(), false).map((o) -> (Group)o).collect(Collectors.toList());

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

			.body("html.body.div[0]", equalTo("group4"))

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
