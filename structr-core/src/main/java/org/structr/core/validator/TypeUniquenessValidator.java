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

import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.structr.core.property.PropertyKey;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 * A validator that ensures global uniqueness for given entity type.
 *
 * @author Christian Morgner
 */
public class TypeUniquenessValidator<T> implements PropertyValidator<T> {

	private static final Logger logger = Logger.getLogger(TypeUniquenessValidator.class.getName());

	//~--- fields ---------------------------------------------------------

	private Class type = null;

	//~--- constructors ---------------------------------------------------

	public TypeUniquenessValidator(Class type) {

		this.type = type;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<T> key, T value, ErrorBuffer errorBuffer) {

		if (value == null) {

			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));

			return false;

		}

		if (!type.isAssignableFrom(object.getClass())) {
			
			// types are different
			return true;
		}
		
		if (key != null) {

			SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
			List<SearchAttribute> attributes = new LinkedList<SearchAttribute>();
			boolean nodeExists               = false;
			String id                        = null;

			attributes.add(Search.andExactType(type));
			attributes.add(Search.andExactProperty(superUserContext, key, value));

			Result<AbstractNode> resultList = null;

			try {

				resultList = Services.command(superUserContext, SearchNodeCommand.class).execute(attributes);
				nodeExists = !resultList.isEmpty();

			} catch (FrameworkException fex) {

				fex.printStackTrace();

			}

			if (nodeExists) {

				AbstractNode foundNode = resultList.get(0);
				if (foundNode.getId() != object.getId()) {

					id = ((AbstractNode) resultList.get(0)).getUuid();

					errorBuffer.add(object.getType(), new UniqueToken(id, key, value));

					return false;
				}
			}
		}

		return true;
	}
	
	@Override
	public boolean requiresSynchronization() {
		return true;
	}
}
