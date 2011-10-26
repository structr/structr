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
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class OnOperation implements Transformation {

	private List<AbstractNode> nodeList = new LinkedList<AbstractNode>();
	private SecurityContext securityContext = null;
	private AbstractNode currentNode = null;

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public void transform(Operation operation) throws InvalidTransformationException {

		if(operation instanceof NodeListOperation) {

			for(AbstractNode node : nodeList) {
				((NodeListOperation)operation).addNodeToList(node);
			}

		} else {

			throw new InvalidTransformationException("ON cannot be applied to " + operation.getKeyword());
		}
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
		this.currentNode = currentNode;
	}

	@Override
	public int getParameterCount() {

		return(1);
	}

	@Override
	public String getKeyword() {

		return("on");
	}

	@Override
	public boolean canExecute() {

		return(!nodeList.isEmpty());
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("ON does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			for(Object o : (Collection)parameter) {
				
				add(o);
			}


		} else {

			add(parameter);
		}
	}

	private void add(Object obj) {

		Object findNodeResult = Services.command(securityContext, FindNodeCommand.class).execute(currentNode, obj);

		if(findNodeResult != null) {

			if(findNodeResult instanceof AbstractNode) {

				nodeList.add((AbstractNode)findNodeResult);

			} else if(findNodeResult instanceof Collection) {

				for(Object o : (Collection)findNodeResult) {

					if(o instanceof AbstractNode) {

						nodeList.add((AbstractNode)o);
					}
				}
			}
		}
	}
}
