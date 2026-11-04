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
package org.structr.appmap;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The REST endpoints an application opens up. A resource access grant on a method of a type or
 * on a user-defined function makes that method an entry point from outside, so it becomes an
 * endpoint of its own. The grants on a type (with or without a view or a relationship path)
 * together make one endpoint for the type, so that the requests to it show among the
 * integrations; they are also recorded as a property of the type, with whether they open it to
 * anonymous or to authenticated users. The endpoints of the users (login, logout, registration,
 * password reset, token and the current user) stand for the users, also when the application
 * does not extend the type User (see usersOf); whether an application grants them says who may
 * register, reset a password or take a token. A grant on a method of the current user
 * (me/method) opens that method of the users. Grants on the schema, on single objects by id, on
 * HTML elements and on the other built-in endpoints say nothing about the application and are
 * left out.
 */
public class EndpointMapContributor implements ApplicationMapContributor {

	// GET, PUT, POST, DELETE, OPTIONS, HEAD and PATCH for anonymous users, and the same for authenticated users, as the
	// flags of UiAuthenticator (which lives in the web layer, so the values are repeated here); PUT, POST, DELETE and PATCH
	// are the ones that write
	static final long PUBLIC_FLAGS              = 16 | 32 | 64 | 128 | 512 | 2048 | 8192;
	static final long AUTHENTICATED_FLAGS       = 1 | 2 | 4 | 8 | 256 | 1024 | 4096;
	static final long PUBLIC_WRITE_FLAGS        = 32 | 64 | 128 | 8192;
	static final long AUTHENTICATED_WRITE_FLAGS = 2 | 4 | 8 | 4096;

	// the signatures of the built-in endpoints that work on the users
	static final Set<String> USER_ENDPOINTS     = Set.of("_login", "_logout", "_registration", "_resetPassword", "_token", "me");

	// the id of the built-in users on the map when the application does not extend them
	static final String BUILTIN_USER_ID         = "type:" + StructrTraits.USER;

	@Override
	public void contribute(final ApplicationMap map, final App app) throws FrameworkException {

		final SchemaIndex schema = map.getSchema();

		for (final NodeInterface node : app.nodeQuery(StructrTraits.RESOURCE_ACCESS).getAsList()) {

			// a node with the label but without a known type comes back as a GenericNode
			if (!node.is(StructrTraits.RESOURCE_ACCESS)) {
				continue;
			}

			final ResourceAccess access = node.as(ResourceAccess.class);
			final String signature      = access.getResourceSignature();

			if (signature == null || signature.isEmpty() || signature.startsWith("/") || signature.startsWith("_id") || signature.startsWith("DOMElement")) {
				continue;
			}

			final String[] parts = signature.split("/");
			final String method  = methodOf(schema, parts);
			final long flags     = access.getFlags();

			if (method != null) {

				final String id = "api:" + signature;

				map.addNode(id, ApplicationMap.API, "/" + signature).setPublic(node.isVisibleToPublicUsers()).addRestAccess(flags);
				map.addReference(id, method, "endpoint", 1.5);

				continue;
			}

			final String type = schema.isType(parts[0]) ? schema.getTypeIds().get(parts[0]) : (USER_ENDPOINTS.contains(parts[0]) ? usersOf(map, schema) : null);

			if (type != null && map.getNode(type) != null) {

				final String id     = "api:" + parts[0];
				final boolean first = !map.hasNode(id);

				// the endpoints of the users are no access to the users as data
				if (schema.isType(parts[0])) {
					map.getNode(type).addRestAccess(flags);
				}

				map.addNode(id, ApplicationMap.API, "/" + parts[0]).setPublic(node.isVisibleToPublicUsers()).addRestAccess(flags);

				// the grants of a type and its views (Item, Item/_Ui) make one endpoint, joined to the type once
				if (first) {
					map.addReference(id, type, "endpoint", 1.5);
				}
			}
		}
	}

	// ----- private methods -----
	/**
	 * The method a grant opens up; older versions of Structr wrote the method name with a capital
	 * first letter into the signature (Contact/ChangeDepartment for changeDepartment), so both
	 * spellings count.
	 */
	private String methodOf(final SchemaIndex schema, final String[] parts) {

		if (parts.length == 2 && "me".equals(parts[0])) {
			return userMethod(schema, parts[1]);
		}

		if (parts.length == 2 && schema.isType(parts[0])) {
			return ObjectUtils.firstNonNull(schema.getTypeMethod(parts[0], parts[1]), schema.getTypeMethod(parts[0], StringUtils.uncapitalize(parts[1])));
		}

		if (!schema.isType(parts[0])) {
			return ObjectUtils.firstNonNull(schema.getGlobalMethod(parts[0]), schema.getGlobalMethod(StringUtils.uncapitalize(parts[0])));
		}

		return null;
	}

	/**
	 * The users on the map: the type User if the application extends it, else a type of its own
	 * for the built-in users, which has no schema node and so no id of one; the types that extend
	 * User inherit from it.
	 */
	private String usersOf(final ApplicationMap map, final SchemaIndex schema) {

		final String own = schema.getTypeIds().get(StructrTraits.USER);

		if (own != null) {
			return own;
		}

		if (!map.hasNode(BUILTIN_USER_ID)) {

			map.addNode(BUILTIN_USER_ID, ApplicationMap.TYPE, StructrTraits.USER);

			for (final Map.Entry<String, String> type : schema.getTypeIds().entrySet()) {

				if (schema.getParents(type.getKey()).contains(StructrTraits.USER)) {
					map.addEdge(type.getValue(), BUILTIN_USER_ID, "inherits", 1.5, type.getKey() + " inherits from " + StructrTraits.USER);
				}
			}
		}

		return BUILTIN_USER_ID;
	}

	// the method of the users a grant on me/method opens, in either spelling: one on User, else one on a type that extends
	// User, directly or further down, as long as only one of them has it
	private String userMethod(final SchemaIndex schema, final String name) {

		final String own = typeMethodOf(schema, StructrTraits.USER, name);

		if (own != null) {
			return own;
		}

		final List<String> found = schema.getTypeIds().keySet().stream()
			.filter(type -> extendsUser(schema, type, new HashSet<>()))
			.map(type -> typeMethodOf(schema, type, name))
			.filter(Objects::nonNull)
			.toList();

		return found.size() == 1 ? found.get(0) : null;
	}

	private String typeMethodOf(final SchemaIndex schema, final String type, final String name) {

		return ObjectUtils.firstNonNull(schema.getTypeMethod(type, name), schema.getTypeMethod(type, StringUtils.uncapitalize(name)));
	}

	private boolean extendsUser(final SchemaIndex schema, final String type, final Set<String> seen) {

		if (!seen.add(type)) {
			return false;
		}

		for (final String parent : schema.getParents(type)) {

			if (StructrTraits.USER.equals(parent) || extendsUser(schema, parent, seen)) {
				return true;
			}
		}

		return false;
	}
}
