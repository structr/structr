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
package org.structr.core.validator;

import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.LowercaseUniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.search.SearchUserCommand;


/**
 * Validator that checks the uniqueness after a given lowercase transformation
 * of the property value.
 *
 * @author Bastian Knerr
 */
public class LowercaseTypeUniquenessValidator implements PropertyValidator<String> {

	private final NodeIndex nodeIndex;
	private final Class type;

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
		if (result == null) {
			return true;
		}

		final String id = result.getUuid();
		errorBuffer.add(object.getType(), new LowercaseUniqueToken(id, key, value));

		return false;
	}


	private static AbstractNode lookup(final NodeIndex index, final PropertyKey key, final String value) {
		try {
			return
				(AbstractNode) Services.command(SecurityContext.getSuperUserInstance(), SearchUserCommand.class).execute(value, key, index);

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
