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
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeAttribute;

/**
 *
 * @author Christian Morgner
 */
public class SetOperation implements PrimaryOperation, NodeListOperation {

	private List<NodeAttribute> attributes = new LinkedList<NodeAttribute>();
	private List<AbstractNode> nodeList = new LinkedList<AbstractNode>();
	private List<Callback> callbacks = new LinkedList<Callback>();
	private SecurityContext securityContext = null;

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		boolean ret = true;
		int count = 0;

		for(AbstractNode node : nodeList) {

			for(NodeAttribute attr : attributes) {

				node.setProperty(attr.getKey(), attr.getValue());
			}

			count++;
		}

		stdOut.append("set: ").append(count).append(" nodes modified");

		return(ret);
	}

	@Override
	public String help() {

		StringBuilder ret = new StringBuilder(100);
		ret.append("usage: set [attribute(s)] [on [node(s)]] - set properties (wildcards allowed)");

		return(ret.toString());
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
	}

	@Override
	public void addCallback(Callback callback) {

		callbacks.add(callback);
	}

	@Override
	public int getParameterCount() {

		return(1);
	}

	@Override
	public String getKeyword() {

		return("set");
	}

	@Override
	public boolean canExecute() {

		return(!attributes.isEmpty());
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("SET does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			for(Object o : (Collection)parameter) {

				addParameterInternal(o);
			}

		} else {

			addParameterInternal(parameter.toString());
		}
	}

	@Override
	public void addNodeToList(AbstractNode node) {

		nodeList.add(node);
	}

	private void addParameterInternal(Object source) throws InvalidParameterException {

		// try to parse attributes and set..
		String[] parts = source.toString().split("[=]+", 2);
		if(parts.length == 2) {

			String key = parts[0].trim();
			String val = parts[1].trim();

			attributes.add(new NodeAttribute(key, val));

		} else {

			throw new InvalidParameterException("Invalid parameter " + source.toString());
		}
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
