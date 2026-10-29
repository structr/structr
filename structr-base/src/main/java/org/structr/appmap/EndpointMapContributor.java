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

/**
 * The REST endpoints an application opens up. A resource access grant on a method of a type or
 * on a user-defined function makes that method an entry point from outside, so it becomes an
 * endpoint of its own; a grant on a type (with or without a view or a relationship path) only
 * opens the type to REST and is recorded as a property of the type, with whether it opens it to
 * anonymous or to authenticated users; the login endpoint opens the users. Grants on the schema,
 * on single objects by id and on HTML elements say nothing about the application and are left out.
 */
public class EndpointMapContributor implements ApplicationMapContributor {

	// GET, PUT, POST, DELETE, OPTIONS, HEAD and PATCH for anonymous users, and the same for authenticated users, as the
	// flags of UiAuthenticator (which lives in the web layer, so the values are repeated here); PUT, POST, DELETE and PATCH
	// are the ones that write
	static final long PUBLIC_FLAGS              = 16 | 32 | 64 | 128 | 512 | 2048 | 8192;
	static final long AUTHENTICATED_FLAGS       = 1 | 2 | 4 | 8 | 256 | 1024 | 4096;
	static final long PUBLIC_WRITE_FLAGS        = 32 | 64 | 128 | 8192;
	static final long AUTHENTICATED_WRITE_FLAGS = 2 | 4 | 8 | 4096;

	@Override
	public void contribute(final ApplicationMap map, final App app) throws FrameworkException {

		final SchemaIndex schema = map.getSchema();

		for (final NodeInterface node : app.nodeQuery(StructrTraits.RESOURCE_ACCESS).getAsList()) {

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

			final String type = typeOf(schema, signature, parts);

			if (type != null && map.getNode(type) != null) {
				map.getNode(type).addRestAccess(flags);
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

		if (parts.length == 2 && schema.isType(parts[0])) {
			return ObjectUtils.firstNonNull(schema.getTypeMethod(parts[0], parts[1]), schema.getTypeMethod(parts[0], StringUtils.uncapitalize(parts[1])));
		}

		if (!schema.isType(parts[0])) {
			return ObjectUtils.firstNonNull(schema.getGlobalMethod(parts[0]), schema.getGlobalMethod(StringUtils.uncapitalize(parts[0])));
		}

		return null;
	}

	private String typeOf(final SchemaIndex schema, final String signature, final String[] parts) {

		if (schema.isType(parts[0])) {
			return schema.getTypeIds().get(parts[0]);
		}

		if ("_login".equals(signature)) {
			return schema.getTypeIds().get(StructrTraits.USER);
		}

		return null;
	}
}
