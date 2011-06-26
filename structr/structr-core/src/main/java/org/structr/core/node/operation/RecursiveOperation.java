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

import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class RecursiveOperation implements Transformation {

	@Override
	public void transform(Operation operation) throws InvalidTransformationException {

		if(operation instanceof BooleanParameterOperation) {

			((BooleanParameterOperation)operation).setBooleanParameter("recursive", true);

		} else {

			throw new InvalidTransformationException("REC cannot be applied to " + operation.getKeyword());
		}
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
	}

	@Override
	public int getParameterCount() {

		return(0);
	}

	@Override
	public String getKeyword() {

		return("rec");
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		// should not happen
		throw new IllegalStateException("REC does not take parameters");
	}

}
