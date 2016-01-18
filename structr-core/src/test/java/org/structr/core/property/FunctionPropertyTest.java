/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.property;

import java.util.Map;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class FunctionPropertyTest extends StructrTest {

	@Override
	protected void setUp(Map<String, Object> additionalConfig) {
		super.setUp(toMap("NodeExtender.log", "true"));
	}
	
	public void testEscaping() {

		// create test node with offending quote

		try (final Tx tx = app.tx()) {

			final PropertyMap properties = new PropertyMap();

			properties.put(AbstractNode.name, "Test");
			properties.put(new StringProperty("_functionTest"), "Function({ // \"})");

			app.create(SchemaNode.class, properties);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

}
