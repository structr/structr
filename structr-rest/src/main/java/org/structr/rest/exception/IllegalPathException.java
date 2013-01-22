/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.exception;

import javax.servlet.http.HttpServletResponse;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class IllegalPathException extends FrameworkException {

	public IllegalPathException() {
		super(HttpServletResponse.SC_BAD_REQUEST, new ErrorBuffer());
	}
}
