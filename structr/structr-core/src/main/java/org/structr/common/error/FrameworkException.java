/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.common.error;

import javax.servlet.http.HttpServletResponse;

/**
 * The base class for all structr exceptions. This class contains
 * a buffer that supports context-dependent, nested error
 * messages.
 *
 * @author Christian Morgner
 */
public class FrameworkException extends Exception {

	private int status = HttpServletResponse.SC_OK;
	private ErrorBuffer errorBuffer = null;

	public FrameworkException(String type, ErrorToken errorToken) {
		this.errorBuffer = new ErrorBuffer();
		this.errorBuffer.add(type, errorToken);
		this.status = errorToken.getStatus();
	}

	public FrameworkException(int status, ErrorBuffer errorBuffer) {
		this.errorBuffer = errorBuffer;
		this.status = status;
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public int getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return errorBuffer.toString();
	}
}
