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
package org.structr.common.error;

/**
 * Specialized exception for errors in property input parsing.
 */
public class NumericalMethodInputParsingException extends FrameworkException {

	public NumericalMethodInputParsingException(final String longMethodName, final String methodName, final String parameterName, final Object value) {

		super(
			422,
			"Cannot parse input for ‛" + parameterName + "‛ in " + longMethodName,
			new SemanticErrorToken(null, null, "must_be_numerical").withValue(value).with("method", methodName).with("parameter", parameterName)
		);
	}
}
