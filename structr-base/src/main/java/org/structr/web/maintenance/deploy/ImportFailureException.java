/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.maintenance.deploy;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;

/**
 */
public class ImportFailureException extends RuntimeException {

	private ErrorBuffer errorBuffer = new ErrorBuffer();

	public ImportFailureException(final String message, final Throwable cause) {

		super(message, cause);

		if (cause instanceof FrameworkException fex) {

			// copy error buffer contents from wrapped exception
			this.errorBuffer = fex.getErrorBuffer();
		}
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

}
