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
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class LinkOperation implements PrimaryOperation, NodeRelationshipOperation, NodePropertyOperation {

	private ParameterState parameterState = ParameterState.NoNodeSet;
	private List<NodeAttribute> attributes = new LinkedList<NodeAttribute>();
	private List<String> relationships = new LinkedList<String>();
	private List<Callback> callbacks = new LinkedList<Callback>();
	private SecurityContext securityContext = null;
	private AbstractNode currentNode = null;
	private AbstractNode startNode = null;
	private AbstractNode endNode = null;

	private enum ParameterState {

		NoNodeSet, StartNodeSet, EndNodeSet
	}

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		boolean ret = false;

		// set default if rels is empty
		if(relationships.isEmpty()) {
			relationships.add("LINK");
		}

		if(parameterState.equals(ParameterState.EndNodeSet)) {

			Command createRelationshipCommand = Services.command(securityContext, CreateRelationshipCommand.class);
			for(String rel : relationships) {

				try {

					final AbstractRelationship newRel = (AbstractRelationship)createRelationshipCommand.execute(startNode, endNode, rel);

					if(newRel != null) {

						Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

							@Override
							public Object execute() throws FrameworkException {

								for(NodeAttribute attribute : attributes) {
									newRel.setProperty(attribute.getKey(), attribute.getValue());
								}

								return (null);
							}
						});

						ret = true;

					} else {

						ret = false;
					}

				} catch(FrameworkException fex) {
					fex.printStackTrace();
				}
			}

		} else {

			throw new NodeCommandException("Invalid number of parameters");
		}

		return (ret);
	}

	@Override
	public String help() {

		StringBuilder ret = new StringBuilder(100);
		ret.append("usage: ln [startNode] [endNode] [using [relationship(s)]] [with [attribute(s)]] - link nodes");

		return (ret.toString());
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
		this.currentNode = currentNode;
	}

	@Override
	public void addCallback(Callback callback) {
		callbacks.add(callback);
	}

	@Override
	public int getParameterCount() {
		return (2);
	}

	@Override
	public String getKeyword() {
		return ("ln");
	}

	@Override
	public boolean canExecute() {

		return (parameterState.equals(ParameterState.EndNodeSet));
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("LN does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		switch(parameterState) {

			case NoNodeSet:
				startNode = getNode(parameter);
				parameterState = ParameterState.StartNodeSet;
				break;

			case StartNodeSet:
				endNode = getNode(parameter);
				parameterState = ParameterState.EndNodeSet;
				break;

			case EndNodeSet:
				throw new InvalidParameterException("LINK only takes two parameters");

		}
	}

	// ----- interface NodeRelationshipOperation -----
	@Override
	public void setRelationshipType(String relationshipType) {

		relationships.add(relationshipType);
	}

	// ----- interface NodePropertyOperation -----
	@Override
	public void setProperty(String key, Object value) {

		attributes.add(new NodeAttribute(key, value));
	}

	// ----- private methods -----
	private AbstractNode getNode(Object parameter) throws InvalidParameterException {

		Command findNodeCommand = Services.command(securityContext, FindNodeCommand.class);
		AbstractNode ret = null;

		if(parameter instanceof Collection) {

			throw new InvalidParameterException("LINK does not take multiple arguments");

		} else {

			try {
				Object findNodeReturnValue = findNodeCommand.execute(currentNode, parameter);
				if(findNodeReturnValue instanceof Collection) {

					throw new InvalidParameterException("LINK does not support wildcards");

				} else if(findNodeReturnValue instanceof AbstractNode) {

					ret = (AbstractNode)findNodeReturnValue;

				} else {

					throw new InvalidParameterException("Node " + parameter.toString() + " not found");
				}

			} catch(FrameworkException fex) {
				fex.printStackTrace();
			}

		}

		return (ret);
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
