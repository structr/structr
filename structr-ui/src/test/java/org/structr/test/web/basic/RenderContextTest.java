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
package org.structr.test.web.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.function.GetFunction;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.test.web.entity.TestOne;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.A;
import org.testng.annotations.Test;
import org.w3c.dom.NodeList;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.*;



public class RenderContextTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(RenderContextTest.class.getName());

	@Test
	public void testVariableReplacementInDynamicTypes() {

		SchemaNode itemNode  = null;
		NodeInterface parent = null;
		NodeInterface child1 = null;
		NodeInterface child2 = null;

		try (final Tx tx = app.tx()) {

			itemNode = app.create(SchemaNode.class, new NodeAttribute(SchemaNode.name, "Item"));

			final PropertyMap properties = new PropertyMap();
			properties.put(SchemaRelationshipNode.sourceId, itemNode.getUuid());
			properties.put(SchemaRelationshipNode.targetId, itemNode.getUuid());
			properties.put(SchemaRelationshipNode.relationshipType, "CHILD");
			properties.put(SchemaRelationshipNode.sourceMultiplicity, "1");
			properties.put(SchemaRelationshipNode.targetMultiplicity, "*");
			properties.put(SchemaRelationshipNode.sourceJsonName, "parentItem");
			properties.put(SchemaRelationshipNode.targetJsonName, "children");

			app.create(SchemaRelationshipNode.class, properties);

			// compile the stuff
			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Class itemClass              = config.getNodeEntityClass("Item");
		final PropertyKey childrenProperty = StructrApp.key(itemClass, "children");

		// create parent/child relationship
		try (final Tx tx = app.tx()) {

			parent = app.create(itemClass);
			child1 = app.create(itemClass);
			child2 = app.create(itemClass);

			final List<NodeInterface> children = new LinkedList<>();
			children.add(child1);
			children.add(child2);

			parent.setProperties(parent.getSecurityContext(), new PropertyMap(childrenProperty, children));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		// verify that parent has two children
		try (final Tx tx = app.tx()) {

			// verify that parentItem can be accessed....
			final Object value = parent.getProperty(childrenProperty);

			assertTrue(value instanceof Iterable);

			final Collection coll = Iterables.toList((Iterable)value);
			assertEquals(2, coll.size());

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		// check property access in template expressions
		try (final Tx tx = app.tx()) {

			assertEquals(parent.toString(), Scripting.replaceVariables(new ActionContext(securityContext), child1, "${this.parentItem}"));

			tx.success();


		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}


	}

	@Test
	public void testFunctionEvaluationInDynamicTypes() {

		NodeInterface item  = null;

		try (final Tx tx = app.tx()) {

			app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Item"),
				new NodeAttribute(new StringProperty("_testMethodCalled"), "Boolean"),
				new NodeAttribute(new StringProperty("___testMethod"), "set(this, 'testMethodCalled', true)")
			);

			// compile the stuff
			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Class itemClass              = config.getNodeEntityClass("Item");

		// create parent/child relationship
		try (final Tx tx = app.tx()) {

			item = app.create(itemClass, new NodeAttribute(SchemaNode.name, "Item"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		// check property access in template expressions
		try (final Tx tx = app.tx()) {

			final RenderContext renderContext = new RenderContext(securityContext);
			renderContext.putDataObject("item", item);

			assertEquals("Invalid combined array dot syntax result: ", "Item", Scripting.replaceVariables(renderContext, item, "${find('Item')[0].name}"));

			Scripting.replaceVariables(renderContext, item, "${item.testMethod()}");
			assertEquals("Invalid method evaluation result: ", "true", Scripting.replaceVariables(renderContext, item, "${item.testMethodCalled}"));

			tx.success();


		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testNotionTransformedPropertyAccess() {

		NodeInterface project = null;
		NodeInterface task1    = null;
		NodeInterface task2    = null;
		NodeInterface task3    = null;

		try (final Tx tx = app.tx()) {

			final SchemaNode projectNode = app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Project"),
				new NodeAttribute(new StringProperty("_taskList"), "Notion(tasks, id, name)"),
				new NodeAttribute(new StringProperty("_taskNames"), "Notion(tasks, name)")
			);

			final SchemaNode taskNode = app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Task")
			);

			// create schema relationship
			final PropertyMap taskProperties = new PropertyMap();
			taskProperties.put(SchemaRelationshipNode.sourceNode, projectNode);
			taskProperties.put(SchemaRelationshipNode.targetNode, taskNode);
			taskProperties.put(SchemaRelationshipNode.relationshipType, "TASK");
			taskProperties.put(SchemaRelationshipNode.relationshipType, "TASK");
			taskProperties.put(SchemaRelationshipNode.sourceMultiplicity, "1");
			taskProperties.put(SchemaRelationshipNode.targetMultiplicity, "*");
			taskProperties.put(SchemaRelationshipNode.sourceJsonName, "project");
			taskProperties.put(SchemaRelationshipNode.targetJsonName, "tasks");

			app.create(SchemaRelationshipNode.class, taskProperties);

			// create schema relationship
			final PropertyMap currentTaskProperties = new PropertyMap();
			currentTaskProperties.put(SchemaRelationshipNode.sourceNode, projectNode);
			currentTaskProperties.put(SchemaRelationshipNode.targetNode, taskNode);
			currentTaskProperties.put(SchemaRelationshipNode.relationshipType, "CURRENT");
			currentTaskProperties.put(SchemaRelationshipNode.sourceMultiplicity, "1");
			currentTaskProperties.put(SchemaRelationshipNode.targetMultiplicity, "1");
			currentTaskProperties.put(SchemaRelationshipNode.sourceJsonName, "projectOfCurrentTask");
			currentTaskProperties.put(SchemaRelationshipNode.targetJsonName, "currentTask");

			app.create(SchemaRelationshipNode.class, currentTaskProperties);

			// compile the stuff
			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Class projectClass           = config.getNodeEntityClass("Project");
		final Class taskClass              = config.getNodeEntityClass("Task");
		final PropertyKey currentTaskKey   = StructrApp.key(projectClass, "currentTask");
		final PropertyKey tasksKey         = StructrApp.key(projectClass, "tasks");

		// create parent/child relationship
		try (final Tx tx = app.tx()) {

			project = app.create(projectClass, new NodeAttribute(SchemaNode.name, "Project1"));
			task1    = app.create(taskClass, new NodeAttribute(SchemaNode.name, "Task1"));
			task2    = app.create(taskClass, new NodeAttribute(SchemaNode.name, "Task2"));
			task3    = app.create(taskClass, new NodeAttribute(SchemaNode.name, "Task3"));

			// add task to project
			final List tasks = new LinkedList<>();
			tasks.add(task1);
			tasks.add(task2);
			tasks.add(task3);

			final PropertyMap projectProperties = new PropertyMap();
			projectProperties.put(tasksKey, tasks);
			projectProperties.put(currentTaskKey, task3);
			project.setProperties(project.getSecurityContext(), projectProperties);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		// check property access in template expressions
		try (final Tx tx = app.tx()) {

			final RenderContext renderContext = new RenderContext(securityContext);
			renderContext.putDataObject("project", project);
			renderContext.putDataObject("task", task1);

			assertEquals("Invalid dot syntax result: ", "Project1", Scripting.replaceVariables(renderContext, project, "${project.name}"));

			assertEquals("Invalid dot syntax result: ", "Task1", Scripting.replaceVariables(renderContext, project, "${project.tasks[0].name}"));
			assertEquals("Invalid dot syntax result: ", "Task2", Scripting.replaceVariables(renderContext, project, "${project.tasks[1].name}"));
			assertEquals("Invalid dot syntax result: ", "Task3", Scripting.replaceVariables(renderContext, project, "${project.tasks[2].name}"));

			assertEquals("Invalid dot syntax result: ", "[Task1, Task2, Task3]", Scripting.replaceVariables(renderContext, project, "${project.taskNames}"));
			assertEquals("Invalid dot syntax result: ", "Task1", Scripting.replaceVariables(renderContext, project, "${project.taskNames[0]}"));
			assertEquals("Invalid dot syntax result: ", "Task2", Scripting.replaceVariables(renderContext, project, "${project.taskNames[1]}"));
			assertEquals("Invalid dot syntax result: ", "Task3", Scripting.replaceVariables(renderContext, project, "${project.taskNames[2]}"));

			assertEquals("Invalid dot syntax result: ", "Task3", Scripting.replaceVariables(renderContext, project, "${project.currentTask.name}"));

			tx.success();


		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}
	}

	@Test
	public void testVariableReplacement() {

		String detailsId = null;
		String pageId    = null;
		String p1Id      = null;
		String p2Id      = null;
		String aId       = null;

		try (final Tx tx = app.tx()) {

			Page page                       = null;
			DOMNode html                    = null;
			DOMNode head                    = null;
			DOMNode body                    = null;
			DOMNode title                   = null;
			DOMNode h1                      = null;
			DOMNode div1                    = null;
			DOMNode p1                      = null;
			DOMNode div2                    = null;
			DOMNode p2                      = null;
			DOMNode div3                    = null;
			DOMNode p3                      = null;
			A a                             = null;
			DOMNode div4                    = null;
			DOMNode p4                      = null;

			detailsId = app.create(TestOne.class, "TestOne").getUuid();
			page      = Page.createNewPage(securityContext, "testpage");

			page.setProperties(page.getSecurityContext(), new PropertyMap(Page.visibleToPublicUsers, true));

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			html  = (DOMNode) page.createElement("html");
			head  = (DOMNode) page.createElement("head");
			body  = (DOMNode) page.createElement("body");
			title = (DOMNode) page.createElement("title");
			h1    = (DOMNode) page.createElement("h1");
			div1  = (DOMNode) page.createElement("div");
			p1    = (DOMNode) page.createElement("p");
			div2  = (DOMNode) page.createElement("div");
			p2    = (DOMNode) page.createElement("p");
			div3  = (DOMNode) page.createElement("div");
			p3    = (DOMNode) page.createElement("p");
			a     = (A) page.createElement("a");
			div4  = (DOMNode) page.createElement("div");
			p4    = (DOMNode) page.createElement("p");

			// add HTML element to page
			page.appendChild(html);

			// add HEAD and BODY elements to HTML
			html.appendChild(head);
			html.appendChild(body);

			// add TITLE element to HEAD
			head.appendChild(title);

			// add H1 element to BODY
			body.appendChild(h1);

			// add DIV element 1 to BODY
			body.appendChild(div1);
			div1.appendChild(p1);

			// add DIV element 2 to DIV
			div1.appendChild(div2);
			div2.appendChild(p2);

			// add DIV element 3 to DIV
			div2.appendChild(div3);
			div3.appendChild(p3);

			// add link to p3
			p3.appendChild(a);
			a.setLinkable(page);

			body.appendChild(div4);
			div4.appendChild(p4);

			final PropertyMap p4Properties = new PropertyMap();
			p4Properties.put(StructrApp.key(DOMElement.class, "restQuery"), "/divs");
			p4Properties.put(StructrApp.key(DOMElement.class, "dataKey"), "div");
			p4.setProperties(p4.getSecurityContext(), p4Properties);

			NodeList paragraphs = page.getElementsByTagName("p");
			assertEquals(p1, paragraphs.item(0));
			assertEquals(p2, paragraphs.item(1));
			assertEquals(p3, paragraphs.item(2));
			assertEquals(p4, paragraphs.item(3));

			// create users
			final User tester1 = app.create(User.class, new NodeAttribute<>(StructrApp.key(User.class, "name"), "tester1"), new NodeAttribute<>(StructrApp.key(User.class, "eMail"), "tester1@test.com"));
			final User tester2 = app.create(User.class, new NodeAttribute<>(StructrApp.key(User.class, "name"), "tester2"), new NodeAttribute<>(StructrApp.key(User.class, "eMail"), "tester2@test.com"));

			assertNotNull("User tester1 should exist.", tester1);
			assertNotNull("User tester2 should exist.", tester2);

			// create admin user for later use
			final PropertyMap adminProperties = new PropertyMap();
			adminProperties.put(StructrApp.key(User.class, "name"),     "admin");
			adminProperties.put(StructrApp.key(User.class, "password"), "admin");
			adminProperties.put(StructrApp.key(User.class, "isAdmin"),   true);

			app.create(User.class, adminProperties);

			pageId = page.getUuid();
			p1Id   = p1.getUuid();
			p2Id   = p2.getUuid();
			aId    = a.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final RenderContext ctx               = new RenderContext(securityContext);
			final NodeInterface detailsDataObject = app.getNodeById(detailsId);
			final Page page                       = app.get(Page.class, pageId);
			final DOMNode p1                      = app.get(DOMNode.class, p1Id);
			final DOMNode p2                      = app.get(DOMNode.class, p2Id);
			final DOMNode a                       = app.get(DOMNode.class, aId);
			TestOne testOne                       = null;

			ctx.setDetailsDataObject(detailsDataObject);
			ctx.setPage(page);

			// test for "empty" return value
			assertEquals("", Scripting.replaceVariables(ctx, p1, "${err}"));
			assertEquals("", Scripting.replaceVariables(ctx, p1, "${this.error}"));
			assertEquals("", Scripting.replaceVariables(ctx, p1, "${this.this.this.error}"));
			assertEquals("", Scripting.replaceVariables(ctx, p1, "${parent.error}"));
			assertEquals("", Scripting.replaceVariables(ctx, p1, "${this.owner}"));
			assertEquals("", Scripting.replaceVariables(ctx, p1, "${parent.owner}"));

			// other functions are tested in the ActionContextTest in structr-core, see there.
			assertEquals("true",  Scripting.replaceVariables(ctx, p1, "${true}"));
			assertEquals("false", Scripting.replaceVariables(ctx, p1, "${false}"));
			assertEquals("yes",   Scripting.replaceVariables(ctx, p1, "${if(true, \"yes\", \"no\")}"));
			assertEquals("no",    Scripting.replaceVariables(ctx, p1, "${if(false, \"yes\", \"no\")}"));
			assertEquals("true",  Scripting.replaceVariables(ctx, p1, "${if(true, true, false)}"));
			assertEquals("false", Scripting.replaceVariables(ctx, p1, "${if(false, true, false)}"));

			// test keywords
			assertEquals("${id} should evaluate to the ID if the current details object", detailsDataObject.getUuid(), Scripting.replaceVariables(ctx, p1, "${id}"));

			ctx.setDetailsDataObject(null);
			assertEquals("${id} should evaluate to the ID if the current details object", "abc12345", Scripting.replaceVariables(ctx, p1, "${id!abc12345}"));
			ctx.setDetailsDataObject(detailsDataObject);


			assertEquals("${id} should be equal to ${current.id}", "true", Scripting.replaceVariables(ctx, p1, "${equal(id, current.id)}"));

			assertEquals("", Scripting.replaceVariables(ctx, p1, "${if(true, null, \"no\")}"));
			assertEquals("", Scripting.replaceVariables(ctx, p1, "${null}"));

			assertEquals("Invalid replacement result", "/testpage?" + page.getUuid(), Scripting.replaceVariables(ctx, p1, "/${page.name}?${page.id}"));
			assertEquals("Invalid replacement result", "/testpage?" + page.getUuid(), Scripting.replaceVariables(ctx, a, "/${link.name}?${link.id}"));

			// these tests find single element => success
			assertEquals("Invalid replacement result", page.getUuid(), Scripting.replaceVariables(ctx, a, "${get(find('Page', 'name', 'testpage'), 'id')}"));
			assertEquals("Invalid replacement result", a.getUuid(), Scripting.replaceVariables(ctx, a, "${get(find('A'), 'id')}"));

			// this test finds multiple <p> elements => error
			assertEquals("Invalid replacement result", GetFunction.ERROR_MESSAGE_GET_ENTITY, Scripting.replaceVariables(ctx, a, "${get(find('P'), 'id')}"));

			// more complex replacement
			//assertEquals("Invalid replacement result", "", a.replaceVariables(ctx, securityContext, "${get(find('P'), 'id')}"));

			// String default value
			assertEquals("bar", Scripting.replaceVariables(ctx, p1, "${request.foo!bar}"));
			assertEquals("camelCase", Scripting.replaceVariables(ctx, p1, "${request.foo!camelCase}"));

			// Number default value (will be evaluated to a string)
			assertEquals("1", Scripting.replaceVariables(ctx, p1, "${page.position!1}"));

			// Number default value
			assertEquals("true", Scripting.replaceVariables(ctx, p1, "${equal(42, this.null!42)}"));


			final User tester1 = app.nodeQuery(User.class).andName("tester1").getFirst();
			final User tester2 = app.nodeQuery(User.class).andName("tester2").getFirst();

			assertNotNull("User tester1 should exist.", tester1);
			assertNotNull("User tester2 should exist.", tester2);

			final ActionContext tester1Context = new ActionContext(SecurityContext.getInstance(tester1, AccessMode.Backend));
			final ActionContext tester2Context = new ActionContext(SecurityContext.getInstance(tester2, AccessMode.Backend));

			// users
			assertEquals("tester1", Scripting.replaceVariables(tester1Context, p1, "${me.name}"));
			assertEquals("tester2", Scripting.replaceVariables(tester2Context, p2, "${me.name}"));

			// allow unauthenticated GET on /pages
			grant("Page/_Ui", 16, true);

			// test GET REST access
			assertEquals("Invalid GET notation result", page.getName(), Scripting.replaceVariables(ctx, p1, "${from_json(GET('http://localhost:" + httpPort + "/structr/rest/Page/ui').body).result[0].name}"));

			grant("Folder", 64, true);
			grant("_login", 64, false);

			assertEquals("Invalid POST result", "201",                            Scripting.replaceVariables(ctx, page, "${POST('http://localhost:" + httpPort + "/structr/rest/Folder', '{name:status}').status}"));
			assertEquals("Invalid POST result", "1.0",                            Scripting.replaceVariables(ctx, page, "${POST('http://localhost:" + httpPort + "/structr/rest/Folder', '{name:result_count}').body.result_count}"));
			assertEquals("Invalid POST result", "application/json;charset=utf-8", Scripting.replaceVariables(ctx, page, "${POST('http://localhost:" + httpPort + "/structr/rest/Folder', '{name:content-type}').headers.Content-Type}"));

			// test POST with invalid name containing curly braces to provoke 422
			assertEquals("Invalid POST result", "422",                             Scripting.replaceVariables(ctx, page, "${POST('http://localhost:" + httpPort + "/structr/rest/Folder', '{name:\"ShouldFail/xyz\"}').status}"));

			// test login and sessions
			final String sessionIdCookie = Scripting.replaceVariables(ctx, page, "${POST('http://localhost:" + httpPort + "/structr/rest/login', '{name:admin,password:admin}').headers.Set-Cookie}");
			final String sessionId       = HttpCookie.parse(sessionIdCookie).get(0).getValue();

			// test authenticated GET request using session ID cookie
			assertEquals("Invalid authenticated GET result", "admin",   Scripting.replaceVariables(ctx, page, "${add_header('Cookie', 'JSESSIONID=" + sessionId + ";Path=/')}${from_json(GET('http://localhost:" + httpPort + "/structr/rest/User?_sort=name').body).result[0].name}"));
			assertEquals("Invalid authenticated GET result", "tester1", Scripting.replaceVariables(ctx, page, "${add_header('Cookie', 'JSESSIONID=" + sessionId + ";Path=/')}${from_json(GET('http://localhost:" + httpPort + "/structr/rest/User?_sort=name').body).result[1].name}"));
			assertEquals("Invalid authenticated GET result", "tester2", Scripting.replaceVariables(ctx, page, "${add_header('Cookie', 'JSESSIONID=" + sessionId + ";Path=/')}${from_json(GET('http://localhost:" + httpPort + "/structr/rest/User?_sort=name').body).result[2].name}"));

			// locale
			final String localeString = ctx.getLocale().toString();
			assertEquals("Invalid locale result", localeString, Scripting.replaceVariables(ctx, page, "${locale}"));

			// set new details object
			final TestOne detailsDataObject2 = app.create(TestOne.class, "TestOne");
			Scripting.replaceVariables(ctx, p1, "${set_details_object(first(find('TestOne', 'id', '" + detailsDataObject2.getUuid() + "')))}");
			assertEquals("${current.id} should resolve to new details object", detailsDataObject2.getUuid(), Scripting.replaceVariables(ctx, p1, "${current.id}"));

			// test values() with single parameter
			assertEquals("Invalid values() result", "[test]", Scripting.replaceVariables(ctx, page, "${values(from_json('{name:test}'))}"));

			testOne = createTestNode(TestOne.class);
			testOne.setProperty(TestOne.htmlString, "<a b=\"c\">&d</a>");

			// escape_html
			assertEquals("Invalid escape_html() result", "&lt;a b=&quot;c&quot;&gt;&amp;d&lt;/a&gt;", Scripting.replaceVariables(ctx, testOne, "${escape_html(this.htmlString)}"));

			testOne.setProperty(TestOne.htmlString, "&lt;a b=&quot;c&quot;&gt;&amp;d&lt;/a&gt;");

			// unescape_html
			assertEquals("Invalid unescape_html() result", "<a b=\"c\">&d</a>", Scripting.replaceVariables(ctx, testOne, "${unescape_html(this.htmlString)}"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}
	}

	@Test
	public void testFiltering() {

		final List<TestOne> testOnes = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			testOnes.addAll(createTestNodes(TestOne.class, 20));

			final RenderContext ctx = new RenderContext(securityContext);
			final TestOne testOne   = testOnes.get(0);

			// test filtering
			ctx.setDetailsDataObject(testOnes.get(5));
			final Object value = Scripting.evaluate(ctx, testOne, "${filter(find('TestOne'), not(equal(data.id, current.id)))}", "test");

			assertNotNull("Invalid filter result", value);
			assertTrue("Invalid filter result", value instanceof List);
			assertEquals("Invalid filter result", 19, ((List)value).size());

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testScriptEvaluation() {

		try (final Tx tx = app.tx()) {

			// create a Project type
			final SchemaNode projectNode = createTestNode(SchemaNode.class, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "Project"));

			// create a Task type with a string property "task"
			final SchemaNode taskNode    = createTestNode(SchemaNode.class,
				new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "Task"),
				new NodeAttribute<>(new StringProperty("_task"), "String")
			);

			// create a schema relationship between them
			createTestNode(SchemaRelationshipNode.class,
				new NodeAttribute<>(StructrApp.key(SchemaRelationshipNode.class, "sourceNode"), projectNode),
				new NodeAttribute<>(StructrApp.key(SchemaRelationshipNode.class, "targetNode"), taskNode),
				new NodeAttribute<>(StructrApp.key(SchemaRelationshipNode.class, "relationshipType"), "has"),
				new NodeAttribute<>(StructrApp.key(SchemaRelationshipNode.class, "sourceMultiplicity"), "1"),
				new NodeAttribute<>(StructrApp.key(SchemaRelationshipNode.class, "targetMultiplicity"), "*"),
				new NodeAttribute<>(StructrApp.key(SchemaRelationshipNode.class, "sourceJsonName"), "project"),
				new NodeAttribute<>(StructrApp.key(SchemaRelationshipNode.class, "targetJsonName"), "tasks")
			);

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}


		try (final Tx tx = app.tx()) {

			// obtain class objects to create instances of the above types
			final Class projectType    = StructrApp.getConfiguration().getNodeEntityClass("Project");
			final Class taskType       = StructrApp.getConfiguration().getNodeEntityClass("Task");
			final PropertyKey taskKey  = StructrApp.key(taskType, "task");
			final PropertyKey tasksKey = StructrApp.key(projectType, "tasks");

			final List<NodeInterface> tasks = new LinkedList<>();

			tasks.add(app.create(taskType, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "Task 1"), new NodeAttribute<>(taskKey, "Task 1")));
			tasks.add(app.create(taskType, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "Task 2"), new NodeAttribute<>(taskKey, "Task 2")));
			tasks.add(app.create(taskType, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "Task 3"), new NodeAttribute<>(taskKey, "Task 3")));

			// create a project and a task
			final NodeInterface project = app.create(projectType,
				new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "project"),
				new NodeAttribute<>(tasksKey, tasks)
			);

			// create an additional test task without a project
			final NodeInterface testTask = app.create(taskType, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "test task"), new NodeAttribute<>(taskKey, "test task"));
			final RenderContext renderContext = new RenderContext(securityContext);

			renderContext.putDataObject("project", project);
			renderContext.putDataObject("task", testTask);

			assertEquals("Invalid scripting evaluation result", "",                     Scripting.replaceVariables(renderContext, null, "${foo.page}"));

			assertEquals("Invalid scripting evaluation result", testTask.getUuid(),     Scripting.replaceVariables(renderContext, null, "${task}"));
			assertEquals("Invalid scripting evaluation result", "test task",            Scripting.replaceVariables(renderContext, null, "${task.task}"));
			assertEquals("Invalid scripting evaluation result", tasks.toString(),       Scripting.replaceVariables(renderContext, null, "${project.tasks}"));

			assertEquals("Invalid scripting evaluation result", tasks.get(0).getUuid(), Scripting.replaceVariables(renderContext, null, "${project.tasks[0]}"));
			assertEquals("Invalid scripting evaluation result", tasks.get(1).getUuid(), Scripting.replaceVariables(renderContext, null, "${project.tasks[1]}"));
			assertEquals("Invalid scripting evaluation result", tasks.get(2).getUuid(), Scripting.replaceVariables(renderContext, null, "${project.tasks[2]}"));

			assertEquals("Invalid scripting evaluation result", "", Scripting.replaceVariables(renderContext, null, "${project.tasks[3]}"));

			assertEquals("Invalid scripting evaluation result", "Task 1", Scripting.replaceVariables(renderContext, null, "${project.tasks[0].task}"));
			assertEquals("Invalid scripting evaluation result", "Task 2", Scripting.replaceVariables(renderContext, null, "${project.tasks[1].task}"));
			assertEquals("Invalid scripting evaluation result", "Task 3", Scripting.replaceVariables(renderContext, null, "${project.tasks[2].task}"));

			assertEquals("Invalid scripting evaluation result", "", Scripting.replaceVariables(renderContext, null, "${project.tasks[3].task}"));

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void testAnyAllAndNoneFunctions1() {

		final ActionContext ctx = new ActionContext(securityContext, null);
		Principal user          = null;
		TestOne test            = null;

		try (final Tx tx = app.tx()) {

			user = app.create(User.class, "user1");
			test = app.create(TestOne.class, "test1");

			app.create(GroupTraitDefinition.class,
				new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "group1"),
				new NodeAttribute<>(StructrApp.key(GroupTraitDefinition.class, "members"), Arrays.asList(new Principal[] { user } ))
			);

			final Group group2 = app.create(GroupTraitDefinition.class,
				new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "group2"),
				new NodeAttribute<>(StructrApp.key(GroupTraitDefinition.class, "members"), Arrays.asList(new Principal[] { user } ))
			);

			app.create(GroupTraitDefinition.class,
				new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "group3"),
				new NodeAttribute<>(StructrApp.key(GroupTraitDefinition.class, "members"), Arrays.asList(new Principal[] { user } ))
			);

			test.setProperty(AbstractNode.owner, group2);

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {

			ctx.setConstant("user", user);
			ctx.setConstant("test", test);

                        assertEquals("Invalid any() result",   "true", Scripting.replaceVariables(ctx, null, "${any(user.groups, is_allowed(data, test, 'read'))}"));
                        assertEquals("Invalid all() result",  "false", Scripting.replaceVariables(ctx, null, "${all(user.groups, is_allowed(data, test, 'read'))}"));
                        assertEquals("Invalid none() result", "false", Scripting.replaceVariables(ctx, null, "${none(user.groups, is_allowed(data, test, 'read'))}"));

			tx.success();

		} catch (FrameworkException ex) {

                        logger.warn("", ex);
                        fail("Unexpected exception");

                }

	}

	@Test
	public void testAnyAllAndNoneFunctions2() {

		final ActionContext ctx = new ActionContext(securityContext, null);

		try (final Tx tx = app.tx()) {

			// expectations (we use Boolean.valueOf(value.toString())
			//   true  == true
			//  "true" == true
			//  false  == false
			// "false" == false
			//      1  == false
			//      0  == false
			//  "test" == false

                        assertEquals("Invalid any() result",   "true", Scripting.replaceVariables(ctx, null, "${any(merge(false, false, false, true), data)}"));
                        assertEquals("Invalid any() result",  "false", Scripting.replaceVariables(ctx, null, "${any(merge(false, false, false, false), data)}"));
                        assertEquals("Invalid any() result",  "false", Scripting.replaceVariables(ctx, null, "${any(merge(false, false, false, 1), data)}"));
                        assertEquals("Invalid any() result",  "false", Scripting.replaceVariables(ctx, null, "${any(merge(false, false, false, 0), data)}"));
                        assertEquals("Invalid any() result",  "true", Scripting.replaceVariables(ctx, null, "${any(merge(false, false, false, 'true'), data)}"));
                        assertEquals("Invalid any() result",  "false", Scripting.replaceVariables(ctx, null, "${any(merge(false, false, false, 'false'), data)}"));
                        assertEquals("Invalid any() result",  "false", Scripting.replaceVariables(ctx, null, "${any(merge(false, false, false, 'test'), data)}"));

                        assertEquals("Invalid all() result",   "true", Scripting.replaceVariables(ctx, null, "${all(merge(true, true, true, true), data)}"));
                        assertEquals("Invalid all() result",  "false", Scripting.replaceVariables(ctx, null, "${all(merge(true, true, true, true, false), data)}"));
                        assertEquals("Invalid all() result",  "false", Scripting.replaceVariables(ctx, null, "${all(merge(true, true, true, true, 1), data)}"));
                        assertEquals("Invalid all() result",  "false", Scripting.replaceVariables(ctx, null, "${all(merge(true, true, true, true, 0), data)}"));
                        assertEquals("Invalid all() result",   "true", Scripting.replaceVariables(ctx, null, "${all(merge(true, true, true, true, 'true'), data)}"));
                        assertEquals("Invalid all() result",  "false", Scripting.replaceVariables(ctx, null, "${all(merge(true, true, true, true, 'false'), data)}"));
                        assertEquals("Invalid all() result",  "false", Scripting.replaceVariables(ctx, null, "${all(merge(true, true, true, true, 'test'), data)}"));

                        assertEquals("Invalid none() result",  "true", Scripting.replaceVariables(ctx, null, "${none(merge(false, false, false), data)}"));
                        assertEquals("Invalid none() result", "false", Scripting.replaceVariables(ctx, null, "${none(merge(false, false, false, true), data)}"));
                        assertEquals("Invalid none() result",  "true", Scripting.replaceVariables(ctx, null, "${none(merge(false, false, false, 1), data)}"));
                        assertEquals("Invalid none() result",  "true", Scripting.replaceVariables(ctx, null, "${none(merge(false, false, false, 0), data)}"));
                        assertEquals("Invalid none() result", "false", Scripting.replaceVariables(ctx, null, "${none(merge(false, false, false, 'true'), data)}"));
                        assertEquals("Invalid none() result",  "true", Scripting.replaceVariables(ctx, null, "${none(merge(false, false, false, 'false'), data)}"));
                        assertEquals("Invalid none() result",  "true", Scripting.replaceVariables(ctx, null, "${none(merge(false, false, false, 'test'), data)}"));

			tx.success();

		} catch (FrameworkException ex) {

                        logger.warn("", ex);
                        fail("Unexpected exception");

                }

	}
}
