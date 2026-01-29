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
package org.structr.rest.exception;


import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;

import java.util.Set;


/**
 *
 */
public class IllegalMethodException extends FrameworkException {

	public IllegalMethodException(final String message, final Set<String> allowedMethods) {

		super(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);

		// 405 is required to return allowed methods
		headers().put("Allow", StringUtils.join(allowedMethods, ","));
	}
}
