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


package org.structr.common.error;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

/**
 * The base class for all structr exceptions. This class contains
 * a buffer that supports context-dependent, nested error
 * messages.
 *
 * @author Christian Morgner
 */
public class FrameworkException extends Exception {

	private ErrorBuffer errorBuffer = null;
	private String message          = null;
	private int status              = HttpServletResponse.SC_OK;

	//~--- constructors ---------------------------------------------------

	public FrameworkException(int status, ErrorBuffer errorBuffer) {

		this.errorBuffer = errorBuffer;
		this.status      = status;

	}

	public FrameworkException(int status, String message) {

		this.status  = status;
		this.message = message;

	}

	public FrameworkException(String type, ErrorToken errorToken) {

		this.errorBuffer = new ErrorBuffer();

		this.errorBuffer.add(type, errorToken);

		this.status = errorToken.getStatus();

	}

	public FrameworkException(int status, Throwable cause) {

		super(cause);

		this.status = status;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public String toString() {

		StringBuilder out                                = new StringBuilder();
		ErrorBuffer buf                                  = getErrorBuffer();
		
		if (buf != null) {
			
			Map<String, Map<String, Set<ErrorToken>>> tokens = buf.getErrorTokens();

			for (Map.Entry<String, Map<String, Set<ErrorToken>>> token : tokens.entrySet()) {

				String tokenKey = token.getKey();

				out.append(tokenKey);

				Map<String, Set<ErrorToken>> errors = token.getValue();

				for (Map.Entry<String, Set<ErrorToken>> error : errors.entrySet()) {

					String errorKey = error.getKey();

					out.append("\n").append(errorKey).append(" => ");

					Set<ErrorToken> singleErrors = error.getValue();

					for (ErrorToken et : singleErrors) {

						out.append(et.getStatus()).append(": ").append(et.getKey()).append(" ").append(et.getErrorToken()).append(", ");
					}

					out.delete(out.length()-2, out.length());

				}

			}
			
		} else {
			
			out.append("FrameworkException(").append(status).append("): ").append(message);
		}

		return out.toString();

	}

	//~--- get methods ----------------------------------------------------

	public ErrorBuffer getErrorBuffer() {

		return errorBuffer;

	}

	public int getStatus() {

		return status;

	}

	@Override
	public String getMessage() {

		return message;

	}

}
