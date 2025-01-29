/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.schema;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedTypeException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class LifecycleMethodsTest extends StructrTest {

	@Test
	public void test01LifecycleMethodsSuccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");
			final JsonType logEntry = schema.addType("LogEntry");

			customer.addMethod("onNodeCreation", "create('LogEntry', 'name', concat('onNodeCreation: ', this.name))");
			customer.addMethod("onCreate", "create('LogEntry', 'name', concat('onCreate: ', this.name))");
			customer.addMethod("afterCreate", "create('LogEntry', 'name', concat('afterCreate: ', this.name))");
			customer.addMethod("onSave", "create('LogEntry', 'name', concat('onSave: ', this.name))");
			customer.addMethod("afterSave", "create('LogEntry', 'name', concat('afterSave: ', this.name))");
			customer.addMethod("onDelete", "create('LogEntry', 'name', concat('onDelete: ', this.name))");
			customer.addMethod("afterDelete", "create('LogEntry', 'name', concat('afterDelete: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Class<NodeInterface> customerType = StructrApp.getConfiguration().getNodeEntityClass("Customer");
		final Class<NodeInterface> logEntryType = StructrApp.getConfiguration().getNodeEntityClass("LogEntry");

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			customer.setProperty(AbstractNode.name, "Tester");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// delete object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			app.delete(customer);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// check results
		try (final Tx tx = app.tx()) {

			final List<AbstractNode> logEntries = (List)app.nodeQuery(logEntryType).sort(AbstractNode.name).getAsList();

			final AbstractNode afterCreate    = logEntries.get(0);
			final AbstractNode afterDelete    = logEntries.get(1);
			final AbstractNode afterSave      = logEntries.get(2);
			final AbstractNode onCreate       = logEntries.get(3);
			final AbstractNode onDelete       = logEntries.get(4);
			final AbstractNode onNodeCreation = logEntries.get(5);
			final AbstractNode onSave         = logEntries.get(6);

			assertEquals(onNodeCreation.getName(),    "onNodeCreation: Customer");
			assertEquals(onCreate.getName(),    "onCreate: Customer");
			assertEquals(afterCreate.getName(), "afterCreate: Customer");

			assertEquals(onSave.getName(),   "onSave: Tester");
			assertEquals(afterSave.getName(),"afterSave: Tester");

			// on deletion, "this" is still available, after deletion, it is not
			assertEquals(onDelete.getName(),    "onDelete: Tester");
			assertEquals(afterDelete.getName(), "afterDelete: ");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	@Test
	public void test02OnCreateFailure() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");

			customer.addMethod("onCreate", "assert(false, 422, concat('onCreate: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Class<NodeInterface> customerType = StructrApp.getConfiguration().getNodeEntityClass("Customer");

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (AssertException expected) {

			// this is expected
			assertEquals(expected.getMessage(), "onCreate: Customer");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}
	@Test
	public void test03OnSaveFailure() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");

			customer.addMethod("onSave", "assert(false, 422, concat('onSave: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Class<NodeInterface> customerType = StructrApp.getConfiguration().getNodeEntityClass("Customer");

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			customer.setProperty(AbstractNode.name, "Tester");

			tx.success();

		} catch (AssertException expected) {

			// this is expected
			assertEquals(expected.getMessage(), "onSave: Tester");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}
	@Test
	public void test04OnDeleteFailure() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");

			customer.addMethod("onDelete", "assert(false, 422, concat('onDelete: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Class<NodeInterface> customerType = StructrApp.getConfiguration().getNodeEntityClass("Customer");

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			customer.setProperty(AbstractNode.name, "Tester");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// delete object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			app.delete(customer);

			tx.success();

			fail("Error in onDelete did not cause transaction rollback!");

		} catch (AssertException expected) {

			// this is expected
			assertEquals(expected.getMessage(), "onDelete: Tester");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	@Test
	public void test05PropertyAccessInAfterDelete() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");
			final JsonType logEntry = schema.addType("LogEntry");

			customer.addMethod("afterDelete", "create('LogEntry', 'name', concat('afterDelete: ', data.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Class<NodeInterface> customerType = StructrApp.getConfiguration().getNodeEntityClass("Customer");
		final Class<NodeInterface> logEntryType = StructrApp.getConfiguration().getNodeEntityClass("LogEntry");

		// create object
		try (final Tx tx = app.tx()) {

			System.out.println("##################################################################");
			System.out.println(app.nodeQuery(SchemaNode.class).andName("Customer").getFirst().getGeneratedSourceCode(securityContext));
			System.out.println("##################################################################");

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException | UnlicensedTypeException fex) {

			fex.printStackTrace();
		}

		// delete object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			app.delete(customer);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// check results
		try (final Tx tx = app.tx()) {

			final List<AbstractNode> logEntries = (List)app.nodeQuery(logEntryType).sort(AbstractNode.name).getAsList();

			final AbstractNode afterDelete = logEntries.get(0);

			assertEquals(afterDelete.getName(), "afterDelete: Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}
}
