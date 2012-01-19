/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.core.node.operation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class UnsetOperation implements PrimaryOperation, NodeListOperation {
	
	private List<AbstractNode> nodeList = new LinkedList<AbstractNode>();
	private List<String> properties = new LinkedList<String>();
	private SecurityContext securityContext = null;

	// ----- PrimaryOperation -----
	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		try {
			for(AbstractNode node : nodeList) {

				for(String property : properties) {

					node.setProperty(property, null);
				}
			}

		} catch(FrameworkException fex) {
			throw new NodeCommandException(fex.getMessage());
		}

		return(true);
	}

	@Override
	public void addCallback(Callback callback) {
	}

	@Override
	public String help() {

		return("unset [property name(s)] on [node(s)] - unset properties ");
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
	}

	@Override
	public int getParameterCount() {

		return(1);
	}

	@Override
	public String getKeyword() {

		return("unset");
	}

	@Override
	public boolean canExecute() {

		return(!(nodeList.isEmpty() && properties.isEmpty()));
	}

	@Override
	public void addSwitch(String sw) throws InvalidSwitchException {

		throw new InvalidSwitchException("UNSET does not support switches");
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			properties.addAll((Collection)parameter);

		} else {

			properties.add(parameter.toString());
		}
	}

	// ----- NodeListOperation -----
	@Override
	public void addNodeToList(AbstractNode node) {

		nodeList.add(node);
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
