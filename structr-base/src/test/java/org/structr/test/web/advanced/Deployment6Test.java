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

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.api.util.ResultStream;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.websocket.command.RemoveCommand;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.*;

public class Deployment6Test extends DeploymentTestBase {

	@Test
	public void test60PreventDeploymentExportOfNestedTemplatesInTrash() {

		String template2UUID = null;
		String template3UUID = null;

		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test60");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test05");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1  = createElement(page, body, "div");

			final Template template1 = createTemplate(page, div1, "template1");
			final Template template2 = createTemplate(page, template1, "template2");
			final Template template3 = createTemplate(page, template2, "template3");

			template2UUID = template2.getUuid();
			template3UUID = template3.getUuid();

			// remove pageId from node and all children ("move to trash")
			template2.getParent().removeChild(template2);
			RemoveCommand.recursivelyRemoveNodesFromPage(template2, securityContext);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			// make sure that the templates from the trash were not exported/imported

			assertNotNull(template2UUID);
			assertNotNull(template3UUID);

			final NodeInterface n1 = app.getNodeById(template2UUID);
			assertEquals("Template node should not be exported if it is in the trash", null, n1);

			final NodeInterface n2 = app.getNodeById(template3UUID);
			assertEquals("Template node should not be exported if it is in the trash as a child of another node", null, n2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test61ContentElementHasNoUselessTextAttributes() {

		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test42");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test42");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1 = createElement(page, body, "div");
			final DOMElement div2 = createElement(page, body, "div");
			final DOMElement div3 = createElement(page, body, "div");

			createContent(page,  div1, "my content text");
			createComment(page,  div2, "my comment text");
			createTemplate(page, div3, "my template text");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// run deployment
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			try (final ResultStream<NodeInterface> results = app.nodeQuery(StructrTraits.CONTENT).getResultStream()) {

				for (NodeInterface content : results) {

					assertFalse("After a deployment, no Content node should have an '_html_#text' attribute!", content.getPropertyContainer().hasProperty("_html_#text"));
				}
			}

			try (final ResultStream<NodeInterface> results = app.nodeQuery(StructrTraits.COMMENT).getResultStream()) {

				for (NodeInterface comment : results) {

					assertFalse("After a deployment, no Comment node should have an '_html_#comment' attribute!", comment.getPropertyContainer().hasProperty("_html_#comment"));
				}
			}

			try (final ResultStream<NodeInterface> results = app.nodeQuery(StructrTraits.TEMPLATE).getResultStream()) {

				for (NodeInterface template : results) {

					assertFalse("After a deployment, no Template node should have an '_html_src' attribute!", template.getPropertyContainer().hasProperty("_html_src"));
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test62CypherPropertyRoundtrip() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addFunctionProperty("testProperty1").setContentType("application/x-cypher").setFormat("MATCH (n:" + randomTenantId + ":SchemaNode) RETURN n");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create("Test");
			final PropertyKey key    = Traits.of("Test").key("testProperty1");
			final Object result      = node.getProperty(key);
			final List<Object> list  = Iterables.toList((Iterable) result);

			assertEquals("Invalid cypher property result before deployment roundtrip.", 1, list.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// run deployment
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create("Test");
			final PropertyKey key    = Traits.of("Test").key("testProperty1");
			final Object result      = node.getProperty(key);

			assertNotNull("Invalid cypher property result after deployment roundtrip.", result);

			final List<Object> list  = Iterables.toList((Iterable) result);

			assertEquals("Invalid cypher property result after deployment roundtrip.", 1, list.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test63FunctionPropertyRoundtrip() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addFunctionProperty("testProperty1").setReadFunction("find('SchemaNode')");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create("Test");
			final PropertyKey key    = Traits.of("Test").key("testProperty1");
			final Object result      = node.getProperty(key);
			final List<Object> list  = Iterables.toList((Iterable) result);

			assertEquals("Invalid cypher property result before deployment roundtrip.", 1, list.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// run deployment
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create("Test");
			final PropertyKey key    = Traits.of("Test").key("testProperty1");
			final Object result      = node.getProperty(key);

			assertNotNull("Invalid function property result after deployment roundtrip.", result);

			final List<Object> list  = Iterables.toList((Iterable) result);

			assertEquals("Invalid cypher property result after deployment roundtrip.", 1, list.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}
}