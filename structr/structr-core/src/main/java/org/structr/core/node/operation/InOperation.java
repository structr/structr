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
import org.structr.common.CurrentSession;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class InOperation implements Transformation {

	private long parentNodeId = 0L;

	@Override
	public void transform(Operation operation) throws InvalidTransformationException {

		if(operation instanceof NodeParentOperation) {

			((NodeParentOperation)operation).setNodeParent(parentNodeId);

		} else {

			throw new InvalidTransformationException("IN cannot be applied to " + operation.getKeyword());
		}
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

		return("in");
	}

	@Override
	public boolean canExecute() {

		return(parentNodeId != -1);
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("IN does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			throw new InvalidParameterException("IN cannot handle multiple parmeters");

		} else {

			try {

				parentNodeId = Long.parseLong(parameter.toString());

			} catch(Throwable t) {

				AbstractNode findNode = (AbstractNode)Services.command(FindNodeCommand.class).execute(CurrentSession.getUser(), parameter);
				if(findNode != null) {

					parentNodeId = findNode.getId();

				} else {

					parentNodeId = -1;
				}
			}

			if(parentNodeId == -1) {

				throw new InvalidParameterException("Invalid parent node ID");
			}
		}
	}
}
