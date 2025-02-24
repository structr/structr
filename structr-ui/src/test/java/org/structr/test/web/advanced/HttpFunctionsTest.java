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

import com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class HttpFunctionsTest extends StructrUiTest {

	@Test
	public void testHttpFunctions() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key("name"),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key("isAdmin"),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// allow all access to group resource
		grant(StructrTraits.GROUP, 16383, true);

		try {

			final ActionContext ctx = new ActionContext(securityContext);
			final Gson gson         = new GsonBuilder().create();

			ctx.addHeader("X-User",     "admin");
			ctx.addHeader("X-Password", "admin");

			// test POST
			final GraphObjectMap postResponse  = (GraphObjectMap)Scripting.evaluate(ctx, null, "${POST('http://localhost:"  + httpPort + "/structr/rest/Group', '{ name: post }')}", "test");

			// extract response headers
			final Map<String, Object> response = postResponse.toMap();
			final Map<String, Object> headers  = (Map)response.get("headers");
			final String location              = (String)headers.get(StructrTraits.LOCATION);

			// test PUT
			Scripting.evaluate(ctx, null, "${PUT('" + location + "', '{ name: put }')}", "test");

			Object getResult = Scripting.evaluate(ctx, null, "${GET('" + location + "', 'application/json')}", "test");
			assertTrue(getResult instanceof GraphObjectMap);
			GraphObjectMap map = (GraphObjectMap)getResult;
			assertNotNull(map.getProperty(new GenericProperty<>(HttpHelper.FIELD_BODY)));
			final Map<String, Object> putResult = gson.fromJson((String)map.getProperty(new GenericProperty<>(HttpHelper.FIELD_BODY)), Map.class);
			assertMapPathValueIs(putResult, "result.name", "put");

			// test PATCH
			Scripting.evaluate(ctx, null, "${PATCH('" + location + "', '{ name: patch }')}", "test");

			getResult = Scripting.evaluate(ctx, null, "${GET('" + location + "', 'application/json')}", "test");
			assertTrue(getResult instanceof GraphObjectMap);
			map = (GraphObjectMap)getResult;
			assertNotNull(map.getProperty(new GenericProperty<>(HttpHelper.FIELD_BODY)));
			final Map<String, Object> patchResult = gson.fromJson((String)map.getProperty(new GenericProperty<>(HttpHelper.FIELD_BODY)), Map.class);
			assertMapPathValueIs(patchResult, "result.name", "patch");

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	public static Object getMapPathValue(final Map<String, Object> map, final String mapPath) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					final List list = (List)current;
					if (index >= list.size()) {

						return null;

					} else {

						current = list.get(index);
					}
				}

			} else if ("#".equals(part)) {

				if (current instanceof List) {

					return ((List)current).size();
				}

				if (current instanceof Map) {

					return ((Map)current).size();
				}

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		return current;
	}

	// static methods
	public static void assertMapPathValueIs(final Map<String, Object> map, final String mapPath, final Object value) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof Collection) {

					final Collection collection = (Collection)current;
					if (index >= collection.size()) {

						// value for nonexisting fields must be null
						assertEquals("Invalid map path result for " + mapPath, value, null);

						// nothing more to check here
						return;

					} else {

						current = Iterators.get(collection.iterator(), index);
					}
				}

			} else if ("#".equals(part)) {

				if (current instanceof List) {

					assertEquals("Invalid collection size for " + mapPath, value, ((List)current).size());

					// nothing more to check here
					return;
				}

				if (current instanceof Map) {

					assertEquals("Invalid map size for " + mapPath, value, ((Map)current).size());

					// nothing more to check here
					return;
				}

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		// ignore type of value if numerical (GSON defaults to double...)
		if (value instanceof Number && current instanceof Number) {

			assertEquals("Invalid map path result for " + mapPath, ((Number)value).doubleValue(), ((Number)current).doubleValue(), 0.0);

		} else {

			assertEquals("Invalid map path result for " + mapPath, value, current);
		}
	}
}
