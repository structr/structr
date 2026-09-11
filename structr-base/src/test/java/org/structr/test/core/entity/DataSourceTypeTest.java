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
package org.structr.test.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Whether the DataSource base type behaves like the abstract type it is.
 *
 * It is registered as a node type so that it can be queried - a query for DataSource is how the UI
 * collects everything that is one - but it has no implementation of its own: every operation in
 * DataSourceTraitDefinition throws, so a node of this exact type could not deliver values, fields or a
 * data type. The type refuses instantiation itself, because TraitsImplementation.isAbstract() is
 * hardcoded to false and the framework therefore has no way to mark a built-in type as abstract.
 */
public class DataSourceTypeTest extends StructrTest {

	@Test
	public void testTheBaseTypeCannotBeInstantiated() {

		try {

			try (final Tx tx = app.tx()) {

				app.create(StructrTraits.DATA_SOURCE, "aBareDataSource");

				tx.success();
			}

			fail("creating a node of the abstract type " + StructrTraits.DATA_SOURCE + " must not be possible");

		} catch (FrameworkException fex) {

			assertEquals("creating an abstract type must be rejected as unprocessable", 422, fex.getStatus());
		}
	}

	@Test
	public void testConcreteDataSourceTypesCanBeInstantiated() {

		// the guard must key on the exact type, not on the trait, or it would block every data source
		for (final String type : List.of(StructrTraits.SCRIPT_DATA_SOURCE, StructrTraits.QUERY_DATA_SOURCE)) {

			try (final Tx tx = app.tx()) {

				assertNotNull("a concrete data source type must still be creatable: " + type, app.create(type, type + "Instance"));

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception while creating " + type + ": " + fex.getMessage());
			}
		}
	}

	@Test
	public void testTheBaseTypeIsStillQueryable() {

		// the reason the type stays registered: the UI collects data sources by querying for the base
		// type, and a query for a type the framework does not know returns a 404 instead of a result
		assertTrue("the DataSource type must remain registered", Traits.exists(StructrTraits.DATA_SOURCE));

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCRIPT_DATA_SOURCE, "aScriptDataSource");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			assertEquals("a query for the base type must find the concrete data source", 1, app.nodeQuery(StructrTraits.DATA_SOURCE).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}
}
