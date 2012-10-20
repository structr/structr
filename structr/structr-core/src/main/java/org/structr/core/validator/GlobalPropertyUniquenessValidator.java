/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.structr.common.PropertyKey;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author chrisi
 */
public class GlobalPropertyUniquenessValidator extends PropertyValidator<String> {

	private static final Logger logger = Logger.getLogger(GlobalPropertyUniquenessValidator.class.getName());

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isValid(GraphObject object, PropertyKey key, String value, ErrorBuffer errorBuffer) {

		if ((value == null) || ((value != null) && (value.toString().length() == 0))) {

			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));

			return false;

		}

		if ((key != null) && (value != null)) {

			// String type = EntityContext.GLOBAL_UNIQUENESS;
			AbstractNode topNode             = null;
			Boolean includeDeletedAndHidden  = false;
			Boolean publicOnly               = false;
			boolean nodeExists               = false;
			List<SearchAttribute> attributes = new LinkedList<SearchAttribute>();
			String id                        = null;

			attributes.add(new TextualSearchAttribute(key, value, SearchOperator.AND));

			Result resultList = null;

			try {

				resultList = (Result) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(topNode, includeDeletedAndHidden,
					publicOnly, attributes);
				nodeExists = !resultList.isEmpty();

			} catch (FrameworkException fex) {

				// handle error
			}

			if (nodeExists) {

				id = ((AbstractNode) resultList.get(0)).getUuid();

				errorBuffer.add(object.getType(), new UniqueToken(id, key, value));

				return false;

			} else {

				return true;
			}
		}

		return false;

	}

}
