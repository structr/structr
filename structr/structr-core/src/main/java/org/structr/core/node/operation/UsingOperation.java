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
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class UsingOperation implements Transformation {

	private List<String> relationshipTypes = new LinkedList<String>();

	@Override
	public void transform(Operation operation) throws InvalidTransformationException {

		if(operation instanceof NodeRelationshipOperation) {

			for(String attribute : relationshipTypes) {

				((NodeRelationshipOperation)operation).setRelationshipType(attribute);
			}
			
		} else {

			throw new InvalidTransformationException("USING cannot be applied to " + operation.getKeyword());
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

		return("using");
	}

	@Override
	public boolean canExecute() {

		return(!relationshipTypes.isEmpty());
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("USING does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			Collection<String> parameters = (Collection<String>)parameter;
			relationshipTypes.addAll(parameters);

		} else {

			relationshipTypes.add(parameter.toString());
		}
	}

}
