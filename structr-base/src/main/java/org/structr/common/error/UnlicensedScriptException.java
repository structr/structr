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
package org.structr.common.error;

import org.slf4j.Logger;

/**
 * Exception to be thrown when an unlicensed scripting function is encountered.
 */
public class UnlicensedScriptException extends RuntimeException {

	public UnlicensedScriptException(final String name, final String module) {

		super(UnlicensedScriptException.buildLogMessage(name, module));
	}

	public void log(final Logger logger) {

		if (logger != null) {

			logger.error(getMessage());

		} else {

			System.out.println(getMessage());
		}
	}

	public static String buildLogMessage(final String name, final String module) {

		final StringBuilder buf = new StringBuilder();

		buf.append("Call to unlicensed StructrScript function ");
		buf.append(name);
		buf.append("(). This function requires the ");
		buf.append(module);
		buf.append(" module. Please contact licensing@structr.com with this error message for more information.");

		return buf.toString();
	}
}
