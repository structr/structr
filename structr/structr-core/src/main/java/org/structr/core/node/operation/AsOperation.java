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
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class AsOperation implements Transformation {

	private String nodeType = "Folder";

	@Override
	public void transform(Operation operation) throws InvalidTransformationException {

		if(operation instanceof NodeTypeOperation) {

			((NodeTypeOperation)operation).setNodeType(nodeType);

		} else {

			throw new InvalidTransformationException("AS cannot be applied to " + operation.getKeyword());
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

		return("as");
	}

	@Override
	public boolean canExecute() {

		return(true);
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("AS does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			throw new InvalidParameterException("AS cannot handle multiple parmeters");

		} else {

			this.nodeType = parameter.toString();
		}
	}

}
