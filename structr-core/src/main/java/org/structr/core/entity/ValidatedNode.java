/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.core.entity;

import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.NullPropertyToken;
import org.structr.core.property.Property;

/**
 * A node with validation. This is a convenience class that avoids having to
 * implement the validation methods for all types separately.
 *
 *
 */
public abstract class ValidatedNode extends AbstractNode {

	protected boolean nonEmpty(Property<String> property, ErrorBuffer errorBuffer) {

		String value  = getProperty(property);
		boolean valid = true;

		if (value == null) {

			errorBuffer.add(new NullPropertyToken(getClass().getSimpleName(), property));
			valid = false;

		} else if (value.isEmpty()) {

			errorBuffer.add(new EmptyPropertyToken(getClass().getSimpleName(), property));
			valid = false;
		}

		return valid;
	}

	protected boolean nonNull(Property property, ErrorBuffer errorBuffer) {

		Object value  = getProperty(property);
		boolean valid = true;

		if (value == null) {

			errorBuffer.add(new EmptyPropertyToken(getClass().getSimpleName(), property));
			valid = false;
		}

		return valid;
	}
}
