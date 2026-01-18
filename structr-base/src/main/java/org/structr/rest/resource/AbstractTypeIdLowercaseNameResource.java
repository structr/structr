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
package org.structr.rest.resource;


import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.WildcardMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 * A resource that matches /{type}/{id}/{name} URLs.
 */
public abstract class AbstractTypeIdLowercaseNameResource extends WildcardMatchEndpoint {

	public AbstractTypeIdLowercaseNameResource() {

		super(
			RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern, true),
			RESTParameter.forPattern("uuid", Settings.getValidUUIDRegexStringForURLParts(), true, "_id"),
			RESTParameter.forPattern("name", "[a-z][a-z_A-Z0-9]*", true)
		);
	}

	public abstract RESTCallHandler handleTypeIdName(final RESTCall call, final String typeName, final String uuid, final String name) throws FrameworkException;

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		final String typeName = call.get("type");
		final String uuid     = call.get("uuid");
		final String name     = call.get("name");

		if (typeName != null && uuid != null && name != null) {

			return handleTypeIdName(call, typeName, uuid, name);
		}

		return null;
	}
}
