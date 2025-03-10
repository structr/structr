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
import org.structr.core.entity.Group;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.flow.impl.*;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

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

			FlowContainer container = app.create("FlowContainer", "testFlow").as(FlowContainer.class);

			result = container.evaluate(securityContext, flowParameters);

			assertNotNull(result);

			FlowAction action = app.create("FlowAction", "createAction").as(FlowAction.class);
			action.setScript("{ ['a','b','c'].forEach( data => Structr.create('User','name',data)) }");
			action.setFlowContainer(container);

			FlowDataSource ds = app.create("FlowDataSource", "ds").as(FlowDataSource.class);
			ds.setQuery("find('User')");
			ds.setFlowContainer(container);

			FlowReturn ret = app.create("FlowReturn", "ds").as(FlowReturn.class);
			ret.setDataSource(ds);
			ret.setFlowContainer(container);

			action.setNext(ret);

			container.setStartNode(action);

			result = container.evaluate(securityContext, flowParameters);
			assertNotNull(result);

			ds.setQuery("size(find('User'))");

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

			FlowContainer container = app.create("FlowContainer", "testFlowForEach").as(FlowContainer.class);

			FlowForEach forEach = app.create("FlowForEach").as(FlowForEach.class);
			forEach.setFlowContainer(container);
			container.setStartNode(forEach);

			FlowDataSource ds = app.create("FlowDataSource").as(FlowDataSource.class);
			ds.setQuery("{return [1,2,3,4,5];}");
			ds.setFlowContainer(container);
			forEach.setDataSource(ds);

			FlowDataSource ds2 = app.create("FlowDataSource").as(FlowDataSource.class);
			ds2.setQuery("now");
			ds2.setFlowContainer(container);

			FlowAggregate agg = app.create("FlowAggregate").as(FlowAggregate.class);
			agg.setFlowContainer(container);
			agg.setDataSource(ds2);
			agg.setScript("{let data = $.get('data'); let currentData = $.get('currentData'); if (data === currentData || $.empty(currentData)) { throw 'ForEach scoping problem! Values should not be the same.' } }");
			forEach.setLoopBody(agg);

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
			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of("User").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "admin"),
				new NodeAttribute<>(Traits.of("User").key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "admin"),
				new NodeAttribute<>(Traits.of("User").key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY),  true)
			);

			// create some test data
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of("Group").key("name"), "group1"));
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of("Group").key("name"), "group2"));
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of("Group").key("name"), "group3"));
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of("Group").key("name"), "group4"));

			// create flow
			final FlowContainer flowContainer = app.create("FlowContainer", "test").as(FlowContainer.class);
			final FlowTypeQuery query         = app.create("FlowTypeQuery", "query").as(FlowTypeQuery.class);
			final FlowReturn    ret           = app.create("FlowReturn", "return").as(FlowReturn.class);

			query.setProperty(Traits.of("FlowTypeQuery").key("dataType"), StructrTraits.GROUP);
			query.setProperty(Traits.of("FlowTypeQuery").key("query"), "{\"type\":\"group\",\"op\":\"and\",\"operations\":[{\"type\":\"sort\",\"key\":\"name\",\"order\":\"desc\",\"queryType\":\"Group\"}],\"queryType\":\"Group\"}");

			query.setFlowContainer(flowContainer);

			ret.setDataSource(query);
			ret.setFlowContainer(flowContainer);

			flowContainer.setStartNode(ret);

			// evaluate flow
			final Iterable<Object> result = flowContainer.evaluate(securityContext, new LinkedHashMap<>());
			final List<Group> groups      = StreamSupport.stream(result.spliterator(), false).map((o) -> ((NodeInterface)o).as(Group.class)).collect(Collectors.toList());

			assertEquals("Invalid number of groups in flow result", 4, groups.size());

			// create a page
			final Page newPage = Page.createNewPage(securityContext, "flowRepeaterTestPage");

			newPage.setVisibility(true, true);

			final DOMNode html  = createElement(newPage, newPage, "html");
			final DOMNode head  = createElement(newPage, html, "head");
			final DOMNode title = createElement(newPage, head, "title", "Test Page for flow-based repeater");
			final DOMNode body  = createElement(newPage, html, "body");
			final DOMNode div1  = createElement(newPage, body, "div", "${group.name}");

			flowContainer.setRepeaterNodes(Arrays.asList(div1));
			div1.setProperty(Traits.of("Div").key("dataKey"), "group");

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

	private DOMNode createElement(final Page page, final DOMNode parent, final String tag, final String... content) throws FrameworkException {

		final DOMNode child = page.createElement(tag);

		parent.appendChild(child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final DOMNode node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}
}
