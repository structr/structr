/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.validator;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertyNotFoundToken;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

/**
 * A validator that normalizes the given value and ensures it is an
 * existing entity of given type.
 *
 * @author Christian Morgner
 */
public class TypeAndExactNameValidator extends PropertyValidator {

	@Override
	public boolean isValid(GraphObject object, String key, Object value, Value parameter, ErrorBuffer errorBuffer) {

		if(parameter == null) {
			throw new IllegalStateException("TypeAndExactNameValidator needs a type parameter.");
		}

		String type = (String)parameter.get();

		if(key == null) {
			return false;
		}

		if(!(value instanceof String)) {
			errorBuffer.add(object.getType(), new TypeToken(key, "string"));
			return false;
		}

		String stringValue = (String)value;
		if(StringUtils.isBlank(stringValue)) {
			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));
			return false;
		}

		// FIXME: search should be case-sensitive!

		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
		attrs.add(Search.andExactName(stringValue));
		attrs.add(Search.andType(type));

		// just check for existance
		try {
			List<AbstractNode> nodes = (List<AbstractNode>)Services.command(securityContext, SearchNodeCommand.class).execute(null, false, false, attrs);
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
