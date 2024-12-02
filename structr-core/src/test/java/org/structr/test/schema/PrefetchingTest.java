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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

public class PrefetchingTest extends StructrTest {

	@Test
	public void testPrefetching() {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final JsonSchema schema       = StructrSchema.createEmptySchema();
			final JsonObjectType base     = schema.addType("BaseType");
			final JsonObjectType customer = schema.addType("Customer");
			final JsonObjectType task     = schema.addType("Task");

			customer.setExtends(base);
			task.setExtends(base);

			customer.relate(task, "HAS_TASK", Cardinality.OneToMany, "customer", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

			SearchCommand.prefetch(StructrApp.getConfiguration().getNodeEntityClass("Customer"), null);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

}
