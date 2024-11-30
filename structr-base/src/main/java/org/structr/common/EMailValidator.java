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
package org.structr.common;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.schema.Validator;

/**
 */
public class EMailValidator implements Validator<GraphObject> {

	@Override
	public boolean isValid(final GraphObject node, final ErrorBuffer errorBuffer) {

		final Class type                = node.getClass();
		final PropertyKey<String> eMail = node.key(type, "eMail");
		boolean valid                   = true;

		valid &= ValidationHelper.isValidUniqueProperty(node, eMail, errorBuffer);

		final String _eMail = (String)node.getProperty(eMail);
		if (_eMail != null) {

			// verify that the address contains at least the @ character,
			// which is a requirement for it to be distinguishable from
			// a user name, so email addresses can less easily interfere
			// with user names.
			if (!_eMail.contains("@")) {

				valid = false;

				errorBuffer.add(new SemanticErrorToken(type.getSimpleName(), "eMail", "must_contain_at_character").withDetail(_eMail));
			}
		}

		return valid;
	}
}
