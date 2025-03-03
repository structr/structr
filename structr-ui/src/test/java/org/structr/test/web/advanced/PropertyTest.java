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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class PropertyTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(PropertyTest.class);

	/**
	 * This test creates a new type "Test" with a Notion property that references a type (User)
	 * which is only present in the ui module.
	 */
	@Test
	public void testNotionProperty() {

		// schema setup
		try (final Tx tx = app.tx()) {

			final NodeInterface test  = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Test")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "ownerPrincipalEmail"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("propertyType"), "Notion"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("format"), "owner, User.eMail"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("schemaNode"), test)
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}
}





