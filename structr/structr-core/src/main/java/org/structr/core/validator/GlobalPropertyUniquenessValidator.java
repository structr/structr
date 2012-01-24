/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.validator;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.dom4j.tree.AbstractNode;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;

/**
 *
 * @author chrisi
 */
public class GlobalPropertyUniquenessValidator extends PropertyValidator<String> {

	private static final Logger logger = Logger.getLogger(GlobalPropertyUniquenessValidator.class.getName());

	@Override
	public boolean isValid(GraphObject object, String key, String value, ErrorBuffer errorBuffer) {

		if(value == null || (value != null && value.toString().length() == 0)) {
			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));
			return false;
		}

		if(key != null && value != null) {

			// String type = EntityContext.GLOBAL_UNIQUENESS;
			AbstractNode topNode = null;
			Boolean includeDeleted = false;
			Boolean publicOnly = false;
			boolean nodeExists = false;

			List<SearchAttribute> attributes = new LinkedList<SearchAttribute>();
			attributes.add(new TextualSearchAttribute(key, value, SearchOperator.AND));

			try {
				List<AbstractNode> resultList = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, attributes);
				nodeExists = !resultList.isEmpty();

			} catch(FrameworkException fex ) {
				// handle error
			}
			
			if(nodeExists) {

				errorBuffer.add(object.getType(), new UniqueToken(key, value));
				return false;

			} else {

				return true;
			}


		}

		return false;
	}
}
