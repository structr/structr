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
package org.structr.web.common;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;

/**
 */
public class TestHelper {

	public static void testViews(final App app, final InputStream specificationSource, final Map<String, List<String>> additionalRequiredAttributes) {

		final Set<String> useLowercaseNameForTypes           = Set.of(StructrTraits.SCHEMA_METHOD);
		final Map<String, Map<String, List<String>>> viewMap = new LinkedHashMap<>();
		final Map<String, List<String>> requiredAttributes   = new LinkedHashMap<>();
		final Map<String, List<String>> baseMap              = new LinkedHashMap<>();
		boolean fail                                         = false;

		baseMap.put("ui",     Arrays.asList("id", "type", "name", "owner", "createdBy", "hidden", "createdDate", "lastModifiedDate", "visibleToPublicUsers", "visibleToAuthenticatedUsers"));
		baseMap.put("_html_", Arrays.asList("_html_data", "_html_is", "_html_properties"));
		baseMap.put("public", Arrays.asList("id", "type"));

		requiredAttributes.put(StructrTraits.DYNAMIC_RESOURCE_ACCESS, Arrays.asList("signature", "i:flags"));
		requiredAttributes.put(StructrTraits.LOCALIZATION,          Arrays.asList("locale"));
		requiredAttributes.put(StructrTraits.RESOURCE_ACCESS,        Arrays.asList("signature", "i:flags"));

		// insert required attributes specified by test class
		if (additionalRequiredAttributes != null) {

			for (final Entry<String, List<String>> entry : additionalRequiredAttributes.entrySet()) {

				final String key       = entry.getKey();
				final List<String> add = entry.getValue();

				List<String> list = requiredAttributes.get(key);
				if (list != null) {

					list.addAll(add);

				} else {

					requiredAttributes.put(key, add);
				}
			}
		}

		// load specs
		try (final InputStream is = specificationSource) {

			// build view map from specification
			for (final String line : IOUtils.readLines(is, "utf-8")) {

				if (StringUtils.isNotBlank(line) && line.contains("=") && !line.trim().startsWith("#")) {

					// format is Type.viewName = list of property names
					final String[] parts      = line.split("=");
					final String[] keyParts   = parts[0].split("\\.");
					final String type         = keyParts[0];
					final String viewName     = keyParts[1];

					Map<String, List<String>> typeMap = viewMap.get(type);
					if (typeMap == null) {

						typeMap = new LinkedHashMap<>();
						viewMap.put(type, typeMap);
					}

					// "empty"views are possible too
					if (parts.length == 2) {

						final String[] valueParts = parts[1].split(",");
						final List<String> list   = new LinkedList<>(Arrays.asList(valueParts));
						final List<String> base   = baseMap.get(viewName);

						// common properties
						if (base != null) {

							list.addAll(base);
						}

						typeMap.put(viewName, list);
					}
				}
			}

		} catch (IOException ioex) {
			throw new AssertionError("Unable to load view specification: " + ioex.getMessage());
		}

		// create test user
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			throw new AssertionError("Unexpected exception");
		}

		// create an instance of each of the internal types and check the views
		for (final Entry<String, Map<String, List<String>>> entry : viewMap.entrySet()) {

			final String typeName                   = entry.getKey();
			final Map<String, List<String>> typeMap = entry.getValue();
			final Traits type                       = Traits.of(typeName);

			if (type != null) {

				// only test node types for now..
				if (!type.isAbstract() && !type.isInterface() && !type.isRelationshipType()) {

					final String namePrefix = useLowercaseNameForTypes.contains(typeName) ? "test" : "Test";
					final String body       = createPostBody(requiredAttributes.get(typeName), namePrefix);

					// create entity
					final String uuid = StringUtils.substringAfterLast(RestAssured
						.given()
						.header("X-User",     "admin")
						.header("X-Password", "admin")
						//.filter(RequestLoggingFilter.logRequestTo(System.out))
						.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
						.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
						.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
						.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
						.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
						.body(body)
						.expect()
						.statusCode(201)
						.when()
						.post("/" + typeName)
						.header("Location"), "/");

					for (final Entry<String, List<String>> view : typeMap.entrySet()) {

						final String viewName    = view.getKey();
						final List<String> keys  = view.getValue();

						// check entity
						final Map<String, Object> result = RestAssured
							.given()
							.header("X-User",     "admin")
							.header("X-Password", "admin")
							.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
							.expect()
							.statusCode(200)
							.when()
							.get("/" + typeName + "/" + uuid + "/" + viewName)
							.andReturn()
							.body()
							.as(Map.class);

						final Map<String, Object> item = (Map<String, Object>)result.get("result");
						final Set<String> expectedKeys = new TreeSet<>(keys);
						final Set<String> itemKeySet   = item.keySet();

						expectedKeys.removeAll(itemKeySet);

						if (!expectedKeys.isEmpty()) {

							System.out.println(type.getName() + "." + viewName + " is missing the following keys: " + expectedKeys);
							fail = true;
						}

						expectedKeys.clear();
						expectedKeys.addAll(keys);

						itemKeySet.removeAll(expectedKeys);

						if (!itemKeySet.isEmpty()) {

							System.out.println(type.getName() + "." + viewName + " contains keys that are not listed in the specification: " + itemKeySet);
							fail = true;
						}
					}
				}
			}
		}

		if (fail) {
			throw new AssertionError("View configuration does not match expected configuration, see log output for details.");
		}
	}

	static int count = 0;

	private static String createPostBody(final List<String> required, final String namePrefix) {

		final StringBuilder body = new StringBuilder("{ name: ");

		body.append("'");
		body.append(namePrefix);
		body.append(++count);
		body.append("'");

		if (required != null) {

			for (final String key : required) {

				body.append(", ");

				// Integer
				if (key.startsWith("i:")) {

					body.append(key.substring(2));
					body.append(": ");
					body.append(count);

				// Boolean
				} else if (key.startsWith("b:")) {

					body.append(key.substring(2));
					body.append(": true");

				// Enum with language
				} else if (key.startsWith("lang:")) {

					body.append(key.substring(5));
					body.append(": \"de\"");

				} else {

					body.append(key);
					body.append(": '");
					body.append(key);
					body.append(count);
					body.append("'");
				}
			}
		}

		body.append(" }");

		return body.toString();
	}
}
