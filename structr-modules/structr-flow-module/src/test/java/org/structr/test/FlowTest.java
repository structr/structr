/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.api.util.Iterables;
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
import org.structr.flow.traits.definitions.FlowDataSourceTraitDefinition;
import org.structr.flow.traits.definitions.FlowTypeQueryTraitDefinition;
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
		Object result                                    = null;

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(StructrTraits.FLOW_CONTAINER, "testFlow").as(FlowContainer.class);

			result = container.evaluate(securityContext, flowParameters);

			assertNull(result);

			FlowAction action = app.create(StructrTraits.FLOW_ACTION, "createAction").as(FlowAction.class);
			action.setScript("{ ['a','b','c'].forEach( data => Structr.create('User','name',data)) }");
			action.setFlowContainer(container);

			FlowDataSource ds = app.create(StructrTraits.FLOW_DATA_SOURCE, "ds").as(FlowDataSource.class);
			ds.setQuery("find('User')");
			ds.setFlowContainer(container);

			FlowReturn ret = app.create(StructrTraits.FLOW_RETURN, "ds").as(FlowReturn.class);
			ret.setDataSource(ds);
			ret.setFlowContainer(container);

			action.setNext(ret);

			container.setStartNode(action);

			result = container.evaluate(securityContext, flowParameters);
			assertNotNull(result);
			assertTrue("Expected iterable result", result instanceof Iterable);

			ds.setQuery("size(find('User'))");

			result = container.evaluate(securityContext, flowParameters);
			assertEquals("Expecting 6 users to have been created by this point.", 6, result);

			tx.success();

		} catch (Throwable ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}


	}

	@Test
	public void testFlowForEach() {

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(StructrTraits.FLOW_CONTAINER, "testFlowForEach").as(FlowContainer.class);

			FlowForEach forEach = app.create(StructrTraits.FLOW_FOR_EACH).as(FlowForEach.class);
			forEach.setFlowContainer(container);
			container.setStartNode(forEach);

			FlowDataSource ds = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			ds.setQuery("{return [1,2,3,4,5];}");
			ds.setFlowContainer(container);
			forEach.setDataSource(ds);

			FlowDataSource ds2 = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			ds2.setQuery("now");
			ds2.setFlowContainer(container);

			FlowAggregate agg = app.create(StructrTraits.FLOW_AGGREGATE).as(FlowAggregate.class);
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
	public void testFlowLogicElements() {

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(StructrTraits.FLOW_CONTAINER, "testFlowDecision").as(FlowContainer.class);

			FlowDecision flowDecision = app.create(StructrTraits.FLOW_DECISION).as(FlowDecision.class);
			flowDecision.setFlowContainer(container);
			container.setStartNode(flowDecision);

			FlowReturn flowReturnTrue = app.create(StructrTraits.FLOW_RETURN).as(FlowReturn.class);
			flowReturnTrue.setFlowContainer(container);
			flowReturnTrue.setResult("'SUCCESS'");
			flowDecision.setTrueElement(flowReturnTrue);

			FlowReturn flowReturnFalse = app.create(StructrTraits.FLOW_RETURN).as(FlowReturn.class);
			flowReturnFalse.setFlowContainer(container);
			flowReturnFalse.setResult("'FAILURE'");
			flowDecision.setFalseElement(flowReturnFalse);

			Object result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("Result of decision element with two returns should be equal to the failure return element.", "FAILURE", result);

			FlowAnd flowAnd  = app.create(StructrTraits.FLOW_AND).as(FlowAnd.class);
			flowAnd.setFlowContainer(container);
			flowDecision.setCondition(flowAnd);

			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("Empty AND element should yield failure result.", "FAILURE", result);

			// Get conditions for flowAnd
			List<FlowCondition> conditions = Iterables.toList(flowAnd.getConditions());

			FlowScriptCondition flowScriptCondition = app.create(StructrTraits.FLOW_SCRIPT_CONDITION).as(FlowScriptCondition.class);
			flowScriptCondition.setFlowContainer(container);
			flowScriptCondition.setScript("true");
			conditions.add(flowScriptCondition);

			// Update flowAnd with its new conditions
			flowAnd.setConditions(conditions);

			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowAnd should return true, since it only has one script condition returning true as well.", "SUCCESS", result);

			// Flip the output of the script condition
			flowScriptCondition.setScript("false");

			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("Result should be false after ScriptCondition has had it's output flipped.", "FAILURE", result);

			// Add second condition to flowAnd
			FlowNot flowNot = app.create(StructrTraits.FLOW_NOT).as(FlowNot.class);
			flowNot.setFlowContainer(container);
			conditions.add(flowNot);
			flowAnd.setConditions(conditions);

			FlowNotNull flowNotNull = app.create(StructrTraits.FLOW_NOT_NULL).as(FlowNotNull.class);
			flowNotNull.setFlowContainer(container);
			flowNot.setConditions(List.of(flowNotNull));

			// Revert flowScriptCondition back to true
			flowScriptCondition.setScript("true");

			// With flowNotNull(null)->flowNot && flowScriptCondition("true") -> flowAnd, we should get a success result
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("With flowNotNull(null)->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be SUCCESS", "SUCCESS", result);

			FlowDataSource flowNotNullDataSource = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			flowNotNullDataSource.setFlowContainer(container);
			flowNotNullDataSource.setDataTarget(List.of(flowNotNull));
			// Ensure datasource was properly linked
			assertEquals(1, Iterables.toList(flowNotNull.getDataSources()).size());

			// With flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition("true") -> flowAnd, we should get a success result
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be SUCCESS", "SUCCESS", result);

			flowNotNullDataSource.setQuery("'No longer empty'");
			// With flowNotNullDataSource('No longer empty')->flowNotNull->flowNot && flowScriptCondition("true") -> flowAnd, we should get a failure result
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowNotNullDataSource('No longer empty')->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be FAILURE", "FAILURE", result);

			// Set data source back to null, so the expression becomes true
			flowNotNullDataSource.setQuery(null);

			FlowOr flowOr = app.create(StructrTraits.FLOW_OR).as(FlowOr.class);
			flowOr.setFlowContainer(container);
			conditions.add(flowOr);
			flowAnd.setConditions(conditions);

			// With the former flowAnd conditions being true and our new florOr being false, we should get a failure
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowOr(null) && flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be FAILURE", "FAILURE", result);

			FlowNotEmpty flowNotEmpty = app.create(StructrTraits.FLOW_NOT_EMPTY).as(FlowNotEmpty.class);
			flowNotEmpty.setFlowContainer(container);
			flowOr.setConditions(List.of(flowNotEmpty));

			FlowCollectionDataSource flowCollectionDataSource = app.create(StructrTraits.FLOW_COLLECTION_DATA_SOURCE).as(FlowCollectionDataSource.class);
			flowCollectionDataSource.setFlowContainer(container);
			flowNotEmpty.setDataSources(List.of(flowCollectionDataSource));

			// FlowNotEmpty should detect empty collection data source as such and result should be FAILURE
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowCollectionDataSource(null)->flowNotEmpty->flowOr && flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be FAILURE", "FAILURE", result);

			FlowDataSource flowCollectionDataSourceDS1 = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			flowCollectionDataSourceDS1.setFlowContainer(container);
			flowCollectionDataSource.setDataSources(List.of(flowCollectionDataSourceDS1));

			// FlowNotEmpty should detect filled collection data source as such and result should be SUCCESS
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowCollectionDataSourceDS1->flowCollectionDataSource->flowNotEmpty->flowOr && flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be SUCCESS", "SUCCESS", result);

			FlowIsTrue flowIsTrue = app.create(StructrTraits.FLOW_IS_TRUE).as(FlowIsTrue.class);
			flowIsTrue.setFlowContainer(container);
			conditions.add(flowIsTrue);
			flowAnd.setConditions(conditions);

			// Newly added flowIsTrue in flowAnd conditions should turn the expression false
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowIsTrue(null) && flowCollectionDataSourceDS1->flowCollectionDataSource->flowNotEmpty->flowOr && flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be FAILURE", "FAILURE", result);

			FlowDataSource flowIsTrueDataSource = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			flowIsTrueDataSource.setFlowContainer(container);
			flowIsTrueDataSource.setQuery("true");
			flowIsTrue.setDataSources(List.of(flowIsTrueDataSource));

			// flowIsTrue should now make the entire expression evaluate as SUCCESS
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowIsTrueDataSource(\"true\")->flowIsTrue && flowCollectionDataSourceDS1->flowCollectionDataSource->flowNotEmpty->flowOr && flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be SUCCESS", "SUCCESS", result);

			flowIsTrueDataSource.setQuery("false");
			// flowIsTrue should now make the entire expression evaluate as FAILURE
			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("flowIsTrueDataSource(\"false\")->flowIsTrue && flowCollectionDataSourceDS1->flowCollectionDataSource->flowNotEmpty->flowOr && flowNotNullDataSource(null)->flowNotNull->flowNot && flowScriptCondition(\"true\") -> flowAnd, result should be FAILURE", "FAILURE", result);

			tx.success();

		} catch (Throwable ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testFlowComparison() {

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(StructrTraits.FLOW_CONTAINER, "testFlowComparison").as(FlowContainer.class);

			FlowDecision flowDecision = app.create(StructrTraits.FLOW_DECISION).as(FlowDecision.class);
			flowDecision.setFlowContainer(container);
			container.setStartNode(flowDecision);

			FlowReturn flowReturnTrue = app.create(StructrTraits.FLOW_RETURN).as(FlowReturn.class);
			flowReturnTrue.setFlowContainer(container);
			flowReturnTrue.setResult("'SUCCESS'");
			flowDecision.setTrueElement(flowReturnTrue);

			FlowReturn flowReturnFalse = app.create(StructrTraits.FLOW_RETURN).as(FlowReturn.class);
			flowReturnFalse.setFlowContainer(container);
			flowReturnFalse.setResult("'FAILURE'");
			flowDecision.setFalseElement(flowReturnFalse);

			FlowDataSource dataSource = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			dataSource.setFlowContainer(container);

			FlowDataSource valueSource = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			valueSource.setFlowContainer(container);

			FlowComparison flowComparison = app.create(StructrTraits.FLOW_COMPARISON).as(FlowComparison.class);
			flowComparison.setFlowContainer(container);
			flowComparison.setDecisions(List.of(flowDecision));
			flowComparison.setDataSources(List.of(dataSource));
			flowComparison.setValueSource(valueSource);
			flowComparison.setOperation(FlowComparison.Operation.equal);

			Object result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("Initial result should be SUCCESS since both value and data source are null.", "SUCCESS", result);

			dataSource.setQuery("123");

			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("Result should be FAILURE since 123 is not equal to value source of null.", "FAILURE", result);

			valueSource.setQuery("123");

			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("Result should be SUCCESS since 123 is equal to value source of 123.", "SUCCESS", result);

			flowComparison.setOperation(FlowComparison.Operation.greater);
			valueSource.setQuery("122");

			result = container.evaluate(securityContext, new HashMap<>());
            assertEquals("Result should be SUCCESS since 123 is greater than value source of 122.", "SUCCESS", result);

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
			final FlowContainer flowContainer = app.create(StructrTraits.FLOW_CONTAINER, "test").as(FlowContainer.class);
			final FlowTypeQuery query         = app.create(StructrTraits.FLOW_TYPE_QUERY, FlowDataSourceTraitDefinition.QUERY_PROPERTY).as(FlowTypeQuery.class);
			final FlowReturn    ret           = app.create(StructrTraits.FLOW_RETURN, "return").as(FlowReturn.class);

			query.setProperty(Traits.of(StructrTraits.FLOW_TYPE_QUERY).key(FlowTypeQueryTraitDefinition.DATA_TYPE_PROPERTY), StructrTraits.GROUP);
			query.setProperty(Traits.of(StructrTraits.FLOW_TYPE_QUERY).key(FlowDataSourceTraitDefinition.QUERY_PROPERTY), "{\"type\":\"group\",\"op\":\"and\",\"operations\":[{\"type\":\"sort\",\"key\":\"name\",\"order\":\"desc\",\"queryType\":\"Group\"}],\"queryType\":\"Group\"}");

			query.setFlowContainer(flowContainer);

			ret.setDataSource(query);
			ret.setFlowContainer(flowContainer);

			flowContainer.setStartNode(ret);

			// evaluate flow
			final Object result           = flowContainer.evaluate(securityContext, new LinkedHashMap<>());

			assertTrue("Result should be an iterable list of groups.", result instanceof Iterable);

			final List<Group> groups      = Iterables.toList((Iterable)result);

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

		.expect()
			.statusCode(200)
			.contentType("text/html;charset=utf-8")

			.body("html.body.div[0]", equalTo("group4"))

		.when()
			.get("/flowRepeaterTestPage");



	//"Strict-Transport-Security:max-age=60,X-Content-Type-Options:nosniff,X-Frame-Options:SAMEORIGIN,X-XSS-Protection:1;mode=block", "List of custom response headers that will be added to every HTTP response");

	}

	@Test
	public void testFlowObjectDataSource() {

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(StructrTraits.FLOW_CONTAINER, "testFlowForEach").as(FlowContainer.class);

			FlowReturn flowReturn = app.create(StructrTraits.FLOW_RETURN).as(FlowReturn.class);
			flowReturn.setFlowContainer(container);
			container.setStartNode(flowReturn);

			FlowObjectDataSource flowObjectDataSource = app.create(StructrTraits.FLOW_OBJECT_DATA_SOURCE).as(FlowObjectDataSource.class);
			flowObjectDataSource.setFlowContainer(container);
			flowReturn.setDataSource(flowObjectDataSource);

			FlowKeyValue flowKeyValue = app.create(StructrTraits.FLOW_KEY_VALUE).as(FlowKeyValue.class);
			flowKeyValue.setKey("test");
			flowKeyValue.setFlowContainer(container);
			flowKeyValue.setObjectDataTargets(List.of(flowObjectDataSource));

			FlowDataSource ds = app.create(StructrTraits.FLOW_DATA_SOURCE).as(FlowDataSource.class);
			ds.setQuery("'success'");
			ds.setFlowContainer(container);
			flowKeyValue.setDataSource(ds);

			final Object result = container.evaluate(securityContext, new HashMap<>());

			assertNotNull("Result should not be empty.", result);
			assertTrue("Result should be a map object.", result instanceof Map);
			assertEquals("Result should have 'success' as it's value for key 'test'.", "success", ((Map)result).get("test"));

			tx.success();

		} catch (Throwable ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}


	}

	@Test
	public void testFlowStartNode() {

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(StructrTraits.FLOW_CONTAINER, "testFlowStartNode").as(FlowContainer.class);

			FlowLog flowLog = app.create(StructrTraits.FLOW_LOG).as(FlowLog.class);
			flowLog.setFlowContainer(container);
			flowLog.setScript("'FlowLog start node is working.'");
			container.setStartNode(flowLog);

			container.evaluate(securityContext, new HashMap<>());

			tx.success();

		} catch (Throwable ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}


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
