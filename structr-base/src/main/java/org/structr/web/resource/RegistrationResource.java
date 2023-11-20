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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.api.APICall;
import org.structr.api.APICallHandler;
import org.structr.api.APIEndpoint;
import org.structr.api.parameter.APIParameter;

/**
 * A resource to register new users.
 */
public class RegistrationResource extends APIEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(RegistrationResource.class.getName());

	public RegistrationResource() {
		super(APIParameter.forStaticString("registration"));
	}

	@Override
	public APICallHandler accept(final SecurityContext securityContext, final APICall call) throws FrameworkException {
		return new RegistrationResourceHandler(securityContext, call.getURL());
	}

}
