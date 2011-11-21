/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.validator;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.tree.AbstractNode;
import org.structr.common.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
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

	private static final Logger logger = Logger.getLogger(GlobalPropertyUniquenessValidator.class.getName());

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

			String type = EntityContext.GLOBAL_UNIQUENESS;
			String stringValue = (String)value;
			AbstractNode topNode = null;
			Boolean includeDeleted = false;
			Boolean publicOnly = false;
			boolean nodeExists = false;

			List<SearchAttribute> attributes = new LinkedList<SearchAttribute>();
			attributes.add(new TextualSearchAttribute(key, stringValue, SearchOperator.AND));

			Semaphore semaphore = null;

			// obtain semaphores and acquire locks
			if(type != null && key != null) {
				semaphore = EntityContext.getSemaphoreForTypeAndProperty(type, key);
				if(semaphore != null) {
					try {	semaphore.acquire(); } catch(InterruptedException iex) { iex.printStackTrace(); }
					logger.log(Level.INFO, "Entering critical section for type {0} key {1} from thread {2}",
					    new Object[] { type, key, Thread.currentThread() } );
				}
			}

			List<AbstractNode> resultList = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, attributes);
			nodeExists = !resultList.isEmpty();

			if(semaphore != null) {
				semaphore.release();
				logger.log(Level.INFO, "Exiting critical section for type {0} key {1} from thread {2}",
				    new Object[] { type, key, Thread.currentThread() } );
			}

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
