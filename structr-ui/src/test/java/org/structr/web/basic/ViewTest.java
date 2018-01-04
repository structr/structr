/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.basic;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.StructrUiTest;
import org.structr.web.entity.User;


public class ViewTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(ViewTest.class.getName());

	private static final Map<String, List<Map<String, List<String>>>> viewMap = new LinkedHashMap<>();

	static {

		final String[] publicBase = { "id", "type" };
		final String[] uiBase     = { "id", "name", "owner", "type", "createdBy", "deleted", "hidden", "createdDate", "lastModifiedDate", "visibleToPublicUsers", "visibleToAuthenticatedUsers" };


		viewMap.put("Group", Arrays.asList(
				toMap("public", publicBase, "name", "members", "blocked", "isGroup"),
				toMap("public", uiBase,     "members", "blocked", "isGroup")
			)
		);

		/*

		viewMap.put("ResourceAccess", Arrays.asList(
				toMap("public", publicBase, "signature", "flags", "isResourceAccess"),
				toMap("ui",     uiBase,     "signature", "flags", "position", "isResourceAccess")
			)
		);

		viewMap.put("User", Arrays.asList(
				toMap("public", publicBase, "name", "isUser"),
				toMap("ui",     uiBase,     "name", "eMail", "isAdmin", "password", "publicKey", "blocked", "sessionIds", "confirmationKey", "backendUser", "frontendUser", "groups", "img", "homeDirectory", "workingDirectory", "isUser", "locale", "favorites", "proxyUrl", "proxyUsername", "proxyPassword", "skipSecurityRelationships")
			)
		);

		viewMap.put("File", Arrays.asList(
				toMap("public", publicBase, "name", "size", "url", "owner", "path", "isFile", "visibleToPublicUsers", "visibleToAuthenticatedUsers", "includeInFrontendExport", "isFavoritable", "isTemplate", "contentType"),
				toMap("ui",     uiBase,     "path", "isFolder", "folders", "files", "parentId")
			)
		);

		viewMap.put("Folder", Arrays.asList(
				toMap("public", publicBase, "name", "isFolder")
			)
		);
		*/
	}

	@Test
	public void testViews() {

		// create test user
		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// create an instance of each of the internal types and check the views
		for (final Entry<String, List<Map<String, List<String>>>> entry : viewMap.entrySet()) {

			final String typeName                      = entry.getKey();
			final List<Map<String, List<String>>> list = entry.getValue();
			final Class type                           = StructrApp.getConfiguration().getNodeEntityClass(typeName);
			int i                                      = 0;

			System.out.println("####################################### Testing " + typeName + "..");

			assertNotNull("Type " + type + " should exist", type);

			for (final Map<String, List<String>> listEntry : list) {

				for (final Entry<String, List<String>> view : listEntry.entrySet()) {

					final String viewName    = view.getKey();
					final List<String> keys  = view.getValue();

					System.out.println(viewName + "-> " + keys);

					// create entity
					final String uuid = StringUtils.substringAfterLast(RestAssured
						.given()
						.header("X-User", "admin")
						.header("X-Password", "admin")
						.body("{ name: 'test" + i++ + "' }")
						.expect()
						.statusCode(201)
						.when()
						.post("/" + typeName)
						.header("Location"), "/");

					// check entity
					RestAssured
						.given()
						.filter(ResponseLoggingFilter.logResponseTo(System.out))
						.header("X-User", "admin")
						.header("X-Password", "admin")
						.expect()
						.statusCode(200)
						.body("result", Matchers.allOf(keys.stream().map(k -> Matchers.hasKey(k)).collect(Collectors.toList())))
						.when()
						.get("/" + typeName + "/" + uuid + "/" + viewName);
				}

			}
		}

	}

	// ----- private methods -----
	private static Map<String, List<String>> toMap(final String key, final String[] base, final String... elements) {


		final Map<String, List<String>> map = new LinkedHashMap<>();
		final List<String> list             = new LinkedList<>();

		list.addAll(Arrays.asList(base));
		list.addAll(Arrays.asList(elements));

		map.put(key, list);

		return map;
	}
}
