/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertyNotFoundToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;

/**
 * A validator that normalizes the given value and ensures it is an
 * existing entity of given type.
 *
 * @author Christian Morgner
 */
public class TypeAndExactNameValidator extends PropertyValidator<String> {

	private String type = null;

	public TypeAndExactNameValidator(String type) {
		this.type = type;
	}

	@Override
	public boolean isValid(GraphObject object, PropertyKey<String> key, String value, ErrorBuffer errorBuffer) {

		if(key == null) {
			return false;
		}

		if(StringUtils.isBlank(value)) {
			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));
			return false;
		}

		// FIXME: search should be case-sensitive!

		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
		attrs.add(Search.andExactName(value));
		attrs.add(Search.andType(type));

		// just check for existance
		try {
			Result nodes = Services.command(securityContext, SearchNodeCommand.class).execute(attrs);
			if(nodes != null && !nodes.isEmpty()) {

				return true;

			} else {

				errorBuffer.add(object.getType(), new PropertyNotFoundToken(key, value));
				return false;
			}

		} catch(FrameworkException fex ) {
			// handle error
		}

		return false;
	}

}
