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
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class CreateOperation implements PrimaryOperation, NodeTypeOperation, NodeParentOperation, NodePropertyOperation {

	private List<NodeAttribute> nodeAttributes = new LinkedList<NodeAttribute>();
	private List<String> nodeNames = new LinkedList<String>();
	private SecurityContext securityContext = null;
	private String nodeType = "Folder";
	private long nodeParent = 0;

	// ----- interface Operation -----
	@Override
	public int getParameterCount() {

		return(1);
	}

	@Override
	public String getKeyword() {

		return("mk");
	}

	@Override
	public boolean executeOperation(final StringBuilder stdOut) throws NodeCommandException {

		boolean ret = false;
		
		try {
			final AbstractNode parent = (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(nodeParent);

			if(parent != null) {

				Boolean retValue = (Boolean)Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Command createRelCommand = Services.command(securityContext, CreateRelationshipCommand.class);
						Command createNodeCommand = Services.command(securityContext, CreateNodeCommand.class);
						List<Long> newNodeIds = new LinkedList<Long>();
						int count = 0;

						for(String nodeName : nodeNames) {

							AbstractNode newNode = (AbstractNode)createNodeCommand.execute(
							    securityContext.getUser(),
							    nodeAttributes
							);

							newNode.setName(nodeName);
							newNode.setType(nodeType);

							createRelCommand.execute(parent, newNode, RelType.HAS_CHILD);
							newNodeIds.add(newNode.getId());

							count++;
						}

						if(newNodeIds.size() == 1) {

							stdOut.append("Node ").append(newNodeIds).append(" with type ").append(nodeType).append(" created");

						} else {

							stdOut.append("Nodes ").append(newNodeIds).append(" with type ").append(nodeType).append(" created");

						}

						return(true);
					}
				});

				if(retValue != null) {

					ret = retValue.booleanValue();
				}


			} else {

				throw new NodeCommandException("Creation parent node not found");
			}

		} catch(FrameworkException fex) {
			fex.printStackTrace();
		}

		return(ret);
	}

	@Override
	public String help() {

		StringBuilder ret = new StringBuilder(100);
		ret.append("usage: mk [name(s)] [as [type]] [in [parent]] [with [attribute(s)]] - make node(s)");

		return(ret.toString());
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {

		if(currentNode != null) {

			nodeParent = currentNode.getId();
		}
	}

	@Override
	public void addCallback(Callback callback) {

	}

	@Override
	public boolean canExecute() {

		return(!nodeNames.isEmpty());
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("MK does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			nodeNames.addAll((Collection)parameter);
			
		} else {
			
			nodeNames.add(parameter.toString());

		}
	}

	// ----- interface NodeTypeOperation -----
	@Override
	public void setNodeType(String type) {

		this.nodeType = type;
	}

	// ----- interface NodeParentOperation -----
	@Override
	public void setNodeParent(long id) {

		this.nodeParent = id;
	}

	// ----- interface NodePropertyOperation -----
	@Override
	public void setProperty(String key, Object value) {

		nodeAttributes.add(new NodeAttribute(key, value));
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
