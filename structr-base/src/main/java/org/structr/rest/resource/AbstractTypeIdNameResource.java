/*
 * Copyright (C) 2010-2023 Structr GmbH
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


import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.api.APICall;
import org.structr.api.APICallHandler;
import org.structr.api.APIEndpoint;
import org.structr.api.config.Settings;
import org.structr.api.parameter.APIParameter;
import org.structr.core.entity.SchemaNode;

/**
 * A resource that matches /{type}/{id}/{name} URLs.
 */
public abstract class AbstractTypeIdNameResource extends APIEndpoint {

	private static final APIParameter typeParameter = APIParameter.forPattern("type", SchemaNode.schemaNodeNamePattern);
	private static final APIParameter uuidParameter = APIParameter.forPattern("uuid", Settings.getValidUUIDRegexString());
	private static final APIParameter nameParameter = APIParameter.forPattern("name", "[a-z_A-Z][a-z_A-Z0-9]*");

	public AbstractTypeIdNameResource() {

		super(
			typeParameter,
			uuidParameter,
			nameParameter
		);
	}

	public abstract APICallHandler handleTypeIdName(final SecurityContext securityContext, final APICall call, final String typeName, final String uuid, final String name);

	@Override
	public APICallHandler accept(final SecurityContext securityContext, final APICall call) throws FrameworkException {

		final String typeName = call.get(typeParameter);
		final String uuid     = call.get(uuidParameter);
		final String name     = call.get(nameParameter);

		if (typeName != null && uuid != null && name != null) {

			return handleTypeIdName(securityContext, call, typeName, uuid, name);
		}

		return null;
	}
}
