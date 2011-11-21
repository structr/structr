/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.validator;

import java.util.LinkedList;
import java.util.List;
import org.dom4j.tree.AbstractNode;
import org.structr.common.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;

/**
 *
 * @author chrisi
 */
public class GlobalPropertyUniquenessValidator extends PropertyValidator<String> {

	@Override
	public boolean isValid(String key, Object value, Value<String> parameter, ErrorBuffer errorBuffer) {

		if(value == null || (value != null && value.toString().length() == 0)) {
			errorBuffer.add("Property '", key, "' must not be empty.");
			return false;
		}

		if(key != null && value != null) {

			if(!(value instanceof String)) {
				return false;
			}

			String stringValue = (String)value;
			AbstractNode topNode = null;
			Boolean includeDeleted = false;
			Boolean publicOnly = false;
			boolean nodeExists = false;

			List<SearchAttribute> attributes = new LinkedList<SearchAttribute>();
			attributes.add(new TextualSearchAttribute(key, stringValue, SearchOperator.AND));

			List<AbstractNode> resultList = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, attributes);
			nodeExists = !resultList.isEmpty();

			if(nodeExists) {

				errorBuffer.add("A node with value '", value, "' for property '", key, "' already exists.");
				return false;

			} else {

				return true;
			}


		}

		return false;
	}
}
