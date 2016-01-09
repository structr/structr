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

import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.LowercaseUniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.search.SearchUserCommand;


/**
 * Validator that checks the uniqueness after a given lowercase transformation
 * of the property value.
 *
 *
 */
public class LowercaseTypeUniquenessValidator implements PropertyValidator<String> {

	private final NodeIndex nodeIndex;
	private final Class type;

	public LowercaseTypeUniquenessValidator(final Class type) {

		this.type = type;
		this.nodeIndex = NodeIndex.caseInsensitive;
	}

	public LowercaseTypeUniquenessValidator(final Class type, final NodeIndex indexKey) {

		nodeIndex = indexKey;
		this.type = type;
	}

	@Override
	public boolean isValid(SecurityContext securityContext, final GraphObject object, final PropertyKey<String> key, final String value, final ErrorBuffer errorBuffer) {

		if (!type.isAssignableFrom(object.getClass())) {

			// types are different
			return true;
		}

		final AbstractNode result = lookup(nodeIndex, key, value);
		if (result != null && result.getId() != object.getId()) {


			final String id = result.getUuid();
			errorBuffer.add(new LowercaseUniqueToken(object.getType(), key, id));

			return false;
		}

		return true;
	}


	private static AbstractNode lookup(final NodeIndex index, final PropertyKey key, final String value) {
		try {
			return (AbstractNode) StructrApp.getInstance().command(SearchUserCommand.class).execute(value, key, index);

		} catch (final FrameworkException fex) {
			fex.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean requiresSynchronization() {
		return true;
	}
}
