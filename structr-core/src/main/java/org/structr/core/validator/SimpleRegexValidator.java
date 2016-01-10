/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.validator;

import java.util.regex.Pattern;
import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.MatchToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 * A simple regular expression validator.
 *
 *
 */
public class SimpleRegexValidator implements PropertyValidator<String> {

	private Pattern pattern = null;

	public SimpleRegexValidator(String expression) {
		this.pattern = Pattern.compile(expression);
	}

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<String> key, String value, ErrorBuffer errorBuffer) {

		if (value != null && pattern.matcher(value).matches()) {
			return true;
		}

		errorBuffer.add(new MatchToken(object.getType(), key, this.pattern.pattern()));
		return false;
	}

	@Override
	public boolean requiresSynchronization() {
		return false;
	}
}
