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
package org.structr.test.core.script;

import org.structr.common.error.FrameworkException;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

public class StructrScriptTest extends StructrTest {

	@Test
	public void testObjectSyntax() {

		final ActionContext ctx = new ActionContext(securityContext);

		try {

			final String script = "${ { name: '{}', test: \"moep\", value: 1, children: [ { name: '[{][' }, { name: '}}]' } ] } }";

			final Object value = Scripting.evaluate(ctx, null, script,  "test");

			assertEquals("StructrScript does not parse object syntax correctly", "HashMap", value.getClass().getSimpleName());

			final Map map = (Map)value;

			assertEquals("StructrScript does not parse object syntax correctly", "{}", map.get("name"));
			assertEquals("StructrScript does not parse object syntax correctly", "moep", map.get("test"));
			assertEquals("StructrScript does not parse object syntax correctly", 1.0, map.get("value"));
			assertEquals("StructrScript does not parse object syntax correctly", "[{][", ((Map)((List)map.get("children")).get(0)).get("name"));
			assertEquals("StructrScript does not parse object syntax correctly", "}}]", ((Map)((List)map.get("children")).get(1)).get("name"));

			System.out.println(value);

		} catch (FrameworkException e) {
			throw new RuntimeException(e);
		}
	}
}
