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
package org.structr.web.resource;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 * A resource to reset a user's password.
 */
public class ResetPasswordResource extends ExactMatchEndpoint {

	public ResetPasswordResource() {
		super(RESTParameter.forStaticString("reset-password"));
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {
		return new ResetPasswordResourceHandler(securityContext, call);
	}
}
