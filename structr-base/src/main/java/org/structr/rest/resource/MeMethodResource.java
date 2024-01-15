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
package org.structr.rest.resource;

import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.WildcardMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 */
public class MeMethodResource extends WildcardMatchEndpoint {

	public MeMethodResource() {
		super(
			RESTParameter.forStaticString("me"),
			RESTParameter.forPattern("name", "[a-z][a-z_A-Z0-9]*")
		);
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		final String name       = call.get("name");
		final Class entityClass = call.getUserType();

		// use actual type of entity returned to support inheritance
		final AbstractMethod method = Methods.resolveMethod(entityClass, name);
		if (method != null && !method.isPrivate()) {

			return new MeMethodResourceHandler(call, method);
		}

		return null;
	}
}
