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
public class WithOperation implements Transformation {

	private List<String> attributes = new LinkedList<String>();

	@Override
	public void transform(Operation operation) throws InvalidTransformationException {

		if(operation instanceof NodePropertyOperation) {

			for(String attribute : attributes) {

				// try to parse attributes and set..
				String[] parts = attribute.split("[=]+");
				if(parts.length == 2) {

					String key = parts[0].trim();
					String val = parts[1].trim();

					if("type".equals(key.toLowerCase()) || "name".equals(key.toLowerCase())) {

						throw new InvalidTransformationException("Name and type are restricted properties");
					}

					((NodePropertyOperation)operation).setProperty(key, val);

				} else {

					throw new InvalidTransformationException("Invalid property " + attribute);
				}
			}

		} else {

			throw new InvalidTransformationException("WITH cannot be applied to " + operation.getKeyword());
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

		return("with");
	}

	@Override
	public boolean canExecute() {

		return(true);
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("WITH does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			Collection<String> parameters = (Collection<String>)parameter;
			attributes.addAll(parameters);

		} else {

			attributes.add(parameter.toString());
		}
	}

}
